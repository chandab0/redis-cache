# Redis-like Cache

A lightweight, high-performance Redis-like in-memory cache library for Java. Zero dependencies, thread-safe, and ready to embed in any application.

## Features

- **Multiple Data Structures**: Strings, Lists, Sets, Sorted Sets, Hashes
- **Universal TTL Support**: Automatic expiration on any key across all data structures with configurable cleanup intervals
- **True Thread-Safety**: Concurrency-safe collections, synchronized list mutations, and ReadWriteLock-protected Sorted Sets
- **Real Blocking Queues**: `blpop` and `brpop` with condition-based blocking and timeouts for producer-consumer workflows
- **Distributed Locks & Redlock**: Lease-based locks with automatic watchdog heartbeat renewal, safe owner token validation, re-entrancy, and multi-node Redlock consensus
- **Distributed Rate Limiting & Token Bucket**: Sliding window log (zero boundary-bursts) and continuous token bucket rate limiters with atomic execution
- **Configurable Eviction Policies**: Bound cache capacity with `ALLKEYS_LRU`, `VOLATILE_LRU`, `VOLATILE_TTL`, `ALLKEYS_FIFO`, or `NO_EVICTION`
- **Zero Dependencies**: Single JAR targeting Java 8+ with no external third-party dependencies
- **Statistics & Metrics**: Built-in tracking of hit/miss rates, key counts, eviction count, and expiration count

## Installation

### Maven Central

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.chandab0</groupId>
    <artifactId>redis-cache</artifactId>
    <version>1.0.3</version>
</dependency>
```

Or with Gradle:

```groovy
implementation 'io.github.chandab0:redis-cache:1.0.3'
```

Or with Gradle Kotlin DSL:

```kotlin
implementation("io.github.chandab0:redis-cache:1.0.3")
```

### Manual Installation

Download the JAR from [Maven Central](https://central.sonatype.com/artifact/io.github.chandab0/redis-cache) and add it to your project's classpath.

## Quick Start

```java
import com.cache.RedisCache;
import com.cache.EvictionPolicy;

public class Example {
    public static void main(String[] args) {
        // Create an unbounded cache or a bounded cache with eviction policy
        RedisCache cache = new RedisCache(10000, EvictionPolicy.ALLKEYS_LRU);

        // String operations
        cache.set("user:1001", "John Doe");
        cache.set("session:abc123", "token_value", 3600000); // 1 hour TTL

        String user = cache.get("user:1001");
        System.out.println(user); // "John Doe"

        // Universal TTL on any data structure
        cache.rpush("tasks", "task1", "task2", "task3");
        cache.expire("tasks", 300); // Expire list in 5 minutes

        // Blocking pop operations
        String task = cache.blpop("tasks", 5); // Wait up to 5s if empty

        // Set operations
        cache.sadd("tags", "java", "cache", "redis");
        boolean exists = cache.sismember("tags", "java"); // true

        // Hash operations
        cache.hset("user:1001:profile", "name", "John");
        cache.hset("user:1001:profile", "age", "30");
        String name = cache.hget("user:1001:profile", "name");

        // Sorted set operations
        cache.zadd("leaderboard", 100, "player1");
        cache.zadd("leaderboard", 250, "player2");
        cache.zadd("leaderboard", 175, "player3");
        List<String> topPlayers = cache.zrevrange("leaderboard", 0, 2);

        // Distributed Lock with automatic Watchdog heartbeat renewal
        com.cache.lock.DistributedLock lock = cache.getLock("order:1001");
        if (lock.tryLock(3, java.util.concurrent.TimeUnit.SECONDS)) {
            try {
                // Critical section protected by lock
                System.out.println("Acquired lock with watchdog renewal");
            } finally {
                lock.unlock();
            }
        }

        // Distributed Rate Limiting (Sliding Window & Token Bucket)
        com.cache.ratelimit.RateLimiter slidingLimiter = cache.getSlidingWindowRateLimiter("api:search", 100, 1, java.util.concurrent.TimeUnit.MINUTES);
        if (slidingLimiter.tryAcquire()) {
            System.out.println("Request allowed under 100 req/min limit");
        }

        com.cache.ratelimit.RateLimiter tokenBucket = cache.getTokenBucketRateLimiter("api:uploads", 10, 2.0); // 10 burst capacity, 2 tokens/sec
        if (tokenBucket.tryAcquire()) {
            System.out.println("Permit granted by token bucket");
        }

        // Get cache stats
        Map<String, Object> stats = cache.info();
        System.out.println(stats);

        cache.shutdown();
    }
}
```

## Eviction Policies & Max Capacity

You can bound the cache memory by setting a maximum key capacity and an eviction policy:

```java
// Bounded cache with 10,000 keys max and LRU eviction
RedisCache cache = new RedisCache(10000, EvictionPolicy.ALLKEYS_LRU);
```

Supported policies:
- `NO_EVICTION`: Throws `IllegalStateException` when full.
- `ALLKEYS_LRU`: Evicts the least recently used keys among all keys.
- `VOLATILE_LRU`: Evicts the least recently used keys only among keys with an active TTL.
- `VOLATILE_TTL`: Evicts keys with the shortest time-to-live remaining.
- `ALLKEYS_FIFO`: Evicts the oldest created keys (First-In, First-Out).

## API Reference

### String Operations

| Method | Description |
|--------|-------------|
| `set(key, value)` | Set a string value |
| `set(key, value, ttlMillis)` | Set with TTL in milliseconds |
| `setnx(key, value)` | Set if not exists |
| `setnx(key, value, ttlMillis)` | Set if not exists with TTL |
| `setxx(key, value)` | Set only if exists |
| `get(key)` | Get value by key |
| `get(key, defaultValue)` | Get or return default |
| `getOrCompute(key, loader)` | Get or compute if missing |
| `getOrCompute(key, loader, ttlMillis)` | Get or compute with TTL |
| `getDel(key)` | Get and delete |
| `getSet(key, newValue)` | Set new and return old |
| `mset(map)` | Set multiple keys |
| `mget(keys...)` | Get multiple keys |
| `incr(key)` | Increment by 1 |
| `incrBy(key, delta)` | Increment by delta |
| `decr(key)` | Decrement by 1 |
| `decrBy(key, delta)` | Decrement by delta |
| `append(key, value)` | Append to string |
| `getRange(key, start, end)` | Get substring |
| `strlen(key)` | Get string length |

### Key Operations (Universal across all types)

| Method | Description |
|--------|-------------|
| `del(keys...)` | Delete keys across any data type |
| `exists(key)` | Check if key exists in any data type |
| `expire(key, seconds)` | Set TTL in seconds on any key |
| `pexpire(key, millis)` | Set TTL in milliseconds on any key |
| `ttl(key)` | Get remaining TTL (seconds) |
| `pttl(key)` | Get remaining TTL (milliseconds) |
| `persist(key)` | Remove TTL |
| `keys(pattern)` | Find keys matching glob pattern |
| `rename(oldKey, newKey)` | Rename a key |

### List Operations

| Method | Description |
|--------|-------------|
| `lpush(key, values...)` | Push to head |
| `rpush(key, values...)` | Push to tail |
| `lpushx(key, value)` | Push to head only if list exists |
| `rpushx(key, value)` | Push to tail only if list exists |
| `lpop(key)` | Pop from head |
| `rpop(key)` | Pop from tail |
| `blpop(key, timeoutSeconds)` | Blocking pop from head with timeout |
| `brpop(key, timeoutSeconds)` | Blocking pop from tail with timeout |
| `llen(key)` | Get list length |
| `lrange(key, start, stop)` | Get range of elements |
| `lindex(key, index)` | Get element by index |
| `ltrim(key, start, stop)` | Trim to range |
| `lset(key, index, value)` | Set element at index |
| `lrem(key, count, value)` | Remove elements |
| `rpoplpush(src, dest)` | Pop from tail and push to head |

### Set Operations

| Method | Description |
|--------|-------------|
| `sadd(key, members...)` | Add members |
| `srem(key, members...)` | Remove members |
| `smembers(key)` | Get all members |
| `sismember(key, member)` | Check membership |
| `scard(key)` | Get member count |
| `spop(key)` | Pop random member |
| `srandmember(key)` | Get random member |
| `smove(src, dest, member)` | Move member between sets |
| `sdiff(keys...)` | Difference of sets |
| `sinter(keys...)` | Intersection of sets |
| `sunion(keys...)` | Union of sets |

### Hash Operations

| Method | Description |
|--------|-------------|
| `hset(key, field, value)` | Set field value |
| `hmset(key, map)` | Set multiple fields |
| `hget(key, field)` | Get field value |
| `hmget(key, fields...)` | Get multiple fields |
| `hgetall(key)` | Get all fields and values |
| `hdel(key, fields...)` | Delete fields |
| `hexists(key, field)` | Check field exists |
| `hkeys(key)` | Get all fields |
| `hvals(key)` | Get all values |
| `hlen(key)` | Get field count |
| `hincrBy(key, field, delta)` | Atomic increment field |
| `hincrByFloat(key, field, delta)` | Atomic increment float field |
| `hsetnx(key, field, value)` | Set if not exists |

### Sorted Set Operations

| Method | Description |
|--------|-------------|
| `zadd(key, score, member)` | Add member with score |
| `zadd(key, memberScoreMap)` | Add multiple members |
| `zrem(key, members...)` | Remove members |
| `zscore(key, member)` | Get member score |
| `zcard(key)` | Get member count |
| `zincrby(key, delta, member)` | Increment score |
| `zrank(key, member)` | Get rank (low to high) |
| `zrevrank(key, member)` | Get rank (high to low) |
| `zrange(key, start, stop)` | Get range by rank |
| `zrevrange(key, start, stop)` | Get range (descending) |
| `zrangeWithScores(key, start, stop)` | Get range with scores |
| `zcount(key, min, max)` | Count in score range |
| `zremrangebyrank(key, start, stop)` | Remove range by rank |
| `zremrangebyscore(key, min, max)` | Remove range by score |

### Rate Limiter Operations

| Method | Description |
|--------|-------------|
| `getSlidingWindowRateLimiter(name, max, dur, unit)` | Create sliding window log rate limiter |
| `getTokenBucketRateLimiter(name, cap, refillRate)` | Create token bucket rate limiter with burst capacity |
| `limiter.tryAcquire()` | Attempt immediate acquisition of 1 permit |
| `limiter.tryAcquire(permits)` | Attempt immediate acquisition of multiple permits |
| `limiter.tryAcquire(timeout, unit)` | Attempt acquisition waiting up to timeout |
| `limiter.tryAcquire(permits, timeout, unit)` | Attempt multi-permit acquisition waiting up to timeout |
| `limiter.acquire()` | Acquire 1 permit, blocking until available |
| `limiter.acquire(permits)` | Acquire multiple permits, blocking until available |
| `limiter.getAvailablePermits()` | Query current available permits / tokens |
| `limiter.reset()` | Reset rate limiter state |
| `executeAtomic(key, action)` | Execute multi-step operations atomically (Lua script equivalent) |

### Distributed Lock Operations

| Method | Description |
|--------|-------------|
| `getLock(lockName)` | Create distributed lock with default 30s watchdog renewal |
| `getLock(lockName, watchdogTimeoutMs)` | Create distributed lock with custom watchdog renewal |
| `getRedlock(lockName, otherCaches...)` | Create multi-instance Redlock across instances |
| `lock.lock()` | Acquire lock (blocking) with watchdog auto-renewal |
| `lock.lock(leaseTime, unit)` | Acquire lock with explicit lease time (watchdog disabled) |
| `lock.tryLock()` | Attempt immediate lock acquisition |
| `lock.tryLock(waitTime, unit)` | Attempt lock acquisition waiting up to `waitTime` |
| `lock.tryLock(waitTime, leaseTime, unit)` | Attempt lock with custom wait and lease timeout |
| `lock.unlock()` | Safely release lock (validating owner token) |
| `lock.isLocked()` | Check if lock is currently held |
| `lock.isHeldByCurrentThread()` | Check if lock is held by calling thread |
| `lock.getHoldCount()` | Get re-entrant hold count |
| `compareAndDelete(key, token)` | Atomic CAS delete for safe lock release |
| `compareAndExpire(key, token, ms)` | Atomic CAS TTL renewal for watchdog heartbeat |

### Utility Operations

| Method | Description |
|--------|-------------|
| `info()` | Get cache statistics and metrics |
| `resetStats()` | Reset hit/miss counters and eviction counters |
| `flushall()` | Clear all data |
| `shutdown()` | Release resources and background cleanup threads |

## Building

Requirements:
- Java 8+
- Maven 3.x

```bash
# Build JAR
mvn clean package

# Run tests
mvn test

# Output: target/redis-cache-1.0.3.jar
```

## Statistics

```java
RedisCache cache = new RedisCache(1000, EvictionPolicy.ALLKEYS_LRU);

Map<String, Object> stats = cache.info();
// {
//   "string_keys": 5,
//   "list_keys": 2,
//   "set_keys": 1,
//   "hash_keys": 3,
//   "sorted_set_keys": 1,
//   "total_keys": 12,
//   "hit_count": 150,
//   "miss_count": 25,
//   "hit_ratio": 0.857,
//   "max_capacity": 1000,
//   "eviction_policy": "ALLKEYS_LRU",
//   "evicted_keys": 0,
//   "expired_keys": 4
// }
```

## Thread Safety

All data structures guarantee full thread-safety:
- Concurrently modified lists are guarded by fine-grained per-list monitors to prevent `ConcurrentModificationException`.
- Sorted sets are backed by thread-safe `ReentrantReadWriteLock` mechanics.
- Hash counters are updated atomically with `Map.compute()`.

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 1000; i++) {
    executor.submit(() -> {
        cache.incr("counter");
        cache.rpush("queue", "item");
        cache.zadd("scores", 10.0, "user");
    });
}
```

## License

MIT License - feel free to use in any project.
