package com.cache;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

public class RedisCacheTest {
    private RedisCache cache;

    @Before
    public void setUp() {
        cache = new RedisCache();
    }

    @After
    public void tearDown() {
        cache.shutdown();
    }

    // ==================== STRING TESTS ====================

    @Test
    public void testSetGet() {
        cache.set("key1", "value1");
        assertEquals("value1", cache.<String>get("key1"));
    }

    @Test
    public void testSetWithTTL() throws InterruptedException {
        cache.set("temp", "value", 100);
        assertEquals("value", cache.<String>get("temp"));
        Thread.sleep(150);
        assertNull(cache.get("temp"));
    }

    @Test
    public void testSetnx() {
        assertTrue(cache.setnx("key", "value1"));
        assertFalse(cache.setnx("key", "value2"));
        assertEquals("value1", cache.<String>get("key"));
    }

    @Test
    public void testGetOrCompute() {
        String value = cache.getOrCompute("computed", k -> "computed_" + k);
        assertEquals("computed_computed", value);
        assertEquals("computed_computed", cache.<String>get("computed"));
    }

    @Test
    public void testIncrDecr() {
        cache.set("counter", 10);
        assertEquals(11, cache.incr("counter"));
        assertEquals(15, cache.incrBy("counter", 4));
        assertEquals(14, cache.decr("counter"));
        assertEquals(10, cache.decrBy("counter", 4));
    }

    @Test
    public void testMsetMget() {
        Map<String, Object> data = new HashMap<>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        data.put("k3", "v3");
        cache.mset(data);

        List<String> values = cache.mget("k1", "k2", "k3");
        assertEquals(Arrays.asList("v1", "v2", "v3"), values);
    }

    @Test
    public void testAppend() {
        cache.set("str", "Hello");
        assertEquals(11, cache.append("str", " World"));
        assertEquals("Hello World", cache.<String>get("str"));
    }

    // ==================== KEY TESTS ====================

    @Test
    public void testExists() {
        cache.set("key", "value");
        assertTrue(cache.exists("key"));
        assertFalse(cache.exists("nonexistent"));
    }

    @Test
    public void testDel() {
        cache.set("key1", "v1");
        cache.set("key2", "v2");
        assertEquals(2, cache.del("key1", "key2", "key3"));
        assertFalse(cache.exists("key1"));
        assertFalse(cache.exists("key2"));
    }

    @Test
    public void testExpire() throws InterruptedException {
        cache.set("key", "value");
        assertTrue(cache.expire("key", 1));
        Thread.sleep(1100);
        assertNull(cache.get("key"));
    }

    @Test
    public void testTTL() throws InterruptedException {
        cache.set("key", "value");
        cache.expire("key", 2);
        long ttl = cache.ttl("key");
        assertTrue(ttl > 0 && ttl <= 2);
    }

    @Test
    public void testKeys() {
        cache.set("user:1", "John");
        cache.set("user:2", "Jane");
        cache.set("session:1", "sess1");
        Set<String> userKeys = cache.keys("user:*");
        assertEquals(2, userKeys.size());
        assertTrue(userKeys.contains("user:1"));
        assertTrue(userKeys.contains("user:2"));
    }

    @Test
    public void testRename() {
        cache.set("old", "value");
        cache.rename("old", "new");
        assertNull(cache.get("old"));
        assertEquals("value", cache.<String>get("new"));
    }

    // ==================== LIST TESTS ====================

    @Test
    public void testListPushPop() {
        assertEquals(3, cache.lpush("list", "a", "b", "c"));
        assertEquals(3, cache.llen("list"));
        assertEquals("c", cache.<String>lpop("list"));
        assertEquals("a", cache.<String>rpop("list"));
        assertEquals(1, cache.llen("list"));
    }

    @Test
    public void testListRange() {
        cache.rpush("list", "1", "2", "3", "4", "5");
        List<String> range = cache.lrange("list", 0, 2);
        assertEquals(Arrays.asList("1", "2", "3"), range);
    }

    @Test
    public void testListIndex() {
        cache.rpush("list", "a", "b", "c");
        assertEquals("a", cache.<String>lindex("list", 0));
        assertEquals("c", cache.<String>lindex("list", -1));
        assertNull(cache.lindex("list", 10));
    }

    @Test
    public void testListTrim() {
        cache.rpush("list", "1", "2", "3", "4", "5");
        cache.ltrim("list", 1, 3);
        List<String> result = cache.lrange("list", 0, -1);
        assertEquals(Arrays.asList("2", "3", "4"), result);
    }

    @Test
    public void testListRem() {
        cache.rpush("list", "a", "b", "a", "c", "a");
        assertEquals(2, cache.lrem("list", 2, "a"));
        List<String> result = cache.lrange("list", 0, -1);
        assertEquals(Arrays.asList("b", "c", "a"), result);
    }

    // ==================== SET TESTS ====================

    @Test
    public void testSetAddRemove() {
        assertEquals(3, cache.sadd("set", "a", "b", "c"));
        assertTrue(cache.sismember("set", "a"));
        assertEquals(3, cache.scard("set"));
        assertEquals(1, cache.srem("set", "b"));
        assertFalse(cache.sismember("set", "b"));
    }

    @Test
    public void testSetMembers() {
        cache.sadd("set", "a", "b", "c");
        Set<String> members = cache.smembers("set");
        assertEquals(3, members.size());
        assertTrue(members.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testSetPop() {
        cache.sadd("set", "a", "b", "c");
        String popped = cache.spop("set");
        assertNotNull(popped);
        assertEquals(2, cache.scard("set"));
    }

    @Test
    public void testSetDiffInterUnion() {
        cache.sadd("set1", "a", "b", "c");
        cache.sadd("set2", "b", "c", "d");

        Set<String> diff = cache.sdiff("set1", "set2");
        assertEquals(1, diff.size());
        assertTrue(diff.contains("a"));

        Set<String> inter = cache.sinter("set1", "set2");
        assertEquals(2, inter.size());
        assertTrue(inter.containsAll(Arrays.asList("b", "c")));

        Set<String> union = cache.sunion("set1", "set2");
        assertEquals(4, union.size());
    }

    // ==================== HASH TESTS ====================

    @Test
    public void testHashSetGet() {
        assertEquals(1, cache.hset("hash", "field1", "value1"));
        assertEquals(0, cache.hset("hash", "field1", "updated"));
        assertEquals("updated", cache.<String>hget("hash", "field1"));
    }

    @Test
    public void testHashMultiple() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);
        cache.hmset("user:1", data);

        List<Object> values = cache.hmget("user:1", "name", "age", "city");
        assertEquals("John", values.get(0));
        assertEquals(30, values.get(1));
        assertNull(values.get(2));

        Map<String, Object> all = cache.hgetall("user:1");
        assertEquals(2, all.size());
    }

    @Test
    public void testHashIncr() {
        cache.hset("hash", "counter", 10);
        assertEquals(15, cache.hincrBy("hash", "counter", 5));
        assertEquals(15.5, cache.hincrByFloat("hash", "counter", 0.5), 0.001);
    }

    @Test
    public void testHashExists() {
        cache.hset("hash", "field", "value");
        assertTrue(cache.hexists("hash", "field"));
        assertFalse(cache.hexists("hash", "nonexistent"));
    }

    @Test
    public void testHashDel() {
        cache.hset("hash", "f1", "v1");
        cache.hset("hash", "f2", "v2");
        assertEquals(2, cache.hdel("hash", "f1", "f2"));
        assertEquals(0, cache.hlen("hash"));
    }

    // ==================== SORTED SET TESTS ====================

    @Test
    public void testSortedSetAdd() {
        assertEquals(1, cache.zadd("zset", 100, "member1"));
        assertEquals(0, cache.zadd("zset", 150, "member1")); // Update score
        assertEquals(150.0, cache.zscore("zset", "member1"), 0.001);
    }

    @Test
    public void testSortedSetRank() {
        cache.zadd("zset", 100, "a");
        cache.zadd("zset", 200, "b");
        cache.zadd("zset", 150, "c");

        assertEquals(0L, cache.zrank("zset", "a").longValue());
        assertEquals(1L, cache.zrank("zset", "c").longValue());
        assertEquals(2L, cache.zrank("zset", "b").longValue());

        assertEquals(0L, cache.zrevrank("zset", "b").longValue());
    }

    @Test
    public void testSortedSetRange() {
        cache.zadd("zset", 100, "a");
        cache.zadd("zset", 200, "b");
        cache.zadd("zset", 150, "c");

        List<String> range = cache.zrange("zset", 0, 1);
        assertEquals(Arrays.asList("a", "c"), range);

        List<String> revRange = cache.zrevrange("zset", 0, 1);
        assertEquals(Arrays.asList("b", "c"), revRange);
    }

    @Test
    public void testSortedSetIncr() {
        cache.zadd("zset", 100, "member");
        assertEquals(150.0, cache.zincrby("zset", 50, "member"), 0.001);
    }

    @Test
    public void testSortedSetCount() {
        cache.zadd("zset", 50, "a");
        cache.zadd("zset", 100, "b");
        cache.zadd("zset", 150, "c");
        cache.zadd("zset", 200, "d");

        assertEquals(2, cache.zcount("zset", 75, 175));
    }

    // ==================== UTILITY TESTS ====================

    @Test
    public void testInfo() {
        cache.set("k1", "v1");
        cache.lpush("list", "item");
        cache.sadd("set", "member");
        cache.hset("hash", "field", "value");
        cache.zadd("zset", 100, "member");

        Map<String, Object> info = cache.info();
        assertEquals(1, ((Number) info.get("string_keys")).intValue());
        assertEquals(1, ((Number) info.get("list_keys")).intValue());
        assertEquals(1, ((Number) info.get("set_keys")).intValue());
        assertEquals(1, ((Number) info.get("hash_keys")).intValue());
        assertEquals(1, ((Number) info.get("sorted_set_keys")).intValue());
    }

    @Test
    public void testFlushall() {
        cache.set("k1", "v1");
        cache.lpush("list", "item");
        cache.flushall();
        assertEquals(0, cache.keys().size());
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threads = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        cache.set("key_" + j, "value_" + j);
                        cache.get("key_" + j);
                        cache.incr("counter");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * iterations, cache.incrBy("counter", 0));
    }

    // ==================== V1.0.2 ENHANCEMENT TESTS ====================

    @Test
    public void testListTtlAndExpiration() throws InterruptedException {
        cache.rpush("tasks", "task1", "task2");
        assertEquals(2, cache.llen("tasks"));
        assertTrue(cache.pexpire("tasks", 100));
        assertTrue(cache.pttl("tasks") > 0);

        Thread.sleep(150);

        assertNull(cache.lpop("tasks"));
        assertEquals(0, cache.llen("tasks"));
        assertFalse(cache.exists("tasks"));
        assertEquals(-2, cache.ttl("tasks"));
    }

    @Test
    public void testSetTtlAndExpiration() throws InterruptedException {
        cache.sadd("tags", "java", "redis");
        assertEquals(2, cache.scard("tags"));
        assertTrue(cache.expire("tags", 1));
        assertTrue(cache.ttl("tags") > 0);

        Thread.sleep(1100);

        assertEquals(0, cache.scard("tags"));
        assertFalse(cache.sismember("tags", "java"));
        assertFalse(cache.exists("tags"));
    }

    @Test
    public void testHashTtlAndExpiration() throws InterruptedException {
        cache.hset("profile", "name", "John");
        assertEquals("John", cache.<String>hget("profile", "name"));
        assertTrue(cache.pexpire("profile", 100));

        Thread.sleep(150);

        assertNull(cache.hget("profile", "name"));
        assertEquals(0, cache.hlen("profile"));
        assertFalse(cache.exists("profile"));
    }

    @Test
    public void testSortedSetTtlAndExpiration() throws InterruptedException {
        cache.zadd("leaderboard", 100.0, "alice");
        assertEquals(100.0, cache.zscore("leaderboard", "alice"), 0.001);
        assertTrue(cache.pexpire("leaderboard", 100));

        Thread.sleep(150);

        assertNull(cache.zscore("leaderboard", "alice"));
        assertEquals(0, cache.zcard("leaderboard"));
        assertFalse(cache.exists("leaderboard"));
    }

    @Test
    public void testUniversalPersistAndTtl() {
        cache.sadd("myset", "val");
        cache.expire("myset", 60);
        assertTrue(cache.ttl("myset") > 0);
        assertTrue(cache.persist("myset"));
        assertEquals(-1, cache.ttl("myset"));
    }

    @Test
    public void testActiveCleanupPurgesNonStringKeys() throws InterruptedException {
        RedisCache fastCache = new RedisCache(50);
        try {
            fastCache.rpush("expList", "a", "b");
            fastCache.sadd("expSet", "s1");
            fastCache.hset("expHash", "f", "v");
            fastCache.zadd("expZset", 10, "m");

            fastCache.pexpire("expList", 60);
            fastCache.pexpire("expSet", 60);
            fastCache.pexpire("expHash", 60);
            fastCache.pexpire("expZset", 60);

            Thread.sleep(200);

            Map<String, Object> info = fastCache.info();
            assertEquals(0, ((Number) info.get("list_keys")).intValue());
            assertEquals(0, ((Number) info.get("set_keys")).intValue());
            assertEquals(0, ((Number) info.get("hash_keys")).intValue());
            assertEquals(0, ((Number) info.get("sorted_set_keys")).intValue());
            assertTrue(fastCache.getExpiredCount() >= 4);
        } finally {
            fastCache.shutdown();
        }
    }

    @Test
    public void testConcurrentListAccessNoException() throws InterruptedException {
        int threads = 8;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        cache.lpush("concurrentList", "val_" + threadId + "_" + j);
                        cache.lrange("concurrentList", 0, 5);
                        cache.llen("concurrentList");
                        if (j % 2 == 0) {
                            cache.rpop("concurrentList");
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(cache.llen("concurrentList") > 0);
    }

    @Test
    public void testConcurrentSortedSetAccess() throws InterruptedException {
        int threads = 8;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        cache.zadd("concurrentZSet", j * 1.5, "item_" + threadId + "_" + j);
                        cache.zrank("concurrentZSet", "item_" + threadId + "_" + j);
                        cache.zrange("concurrentZSet", 0, 3);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threads * iterations, cache.zcard("concurrentZSet"));
    }

    @Test
    public void testConcurrentHashIncrBy() throws InterruptedException {
        int threads = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        cache.hincrBy("metrics", "hits", 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(1000L, cache.hincrBy("metrics", "hits", 0));
    }

    @Test
    public void testBlpopImmediateAndBlocking() throws InterruptedException, ExecutionException, TimeoutException {
        // Immediate pop when data exists
        cache.rpush("jobQueue", "immediateJob");
        assertEquals("immediateJob", cache.<String>blpop("jobQueue", 1));

        // Blocking pop waiting for producer
        ExecutorService single = Executors.newSingleThreadExecutor();
        Future<String> future = single.submit(() -> cache.<String>blpop("jobQueue", 3));

        // Let consumer block
        Thread.sleep(150);
        assertFalse(future.isDone());

        // Push data from main thread
        cache.rpush("jobQueue", "delayedJob");

        String popped = future.get(2, TimeUnit.SECONDS);
        assertEquals("delayedJob", popped);
        single.shutdown();
    }

    @Test
    public void testBlpopTimeout() {
        long start = System.currentTimeMillis();
        String result = cache.blpop("emptyQueue", 1);
        long elapsed = System.currentTimeMillis() - start;

        assertNull(result);
        assertTrue(elapsed >= 900); // Should have waited ~1s
    }

    @Test
    public void testBrpopBlocking() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService single = Executors.newSingleThreadExecutor();
        Future<String> future = single.submit(() -> cache.<String>brpop("rJobQueue", 3));

        Thread.sleep(150);
        cache.rpush("rJobQueue", "item1", "item2");

        String popped = future.get(2, TimeUnit.SECONDS);
        assertEquals("item2", popped); // RPOP pops from tail
        single.shutdown();
    }

    @Test
    public void testAllkeysLruEviction() {
        RedisCache lruCache = new RedisCache(3, EvictionPolicy.ALLKEYS_LRU);
        try {
            lruCache.set("k1", "v1");
            lruCache.set("k2", "v2");
            lruCache.set("k3", "v3");

            // Access k1 and k2 to update their LRU time
            lruCache.get("k1");
            lruCache.get("k2");

            // Insert 4th key -> k3 should be evicted
            lruCache.set("k4", "v4");

            assertEquals(3, lruCache.keys().size());
            assertNotNull(lruCache.get("k1"));
            assertNotNull(lruCache.get("k2"));
            assertNotNull(lruCache.get("k4"));
            assertNull(lruCache.get("k3")); // Evicted
            assertEquals(1, lruCache.getEvictedCount());
        } finally {
            lruCache.shutdown();
        }
    }

    @Test
    public void testVolatileLruEviction() {
        RedisCache vlruCache = new RedisCache(3, EvictionPolicy.VOLATILE_LRU);
        try {
            vlruCache.set("persistKey", "val"); // No TTL
            vlruCache.set("v1", "val1", 60000); // TTL
            vlruCache.set("v2", "val2", 60000); // TTL

            // Access v1
            vlruCache.get("v1");

            // Insert 4th key -> v2 (oldest volatile) should be evicted, NOT persistKey
            vlruCache.set("v3", "val3", 60000);

            assertEquals(3, vlruCache.keys().size());
            assertNotNull(vlruCache.get("persistKey"));
            assertNotNull(vlruCache.get("v1"));
            assertNotNull(vlruCache.get("v3"));
            assertNull(vlruCache.get("v2")); // Evicted
        } finally {
            vlruCache.shutdown();
        }
    }

    @Test
    public void testVolatileTtlEviction() {
        RedisCache vttlCache = new RedisCache(3, EvictionPolicy.VOLATILE_TTL);
        try {
            vttlCache.set("persistKey", "val"); // No TTL
            vttlCache.set("longTtl", "val1", 100000); // Long TTL
            vttlCache.set("shortTtl", "val2", 5000);  // Short TTL

            // Insert 4th key -> shortTtl should be evicted first
            vttlCache.set("newKey", "val3");

            assertEquals(3, vttlCache.keys().size());
            assertNotNull(vttlCache.get("persistKey"));
            assertNotNull(vttlCache.get("longTtl"));
            assertNotNull(vttlCache.get("newKey"));
            assertNull(vttlCache.get("shortTtl")); // Evicted
        } finally {
            vttlCache.shutdown();
        }
    }

    @Test
    public void testAllkeysFifoEviction() throws InterruptedException {
        RedisCache fifoCache = new RedisCache(3, EvictionPolicy.ALLKEYS_FIFO);
        try {
            fifoCache.set("k1", "v1");
            Thread.sleep(10);
            fifoCache.set("k2", "v2");
            Thread.sleep(10);
            fifoCache.set("k3", "v3");

            // Access k1 (FIFO should ignore access order)
            fifoCache.get("k1");

            // Insert k4 -> k1 was created first, so it must be evicted
            fifoCache.set("k4", "v4");

            assertEquals(3, fifoCache.keys().size());
            assertNull(fifoCache.get("k1")); // Evicted
            assertNotNull(fifoCache.get("k2"));
            assertNotNull(fifoCache.get("k3"));
            assertNotNull(fifoCache.get("k4"));
        } finally {
            fifoCache.shutdown();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNoEvictionThrowsOnOom() {
        RedisCache noEvictCache = new RedisCache(2, EvictionPolicy.NO_EVICTION);
        try {
            noEvictCache.set("k1", "v1");
            noEvictCache.set("k2", "v2");
            // 3rd key should throw IllegalStateException
            noEvictCache.set("k3", "v3");
        } finally {
            noEvictCache.shutdown();
        }
    }

    @Test
    public void testEvictionMetricsInInfo() {
        RedisCache testCache = new RedisCache(2, EvictionPolicy.ALLKEYS_LRU);
        try {
            testCache.set("a", "1");
            testCache.set("b", "2");
            testCache.set("c", "3");

            Map<String, Object> info = testCache.info();
            assertEquals(2, ((Number) info.get("max_capacity")).intValue());
            assertEquals("ALLKEYS_LRU", info.get("eviction_policy"));
            assertEquals(1L, ((Number) info.get("evicted_keys")).longValue());
        } finally {
            testCache.shutdown();
        }
    }
}
