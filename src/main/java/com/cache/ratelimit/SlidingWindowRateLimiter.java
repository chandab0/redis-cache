package com.cache.ratelimit;

import com.cache.RedisCache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Sliding window log rate limiter using Redis Sorted Sets.
 *
 * <p>Unlike fixed-window limiters, the sliding window log eliminates boundary-burst anomalies
 * by recording timestamp logs of each permit in a sorted set and pruning logs older than
 * {@code now - windowSize}.
 *
 * <p>All operations are executed atomically on {@link RedisCache} using {@code executeAtomic}
 * to prevent race conditions during high concurrent traffic.
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final RedisCache cache;
    private final String name;
    private final String key;
    private final long maxPermits;
    private final long windowDurationMs;

    /**
     * Creates a new sliding window rate limiter.
     *
     * @param cache RedisCache instance
     * @param name unique rate limiter name
     * @param maxPermits maximum permits allowed within the window
     * @param windowDuration length of the sliding window
     * @param windowUnit time unit of the window duration
     */
    public SlidingWindowRateLimiter(RedisCache cache, String name, long maxPermits, long windowDuration, TimeUnit windowUnit) {
        if (cache == null) {
            throw new IllegalArgumentException("cache cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (maxPermits <= 0) {
            throw new IllegalArgumentException("maxPermits must be > 0");
        }
        if (windowDuration <= 0 || windowUnit == null) {
            throw new IllegalArgumentException("windowDuration and windowUnit must be valid");
        }

        this.cache = cache;
        this.name = name;
        this.key = "ratelimit:sliding:" + name;
        this.maxPermits = maxPermits;
        this.windowDurationMs = windowUnit.toMillis(windowDuration);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(long permits) {
        try {
            return tryAcquire(permits, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    @Override
    public boolean tryAcquire(long permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be > 0");
        }
        if (permits > maxPermits) {
            return false;
        }

        long timeoutMs = unit != null && timeout > 0 ? unit.toMillis(timeout) : 0;
        long deadline = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : System.currentTimeMillis();

        while (true) {
            long waitTimeMs = cache.executeAtomic(key, c -> {
                long now = System.currentTimeMillis();
                long windowStart = now - windowDurationMs;

                // 1. Remove expired timestamps outside the current sliding window
                c.zremrangebyscore(key, 0, windowStart);

                // 2. Count current elements in window
                long currentCount = c.zcard(key);
                if (currentCount + permits <= maxPermits) {
                    // 3. Add timestamp entries
                    for (int i = 0; i < permits; i++) {
                        c.zadd(key, (double) now, now + ":" + UUID.randomUUID().toString());
                    }
                    c.pexpire(key, windowDurationMs);
                    return 0L; // Acquired
                }

                // 4. Calculate time to wait until oldest item rolls out
                List<Map.Entry<String, Double>> oldest = c.zrangeWithScores(key, 0, 0);
                if (!oldest.isEmpty()) {
                    double oldestScore = oldest.get(0).getValue();
                    long expiresAt = (long) oldestScore + windowDurationMs;
                    long delay = expiresAt - now;
                    return Math.max(1L, delay);
                }

                return 10L;
            });

            if (waitTimeMs == 0) {
                return true;
            }

            if (timeoutMs <= 0) {
                return false;
            }

            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }

            long sleepTime = Math.min(waitTimeMs, remaining);
            Thread.sleep(sleepTime);
        }
    }

    @Override
    public void acquire() {
        acquire(1);
    }

    @Override
    public void acquire(long permits) {
        try {
            tryAcquire(permits, Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring rate limit permits", e);
        }
    }

    @Override
    public long getAvailablePermits() {
        return cache.executeAtomic(key, c -> {
            long now = System.currentTimeMillis();
            long windowStart = now - windowDurationMs;
            c.zremrangebyscore(key, 0, windowStart);
            long currentCount = c.zcard(key);
            return Math.max(0L, maxPermits - currentCount);
        });
    }

    @Override
    public void reset() {
        cache.del(key);
    }

    @Override
    public String getName() {
        return name;
    }

    public long getMaxPermits() {
        return maxPermits;
    }

    public long getWindowDurationMs() {
        return windowDurationMs;
    }
}
