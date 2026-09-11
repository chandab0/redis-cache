package com.cache.lock;

import com.cache.RedisCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DistributedLockTest {

    private RedisCache cache;

    @Before
    public void setUp() {
        cache = new RedisCache();
    }

    @After
    public void tearDown() {
        cache.shutdown();
    }

    @Test
    public void testBasicLockUnlock() {
        DistributedLock lock = cache.getLock("resource1");
        assertFalse(lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
        assertEquals(0, lock.getHoldCount());

        lock.lock();
        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());
        assertEquals(1, lock.getHoldCount());

        lock.unlock();
        assertFalse(lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
        assertEquals(0, lock.getHoldCount());
    }

    @Test
    public void testTryLock() {
        DistributedLock lock = cache.getLock("resource_try");
        assertTrue(lock.tryLock());
        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());

        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    public void testLockPreventsConcurrentAccess() throws Exception {
        DistributedLock lock1 = cache.getLock("sharedResource");
        DistributedLock lock2 = cache.getLock("sharedResource");

        lock1.lock();
        assertTrue(lock1.isHeldByCurrentThread());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> attempt = executor.submit(() -> lock2.tryLock(50, TimeUnit.MILLISECONDS));

        assertFalse("Second thread should not acquire held lock", attempt.get());

        lock1.unlock();

        Future<Boolean> attemptAfterRelease = executor.submit(() -> {
            boolean acquired = lock2.tryLock(100, TimeUnit.MILLISECONDS);
            if (acquired) {
                lock2.unlock();
            }
            return acquired;
        });

        assertTrue("Second thread should acquire after release", attemptAfterRelease.get());
        executor.shutdown();
    }

    @Test
    public void testTryLockWithTimeout() throws Exception {
        DistributedLock lock = cache.getLock("timeoutResource");
        lock.lock();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            DistributedLock clientLock = cache.getLock("timeoutResource");
            return clientLock.tryLock(2, TimeUnit.SECONDS);
        });

        // Let the second thread wait in tryLock
        Thread.sleep(150);
        assertFalse(future.isDone());

        // Release lock from main thread
        lock.unlock();

        assertTrue("Second thread should acquire lock within waitTime", future.get(2, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    public void testTryLockTimeoutExpires() throws Exception {
        DistributedLock lock = cache.getLock("expireResource");
        lock.lock();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        long start = System.currentTimeMillis();
        Future<Boolean> future = executor.submit(() -> {
            DistributedLock clientLock = cache.getLock("expireResource");
            return clientLock.tryLock(300, TimeUnit.MILLISECONDS);
        });

        assertFalse(future.get(1, TimeUnit.SECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 280);

        lock.unlock();
        executor.shutdown();
    }

    @Test
    public void testReentrancy() {
        DistributedLock lock = cache.getLock("reentrantResource");

        lock.lock();
        assertEquals(1, lock.getHoldCount());

        lock.lock();
        assertEquals(2, lock.getHoldCount());

        assertTrue(lock.tryLock());
        assertEquals(3, lock.getHoldCount());

        lock.unlock();
        assertEquals(2, lock.getHoldCount());
        assertTrue(lock.isLocked());

        lock.unlock();
        assertEquals(1, lock.getHoldCount());
        assertTrue(lock.isLocked());

        lock.unlock();
        assertEquals(0, lock.getHoldCount());
        assertFalse(lock.isLocked());
    }

    @Test(expected = IllegalMonitorStateException.class)
    public void testUnlockByNonHolderThrows() throws Exception {
        DistributedLock lock = cache.getLock("unheldResource");
        lock.unlock();
    }

    @Test
    public void testUnlockByDifferentThreadThrows() throws Exception {
        DistributedLock lock = cache.getLock("differentThreadResource");
        lock.lock();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            DistributedLock otherLock = cache.getLock("differentThreadResource");
            otherLock.unlock();
        });

        try {
            future.get();
            fail("Expected ExecutionException caused by IllegalMonitorStateException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalMonitorStateException);
        } finally {
            lock.unlock();
            executor.shutdown();
        }
    }

    @Test
    public void testSafeFencingTokenRelease() {
        // Direct test of compareAndDelete ensuring token validation
        String key = "test:fencing";
        cache.set(key, "ownerA");

        assertFalse("Owner B cannot delete Owner A's lock", cache.compareAndDelete(key, "ownerB"));
        assertEquals("ownerA", cache.get(key));

        assertTrue("Owner A can safely delete its own lock", cache.compareAndDelete(key, "ownerA"));
        assertNull(cache.get(key));
    }

    @Test
    public void testWatchdogRenewal() throws InterruptedException {
        // Use a short 300ms watchdog timeout so heartbeat runs every 100ms
        DistributedLock lock = cache.getLock("watchdogResource", 300);

        lock.lock(); // default watchdog enabled
        assertTrue(lock.isLocked());

        // Sleep 600ms - longer than initial 300ms watchdog lease
        Thread.sleep(600);

        // Lock should still be held because watchdog renewed it
        assertTrue("Watchdog should have kept lock alive", lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());

        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    public void testExplicitLeaseTimeDoesNotRenew() throws InterruptedException {
        DistributedLock lock = cache.getLock("leaseResource");

        // Explicit lease of 200ms -> watchdog is disabled
        boolean acquired = lock.tryLock(100, 200, TimeUnit.MILLISECONDS);
        assertTrue(acquired);
        assertTrue(lock.isLocked());

        // Sleep 350ms -> should expire
        Thread.sleep(350);

        assertFalse("Lock should have expired after explicit lease time", lock.isLocked());
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    public void testRedlockQuorumSuccess() throws Exception {
        RedisCache c1 = new RedisCache();
        RedisCache c2 = new RedisCache();
        RedisCache c3 = new RedisCache();

        try {
            DistributedLock redlock = new Redlock("multiClusterLock", c1, c2, c3);
            assertTrue(redlock.tryLock(1, TimeUnit.SECONDS));
            assertTrue(redlock.isLocked());
            assertTrue(redlock.isHeldByCurrentThread());
            assertEquals(1, redlock.getHoldCount());

            // Check re-entrancy on Redlock
            assertTrue(redlock.tryLock());
            assertEquals(2, redlock.getHoldCount());

            redlock.unlock();
            assertEquals(1, redlock.getHoldCount());
            assertTrue(redlock.isLocked());

            redlock.unlock();
            assertEquals(0, redlock.getHoldCount());
            assertFalse(redlock.isLocked());
        } finally {
            c1.shutdown();
            c2.shutdown();
            c3.shutdown();
        }
    }

    @Test
    public void testRedlockRollbackOnQuorumFailure() {
        RedisCache c1 = new RedisCache();
        RedisCache c2 = new RedisCache();
        RedisCache c3 = new RedisCache();

        try {
            // Block 2 out of 3 instances so quorum (2) cannot be reached
            c1.set("redlock:blockedQuorum", "foreignClient");
            c2.set("redlock:blockedQuorum", "foreignClient");

            DistributedLock redlock = new Redlock("blockedQuorum", c1, c2, c3);
            boolean acquired = redlock.tryLock();
            assertFalse("Should fail because quorum of 2 was not achieved", acquired);

            // Verify rollback: c3 should have been unlocked / cleared
            assertFalse("Instance c3 should have been rolled back and left unlocked", c3.exists("redlock:blockedQuorum"));
        } finally {
            c1.shutdown();
            c2.shutdown();
            c3.shutdown();
        }
    }

    @Test
    public void testHighConcurrencyLockContention() throws InterruptedException {
        final int numThreads = 10;
        final int incrementsPerThread = 50;
        final AtomicInteger sharedCounter = new AtomicInteger(0);
        final DistributedLock lock = cache.getLock("concurrencyCounterLock");

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        lock.lock();
                        try {
                            int current = sharedCounter.get();
                            // simulate work
                            Thread.sleep(1);
                            sharedCounter.set(current + 1);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("All threads should finish within 30 seconds", doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals("Counter should equal exact total increments", numThreads * incrementsPerThread, sharedCounter.get());
        assertFalse("Lock should be fully unlocked after all threads finish", lock.isLocked());
    }
}
