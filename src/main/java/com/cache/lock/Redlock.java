package com.cache.lock;

import com.cache.RedisCache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-instance distributed lock implementation based on the Redlock algorithm.
 *
 * <p>Provides fault-tolerant locking across {@code N} independent {@link RedisCache} instances.
 * A lock is considered successfully acquired if and only if:
 * <ol>
 *   <li>The client acquires the lock on at least a quorum of instances ({@code N / 2 + 1}).</li>
 *   <li>The total time spent acquiring does not exceed the total lock validity time.</li>
 * </ol>
 * If acquisition fails, all acquired instances are immediately rolled back (unlocked).
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public class Redlock implements DistributedLock {

    private static final ScheduledExecutorService WATCHDOG_EXECUTOR =
            Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()), r -> {
                Thread t = new Thread(r, "redlock-watchdog");
                t.setDaemon(true);
                return t;
            });

    public static final long DEFAULT_WATCHDOG_TIMEOUT_MS = 30000; // 30 seconds
    private static final double CLOCK_DRIFT_FACTOR = 0.01; // 1% clock drift

    private final List<RedisCache> caches;
    private final String lockName;
    private final String lockKey;
    private final String clientId;
    private final int quorum;
    private final long watchdogTimeoutMillis;

    private final ConcurrentHashMap<Long, RedlockHoldState> threadStates = new ConcurrentHashMap<>();

    private static class RedlockHoldState {
        final String ownerToken;
        final AtomicInteger holdCount = new AtomicInteger(0);
        volatile ScheduledFuture<?> watchdogFuture;

        RedlockHoldState(String ownerToken) {
            this.ownerToken = ownerToken;
        }
    }

    /**
     * Creates a Redlock instance across the given caches.
     *
     * @param lockName name of the lock
     * @param caches array of RedisCache instances (must contain at least 1)
     */
    public Redlock(String lockName, RedisCache... caches) {
        this(lockName, Arrays.asList(caches), DEFAULT_WATCHDOG_TIMEOUT_MS);
    }

    /**
     * Creates a Redlock instance across the given list of caches.
     *
     * @param lockName name of the lock
     * @param caches list of RedisCache instances
     */
    public Redlock(String lockName, List<RedisCache> caches) {
        this(lockName, caches, DEFAULT_WATCHDOG_TIMEOUT_MS);
    }

    /**
     * Creates a Redlock instance with custom watchdog timeout.
     *
     * @param lockName name of the lock
     * @param caches list of RedisCache instances
     * @param watchdogTimeoutMillis watchdog timeout in milliseconds
     */
    public Redlock(String lockName, List<RedisCache> caches, long watchdogTimeoutMillis) {
        if (lockName == null || lockName.trim().isEmpty()) {
            throw new IllegalArgumentException("lockName cannot be null or empty");
        }
        if (caches == null || caches.isEmpty()) {
            throw new IllegalArgumentException("caches cannot be null or empty");
        }
        this.lockName = lockName;
        this.lockKey = "redlock:" + lockName;
        this.caches = Collections.unmodifiableList(new ArrayList<>(caches));
        this.quorum = (this.caches.size() / 2) + 1;
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
            throw new RuntimeException("Interrupted while acquiring Redlock: " + lockName, e);
        }
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        try {
            tryLock(Long.MAX_VALUE / 2, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring Redlock: " + lockName, e);
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
            RedlockHoldState state = threadStates.get(threadId);
            if (state != null) {
                state.holdCount.incrementAndGet();
                return true;
            }
        }

        long waitTimeMillis = unit != null && waitTime > 0 ? unit.toMillis(waitTime) : 0;
        long deadline = waitTimeMillis > 0 ? System.currentTimeMillis() + waitTimeMillis : System.currentTimeMillis();
        boolean enableWatchdog = (leaseTime <= 0);
        long effectiveLeaseMillis = enableWatchdog ? watchdogTimeoutMillis : unit.toMillis(leaseTime);

        // 2. Initial acquisition attempt across instances
        if (acquireQuorum(ownerToken, effectiveLeaseMillis)) {
            onLockAcquired(threadId, ownerToken, enableWatchdog);
            return true;
        }

        if (waitTimeMillis <= 0) {
            return false;
        }

        // 3. Retry loop with randomized backoff
        long backoff = 10;
        Random random = new Random();
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }

            long sleepTime = Math.min(Math.min(backoff + random.nextInt(15), remaining), 100);
            Thread.sleep(sleepTime);
            backoff = Math.min(backoff * 2, 100);

            if (acquireQuorum(ownerToken, effectiveLeaseMillis)) {
                onLockAcquired(threadId, ownerToken, enableWatchdog);
                return true;
            }
        }

        return false;
    }

    private boolean acquireQuorum(String ownerToken, long leaseMillis) {
        long startTime = System.currentTimeMillis();
        int acquiredCount = 0;

        for (RedisCache cache : caches) {
            try {
                if (cache.setnx(lockKey, ownerToken, leaseMillis)) {
                    acquiredCount++;
                }
            } catch (Exception ignored) {
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        long drift = (long) (leaseMillis * CLOCK_DRIFT_FACTOR) + 2;
        long validityTime = leaseMillis - elapsedTime - drift;

        if (acquiredCount >= quorum && validityTime > 0) {
            return true;
        }

        // Rollback: unlock all instances to ensure clean state
        unlockAllInstances(ownerToken);
        return false;
    }

    private void onLockAcquired(long threadId, String ownerToken, boolean enableWatchdog) {
        RedlockHoldState state = threadStates.computeIfAbsent(threadId, k -> new RedlockHoldState(ownerToken));
        state.holdCount.set(1);

        if (enableWatchdog) {
            startWatchdog(threadId, state);
        }
    }

    private void startWatchdog(long threadId, RedlockHoldState state) {
        long interval = Math.max(10, watchdogTimeoutMillis / 3);
        state.watchdogFuture = WATCHDOG_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                if (state.holdCount.get() <= 0) {
                    return;
                }
                int renewed = 0;
                for (RedisCache cache : caches) {
                    try {
                        if (cache.compareAndExpire(lockKey, state.ownerToken, watchdogTimeoutMillis)) {
                            renewed++;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (renewed < quorum) {
                    // Lost quorum renewal
                    stopWatchdog(state);
                }
            } catch (Exception ignored) {
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void stopWatchdog(RedlockHoldState state) {
        ScheduledFuture<?> future = state.watchdogFuture;
        if (future != null) {
            future.cancel(true);
            state.watchdogFuture = null;
        }
    }

    private void unlockAllInstances(String ownerToken) {
        for (RedisCache cache : caches) {
            try {
                cache.compareAndDelete(lockKey, ownerToken);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void unlock() {
        long threadId = Thread.currentThread().getId();
        RedlockHoldState state = threadStates.get(threadId);

        if (state == null || state.holdCount.get() <= 0 || !isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException("Thread " + threadId + " does not hold Redlock: " + lockName);
        }

        int remainingHolds = state.holdCount.decrementAndGet();
        if (remainingHolds == 0) {
            try {
                stopWatchdog(state);
                unlockAllInstances(state.ownerToken);
            } finally {
                threadStates.remove(threadId);
            }
        }
    }

    @Override
    public boolean isLocked() {
        int lockedNodes = 0;
        for (RedisCache cache : caches) {
            try {
                if (cache.exists(lockKey)) {
                    lockedNodes++;
                }
            } catch (Exception ignored) {
            }
        }
        return lockedNodes >= quorum;
    }

    @Override
    public boolean isHeldByCurrentThread() {
        long threadId = Thread.currentThread().getId();
        RedlockHoldState state = threadStates.get(threadId);
        if (state == null || state.holdCount.get() <= 0) {
            return false;
        }

        int nodesWithToken = 0;
        for (RedisCache cache : caches) {
            try {
                Object val = cache.get(lockKey);
                if (Objects.equals(val, state.ownerToken)) {
                    nodesWithToken++;
                }
            } catch (Exception ignored) {
            }
        }

        if (nodesWithToken >= quorum) {
            return true;
        }

        // Quorum lost
        stopWatchdog(state);
        threadStates.remove(threadId);
        return false;
    }

    @Override
    public int getHoldCount() {
        if (!isHeldByCurrentThread()) {
            return 0;
        }
        RedlockHoldState state = threadStates.get(Thread.currentThread().getId());
        return state != null ? state.holdCount.get() : 0;
    }

    @Override
    public String getLockName() {
        return lockName;
    }

    public String getLockKey() {
        return lockKey;
    }

    public int getQuorum() {
        return quorum;
    }

    public int getInstanceCount() {
        return caches.size();
    }
}
