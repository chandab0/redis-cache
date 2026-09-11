package com.cache.ratelimit;

import com.cache.RedisCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RateLimiterTest {

    private RedisCache cache;

    @Before
    public void setUp() {
        cache = new RedisCache();
    }

    @After
    public void tearDown() {
        cache.shutdown();
    }

    // ==================== SLIDING WINDOW TESTS ====================

    @Test
    public void testSlidingWindowBasicAcquire() {
        RateLimiter limiter = cache.getSlidingWindowRateLimiter("basicSliding", 5, 1, TimeUnit.SECONDS);

        assertEquals(5, limiter.getAvailablePermits());
        for (int i = 0; i < 5; i++) {
            assertTrue("Permit " + i + " should be acquired", limiter.tryAcquire());
        }

        assertFalse("6th permit should be rejected", limiter.tryAcquire());
        assertEquals(0, limiter.getAvailablePermits());
    }

    @Test
    public void testSlidingWindowSlidesOverTime() throws InterruptedException {
        RateLimiter limiter = cache.getSlidingWindowRateLimiter("slidingTime", 3, 200, TimeUnit.MILLISECONDS);

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        // Wait for sliding window to expire old entries
        Thread.sleep(250);

        assertTrue("Should be able to acquire after window slides", limiter.tryAcquire());
        assertEquals(2, limiter.getAvailablePermits());
    }

    @Test
    public void testSlidingWindowMultiPermits() {
        RateLimiter limiter = cache.getSlidingWindowRateLimiter("multiPermits", 10, 1, TimeUnit.SECONDS);

        assertTrue(limiter.tryAcquire(6));
        assertEquals(4, limiter.getAvailablePermits());

        assertFalse("Cannot acquire 5 when only 4 left", limiter.tryAcquire(5));
        assertTrue("Can acquire remaining 4", limiter.tryAcquire(4));
        assertEquals(0, limiter.getAvailablePermits());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    public void testSlidingWindowBlockingAcquire() throws Exception {
        RateLimiter limiter = cache.getSlidingWindowRateLimiter("blockingSliding", 2, 200, TimeUnit.MILLISECONDS);

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        long start = System.currentTimeMillis();
        boolean acquired = limiter.tryAcquire(1, 500, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Blocking acquire should succeed after window slides", acquired);
        assertTrue("Should have waited for window to slide", elapsed >= 150);
    }

    // ==================== TOKEN BUCKET TESTS ====================

    @Test
    public void testTokenBucketBasicAcquire() {
        RateLimiter limiter = cache.getTokenBucketRateLimiter("basicBucket", 5, 2.0);

        assertEquals(5, limiter.getAvailablePermits());
        for (int i = 0; i < 5; i++) {
            assertTrue("Token " + i + " should be acquired", limiter.tryAcquire());
        }

        assertFalse("Bucket should be empty", limiter.tryAcquire());
        assertEquals(0, limiter.getAvailablePermits());
    }

    @Test
    public void testTokenBucketRefillOverTime() throws InterruptedException {
        // 5 tokens/sec = 1 token every 200ms
        RateLimiter limiter = cache.getTokenBucketRateLimiter("refillBucket", 5, 5.0);

        assertTrue(limiter.tryAcquire(5));
        assertFalse(limiter.tryAcquire());

        // Wait 450ms -> should refill ~2 tokens
        Thread.sleep(450);

        assertTrue(limiter.tryAcquire(2));
        assertFalse(limiter.tryAcquire(2));
    }

    @Test
    public void testTokenBucketBlockingAcquire() throws Exception {
        // 10 tokens/sec = 1 token every 100ms
        RateLimiter limiter = cache.getTokenBucketRateLimiter("blockingBucket", 2, 10.0);

        assertTrue(limiter.tryAcquire(2));
        assertFalse(limiter.tryAcquire());

        long start = System.currentTimeMillis();
        boolean acquired = limiter.tryAcquire(1, 400, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Blocking token acquire should succeed after refill", acquired);
        assertTrue("Should have waited for token replenishment", elapsed >= 80);
    }

    @Test
    public void testTokenBucketCapacityCap() throws InterruptedException {
        RateLimiter limiter = cache.getTokenBucketRateLimiter("cappedBucket", 5, 20.0);

        // Sleep 300ms (at 20 tokens/sec would generate 6 tokens)
        Thread.sleep(300);

        // Available tokens must still be capped at max capacity 5
        assertEquals(5, limiter.getAvailablePermits());
    }

    // ==================== DIRECT ZREMRANGEBYSCORE TEST ====================

    @Test
    public void testZremrangebyscore() {
        String key = "test:zset:score";
        cache.zadd(key, 10.0, "m10");
        cache.zadd(key, 20.0, "m20");
        cache.zadd(key, 30.0, "m30");
        cache.zadd(key, 40.0, "m40");
        cache.zadd(key, 50.0, "m50");

        assertEquals(5, cache.zcard(key));

        long removed = cache.zremrangebyscore(key, 20.0, 40.0);
        assertEquals(3, removed);
        assertEquals(2, cache.zcard(key));

        List<String> remaining = cache.zrange(key, 0, -1);
        assertEquals(2, remaining.size());
        assertTrue(remaining.contains("m10"));
        assertTrue(remaining.contains("m50"));
    }

    // ==================== CONCURRENCY & RACE CONDITION TESTS ====================

    @Test
    public void testConcurrentSlidingWindowNoOverAdmission() throws InterruptedException {
        final int maxPermits = 40;
        final RateLimiter limiter = cache.getSlidingWindowRateLimiter("concurrentSliding", maxPermits, 5, TimeUnit.SECONDS);

        final int numThreads = 20;
        final int attemptsPerThread = 5; // Total 100 attempts for 40 permits
        final AtomicInteger granted = new AtomicInteger(0);
        final AtomicInteger rejected = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < attemptsPerThread; j++) {
                        if (limiter.tryAcquire()) {
                            granted.incrementAndGet();
                        } else {
                            rejected.incrementAndGet();
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
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals("Exactly maxPermits should have been granted", maxPermits, granted.get());
        assertEquals("Total attempts minus maxPermits should have been rejected", 100 - maxPermits, rejected.get());
    }

    @Test
    public void testConcurrentTokenBucketNoOverAdmission() throws InterruptedException {
        final int capacity = 40;
        // Very slow refill so tokens don't meaningfully refill during the burst
        final RateLimiter limiter = cache.getTokenBucketRateLimiter("concurrentBucket", capacity, 0.001);

        final int numThreads = 20;
        final int attemptsPerThread = 5; // Total 100 attempts for 40 tokens
        final AtomicInteger granted = new AtomicInteger(0);
        final AtomicInteger rejected = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < attemptsPerThread; j++) {
                        if (limiter.tryAcquire()) {
                            granted.incrementAndGet();
                        } else {
                            rejected.incrementAndGet();
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
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals("Exactly capacity tokens should have been granted", capacity, granted.get());
        assertEquals("Total attempts minus capacity should have been rejected", 100 - capacity, rejected.get());
    }

    @Test
    public void testReset() {
        RateLimiter sliding = cache.getSlidingWindowRateLimiter("resetSliding", 3, 1, TimeUnit.MINUTES);
        assertTrue(sliding.tryAcquire(3));
        assertFalse(sliding.tryAcquire());

        sliding.reset();
        assertEquals(3, sliding.getAvailablePermits());
        assertTrue(sliding.tryAcquire(3));

        RateLimiter bucket = cache.getTokenBucketRateLimiter("resetBucket", 4, 1.0);
        assertTrue(bucket.tryAcquire(4));
        assertFalse(bucket.tryAcquire());

        bucket.reset();
        assertEquals(4, bucket.getAvailablePermits());
        assertTrue(bucket.tryAcquire(4));
    }
}
