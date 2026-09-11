package com.cache.ratelimit;

import java.util.concurrent.TimeUnit;

/**
 * Universal interface for distributed rate limiting.
 *
 * <p>Supports immediate non-blocking acquisition ({@link #tryAcquire()}),
 * timeout-bounded acquisition ({@link #tryAcquire(long, TimeUnit)}),
 * and blocking acquisition ({@link #acquire()}).
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public interface RateLimiter {

    /**
     * Attempts to acquire 1 permit immediately without blocking.
     *
     * @return true if permit was acquired, false if rate limited
     */
    boolean tryAcquire();

    /**
     * Attempts to acquire the specified number of permits immediately without blocking.
     *
     * @param permits number of permits to acquire (must be &gt; 0)
     * @return true if permits were acquired, false if rate limited
     */
    boolean tryAcquire(long permits);

    /**
     * Attempts to acquire 1 permit, waiting up to the specified timeout if needed.
     *
     * @param timeout maximum time to wait
     * @param unit time unit of timeout
     * @return true if permit was acquired, false if timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Attempts to acquire the specified number of permits, waiting up to the specified timeout if needed.
     *
     * @param permits number of permits (must be &gt; 0)
     * @param timeout maximum time to wait
     * @param unit time unit of timeout
     * @return true if permits were acquired, false if timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    boolean tryAcquire(long permits, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Acquires 1 permit, blocking indefinitely until available.
     */
    void acquire();

    /**
     * Acquires the specified number of permits, blocking indefinitely until available.
     *
     * @param permits number of permits (must be &gt; 0)
     */
    void acquire(long permits);

    /**
     * Returns the currently available permits or tokens.
     *
     * @return number of available permits
     */
    long getAvailablePermits();

    /**
     * Resets the rate limiter state.
     */
    void reset();

    /**
     * Returns the resource name associated with this rate limiter.
     *
     * @return resource name
     */
    String getName();
}
