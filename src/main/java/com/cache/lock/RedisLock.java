package com.cache.lock;

import com.cache.RedisCache;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributed lock implementation for {@link RedisCache}.
 *
 * <p>Key features:
 * <ul>
 *   <li><b>Owner Token / Fencing:</b> Each lock acquisition is tagged with a unique client ID and thread ID,
 *       ensuring only the holding thread can release or renew the lock via atomic CAS.</li>
 *   <li><b>Re-entrancy:</b> Tracks re-entrant hold counts per thread.</li>
 *   <li><b>Watchdog Heartbeat:</b> When acquired without an explicit lease time, a background daemon task
 *       periodically renews the lease every {@code watchdogTimeout / 3} until {@link #unlock()} is invoked.</li>
 *   <li><b>Safe Release:</b> Uses {@code compareAndDelete} so an expired lock acquired by another client
 *       is never inadvertently deleted.</li>
 *   <li><b>Adaptive Backoff:</b> Retries with backoff during lock contention to reduce busy-waiting overhead.</li>
 * </ul>
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public class RedisLock implements DistributedLock {

    private static final ScheduledExecutorService WATCHDOG_EXECUTOR =
            Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()), r -> {
                Thread t = new Thread(r, "redis-lock-watchdog");
                t.setDaemon(true);
                return t;
            });

    public static final long DEFAULT_WATCHDOG_TIMEOUT_MS = 30000; // 30 seconds

    private final RedisCache cache;
    private final String lockName;
    private final String lockKey;
    private final String clientId;
    private final long watchdogTimeoutMillis;

    private final ConcurrentHashMap<Long, LockHoldState> threadStates = new ConcurrentHashMap<>();

    private static class LockHoldState {
        final String ownerToken;
        final AtomicInteger holdCount = new AtomicInteger(0);
        volatile ScheduledFuture<?> watchdogFuture;

        LockHoldState(String ownerToken) {
            this.ownerToken = ownerToken;
        }
    }

    /**
     * Creates a new distributed lock with the default 30-second watchdog timeout.
     *
     * @param cache RedisCache instance
     * @param lockName name of the lock
     */
    public RedisLock(RedisCache cache, String lockName) {
        this(cache, lockName, DEFAULT_WATCHDOG_TIMEOUT_MS);
    }

    /**
     * Creates a new distributed lock with a custom watchdog timeout.
     *
     * @param cache RedisCache instance
     * @param lockName name of the lock
     * @param watchdogTimeoutMillis watchdog timeout in milliseconds
     */
    public RedisLock(RedisCache cache, String lockName, long watchdogTimeoutMillis) {
        if (cache == null) {
            throw new IllegalArgumentException("cache cannot be null");
        }
        if (lockName == null || lockName.trim().isEmpty()) {
            throw new IllegalArgumentException("lockName cannot be null or empty");
        }
        this.cache = cache;
        this.lockName = lockName;
        this.lockKey = "lock:" + lockName;
        this.clientId = UUID.randomUUID().toString();
        this.watchdogTimeoutMillis = watchdogTimeoutMillis > 0 ? watchdogTimeoutMillis : DEFAULT_WATCHDOG_TIMEOUT_MS;
    }

    private String buildOwnerToken(long threadId) {
        return clientId + ":" + threadId;
    }

    @Override
    public void lock() {
        try {
            tryLock(Long.MAX_VALUE / 2, -1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock: " + lockName, e);
        }
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        try {
            tryLock(Long.MAX_VALUE / 2, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock: " + lockName, e);
        }
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(0, -1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        return tryLock(waitTime, -1, unit);
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        long threadId = Thread.currentThread().getId();
        String ownerToken = buildOwnerToken(threadId);

        // 1. Re-entrancy check
        if (isHeldByCurrentThread()) {
            LockHoldState state = threadStates.get(threadId);
            if (state != null) {
                state.holdCount.incrementAndGet();
                return true;
            }
        }

        long waitTimeMillis = unit != null && waitTime > 0 ? unit.toMillis(waitTime) : 0;
        long deadline = waitTimeMillis > 0 ? System.currentTimeMillis() + waitTimeMillis : System.currentTimeMillis();
        boolean enableWatchdog = (leaseTime <= 0);
        long effectiveLeaseMillis = enableWatchdog ? watchdogTimeoutMillis : unit.toMillis(leaseTime);

        // 2. Initial acquisition attempt
        if (cache.setnx(lockKey, ownerToken, effectiveLeaseMillis)) {
            onLockAcquired(threadId, ownerToken, enableWatchdog);
            return true;
        }

        if (waitTimeMillis <= 0) {
            return false;
        }

        // 3. Contention retry loop with backoff
        long backoff = 10; // Start at 10ms
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }

            long sleepTime = Math.min(Math.min(backoff, remaining), 100);
            Thread.sleep(sleepTime);
            backoff = Math.min(backoff * 2, 100);

            if (cache.setnx(lockKey, ownerToken, effectiveLeaseMillis)) {
                onLockAcquired(threadId, ownerToken, enableWatchdog);
                return true;
            }
        }

        return false;
    }

    private void onLockAcquired(long threadId, String ownerToken, boolean enableWatchdog) {
        LockHoldState state = threadStates.computeIfAbsent(threadId, k -> new LockHoldState(ownerToken));
        state.holdCount.set(1);

        if (enableWatchdog) {
            startWatchdog(threadId, state);
        }
    }

    private void startWatchdog(long threadId, LockHoldState state) {
        long interval = Math.max(10, watchdogTimeoutMillis / 3);
        state.watchdogFuture = WATCHDOG_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                if (state.holdCount.get() <= 0) {
                    return;
                }
                boolean renewed = cache.compareAndExpire(lockKey, state.ownerToken, watchdogTimeoutMillis);
                if (!renewed) {
                    // Lock was lost or expired
                    stopWatchdog(state);
                }
            } catch (Exception ignored) {
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void stopWatchdog(LockHoldState state) {
        ScheduledFuture<?> future = state.watchdogFuture;
        if (future != null) {
            future.cancel(true);
            state.watchdogFuture = null;
        }
    }

    @Override
    public void unlock() {
        long threadId = Thread.currentThread().getId();
        LockHoldState state = threadStates.get(threadId);

        if (state == null || state.holdCount.get() <= 0 || !isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException("Thread " + threadId + " does not hold lock: " + lockName);
        }

        int remainingHolds = state.holdCount.decrementAndGet();
        if (remainingHolds == 0) {
            try {
                stopWatchdog(state);
                cache.compareAndDelete(lockKey, state.ownerToken);
            } finally {
                threadStates.remove(threadId);
            }
        }
    }

    @Override
    public boolean isLocked() {
        return cache.exists(lockKey);
    }

    @Override
    public boolean isHeldByCurrentThread() {
        long threadId = Thread.currentThread().getId();
        LockHoldState state = threadStates.get(threadId);
        if (state == null || state.holdCount.get() <= 0) {
            return false;
        }

        Object currentVal = cache.get(lockKey);
        if (Objects.equals(currentVal, state.ownerToken)) {
            return true;
        }

        // Lock expired or taken by another owner in the cache
        stopWatchdog(state);
        threadStates.remove(threadId);
        return false;
    }

    @Override
    public int getHoldCount() {
        if (!isHeldByCurrentThread()) {
            return 0;
        }
        LockHoldState state = threadStates.get(Thread.currentThread().getId());
        return state != null ? state.holdCount.get() : 0;
    }

    @Override
    public String getLockName() {
        return lockName;
    }

    /**
     * Gets the full cache key used for this lock.
     *
     * @return key string in RedisCache
     */
    public String getLockKey() {
        return lockKey;
    }

    /**
     * Returns the watchdog timeout in milliseconds.
     *
     * @return watchdog timeout
     */
    public long getWatchdogTimeoutMillis() {
        return watchdogTimeoutMillis;
    }
}
