package com.cache.ratelimit;

import com.cache.RedisCache;

import java.util.concurrent.TimeUnit;

/**
 * Token bucket rate limiter with continuous replenishment and burst capacity.
 *
 * <p>Tokens refill smoothly over time at {@code refillRatePerSecond} up to {@code capacity}.
 * Any unused tokens accumulate up to capacity to accommodate bursts of traffic.
 *
 * <p>All token replenishments and consumptions are executed atomically on {@link RedisCache}
 * via {@code executeAtomic} to eliminate race conditions.
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final RedisCache cache;
    private final String name;
    private final String key;
    private final long capacity;
    private final double refillRatePerSecond;

    /**
     * Creates a new token bucket rate limiter.
     *
     * @param cache RedisCache instance
     * @param name unique rate limiter name
     * @param capacity maximum token capacity (burst limit)
     * @param refillRatePerSecond number of tokens added per second
     */
    public TokenBucketRateLimiter(RedisCache cache, String name, long capacity, double refillRatePerSecond) {
        if (cache == null) {
            throw new IllegalArgumentException("cache cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be > 0");
        }

        this.cache = cache;
        this.name = name;
        this.key = "ratelimit:tokenbucket:" + name;
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
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
        if (permits > capacity) {
            return false;
        }

        long timeoutMs = unit != null && timeout > 0 ? unit.toMillis(timeout) : 0;
        long deadline = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : System.currentTimeMillis();

        while (true) {
            long waitTimeMs = cache.executeAtomic(key, c -> {
                long now = System.currentTimeMillis();
                Object rawTokens = c.hget(key, "tokens");
                Object rawRefill = c.hget(key, "last_refill");

                double currentTokens;
                if (rawTokens == null || rawRefill == null) {
                    currentTokens = (double) capacity;
                } else {
                    double tokens = Double.parseDouble(rawTokens.toString());
                    long lastRefill = Long.parseLong(rawRefill.toString());
                    long deltaMs = Math.max(0, now - lastRefill);
                    double replenished = (deltaMs / 1000.0) * refillRatePerSecond;
                    currentTokens = Math.min((double) capacity, tokens + replenished);
                }

                if (currentTokens >= permits) {
                    double remainingTokens = currentTokens - permits;
                    c.hset(key, "tokens", String.valueOf(remainingTokens));
                    c.hset(key, "last_refill", String.valueOf(now));

                    // Auto-expire idle bucket after 2x full refill duration
                    long ttlMs = Math.max(60000L, (long) Math.ceil((capacity / refillRatePerSecond) * 1000) * 2);
                    c.pexpire(key, ttlMs);
                    return 0L; // Acquired
                }

                // Not enough tokens: calculate time until required permits replenish
                double shortfall = permits - currentTokens;
                long delayMs = (long) Math.ceil((shortfall / refillRatePerSecond) * 1000.0);

                c.hset(key, "tokens", String.valueOf(currentTokens));
                c.hset(key, "last_refill", String.valueOf(now));
                return Math.max(1L, delayMs);
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
            throw new RuntimeException("Interrupted while acquiring rate limit tokens", e);
        }
    }

    @Override
    public long getAvailablePermits() {
        return cache.executeAtomic(key, c -> {
            long now = System.currentTimeMillis();
            Object rawTokens = c.hget(key, "tokens");
            Object rawRefill = c.hget(key, "last_refill");

            if (rawTokens == null || rawRefill == null) {
                return capacity;
            }

            double tokens = Double.parseDouble(rawTokens.toString());
            long lastRefill = Long.parseLong(rawRefill.toString());
            long deltaMs = Math.max(0, now - lastRefill);
            double replenished = (deltaMs / 1000.0) * refillRatePerSecond;
            double currentTokens = Math.min((double) capacity, tokens + replenished);
            return (long) currentTokens;
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

    public long getCapacity() {
        return capacity;
    }

    public double getRefillRatePerSecond() {
        return refillRatePerSecond;
    }
}
