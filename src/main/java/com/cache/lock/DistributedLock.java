package com.cache.lock;

import java.util.concurrent.TimeUnit;

/**
 * Interface representing a distributed lock with lease-time, watchdog auto-renewal,
 * safe owner token validation, and re-entrancy support.
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public interface DistributedLock {

    /**
     * Acquires the lock, blocking until acquired.
     * The lock lease is automatically kept alive by a watchdog heartbeat until {@link #unlock()} is called.
     */
    void lock();

    /**
     * Acquires the lock with an explicit lease time, blocking until acquired.
     * The watchdog heartbeat is disabled when an explicit leaseTime is provided;
     * the lock will automatically expire after the lease time if not unlocked earlier.
     *
     * @param leaseTime maximum time to hold the lock before auto-releasing
     * @param unit time unit of leaseTime
     */
    void lock(long leaseTime, TimeUnit unit);

    /**
     * Attempts to acquire the lock immediately without waiting.
     * If acquired, the lock lease is kept alive by a watchdog heartbeat until {@link #unlock()} is called.
     *
     * @return true if the lock was acquired, false otherwise
     */
    boolean tryLock();

    /**
     * Attempts to acquire the lock, waiting up to waitTime.
     * If acquired, the lock lease is kept alive by a watchdog heartbeat until {@link #unlock()} is called.
     *
     * @param waitTime maximum time to wait for the lock
     * @param unit time unit of waitTime
     * @return true if the lock was acquired, false if waitTime elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException;

    /**
     * Attempts to acquire the lock, waiting up to waitTime with an explicit leaseTime.
     * The watchdog heartbeat is disabled when an explicit leaseTime is provided.
     *
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime maximum time to hold the lock before auto-releasing
     * @param unit time unit of waitTime and leaseTime
     * @return true if the lock was acquired, false if waitTime elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock held by the current thread.
     * If the current thread holds re-entrant locks, decrements the hold count.
     * The lock is fully released in the cache only when the hold count reaches 0.
     *
     * @throws IllegalMonitorStateException if the current thread does not hold this lock
     */
    void unlock();

    /**
     * Checks if the lock is currently held by any thread/client.
     *
     * @return true if locked, false otherwise
     */
    boolean isLocked();

    /**
     * Checks if the lock is currently held by the calling thread.
     *
     * @return true if held by current thread, false otherwise
     */
    boolean isHeldByCurrentThread();

    /**
     * Returns the number of holds on this lock by the current thread.
     *
     * @return hold count, or 0 if not held by current thread
     */
    int getHoldCount();

    /**
     * Returns the name / key of this lock.
     *
     * @return lock name
     */
    String getLockName();
}
