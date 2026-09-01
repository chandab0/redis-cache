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
}
