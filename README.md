# Redis-like Cache

A lightweight, high-performance Redis-like in-memory cache library for Java. Zero dependencies, thread-safe, and ready to embed in any application.

## Features

- **Multiple Data Structures**: Strings, Lists, Sets, Sorted Sets, Hashes
- **TTL Support**: Automatic expiration with configurable cleanup intervals
- **Thread-Safe**: All operations are concurrent-safe using `ConcurrentHashMap`
- **Zero Dependencies**: Single JAR with no external requirements
- **Statistics**: Built-in hit/miss tracking and cache metrics

## Installation

### Maven Central

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.chandab0</groupId>
    <artifactId>redis-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or with Gradle:

```groovy
implementation 'io.github.chandab0:redis-cache:1.0.0'
```

Or with Gradle Kotlin DSL:

```kotlin
implementation("io.github.chandab0:redis-cache:1.0.0")
```

### Manual Installation

Download the JAR from [Maven Central](https://central.sonatype.com/artifact/io.github.chandab0/redis-cache) and add it to your project's classpath.

## Quick Start

```java
import com.cache.RedisCache;

public class Example {
    public static void main(String[] args) {
        RedisCache cache = new RedisCache();

        // String operations
        cache.set("user:1001", "John Doe");
        cache.set("session:abc123", "token_value", 3600000); // 1 hour TTL

        String user = cache.get("user:1001");
        System.out.println(user); // "John Doe"

        // List operations
        cache.rpush("tasks", "task1", "task2", "task3");
        String task = cache.lpop("tasks");

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

        // Get cache stats
        Map<String, Object> stats = cache.info();
        System.out.println(stats);

        cache.shutdown();
    }
}
```

## API Reference

### String Operations

| Method | Description |
|--------|-------------|
| `set(key, value)` | Set a string value |
| `set(key, value, ttlMillis)` | Set with TTL in milliseconds |
| `setnx(key, value)` | Set if not exists |
| `get(key)` | Get value by key |
| `get(key, defaultValue)` | Get or return default |
| `getOrCompute(key, loader)` | Get or compute if missing |
| `getDel(key)` | Get and delete |
| `getSet(key, newValue)` | Set new and return old |
| `mset(map)` | Set multiple keys |
| `mget(keys...)` | Get multiple keys |
| `incr(key)` | Increment by 1 |
| `incrBy(key, delta)` | Increment by delta |
| `decr(key)` | Decrement by 1 |
| `append(key, value)` | Append to string |
| `strlen(key)` | Get string length |

### Key Operations

| Method | Description |
|--------|-------------|
| `del(keys...)` | Delete keys |
| `exists(key)` | Check if key exists |
| `expire(key, seconds)` | Set TTL in seconds |
| `pexpire(key, millis)` | Set TTL in milliseconds |
| `ttl(key)` | Get remaining TTL (seconds) |
| `pttl(key)` | Get remaining TTL (milliseconds) |
| `persist(key)` | Remove TTL |
| `keys(pattern)` | Find keys matching pattern |
| `rename(oldKey, newKey)` | Rename a key |

### List Operations

| Method | Description |
|--------|-------------|
| `lpush(key, values...)` | Push to head |
| `rpush(key, values...)` | Push to tail |
| `lpop(key)` | Pop from head |
| `rpop(key)` | Pop from tail |
| `llen(key)` | Get list length |
| `lrange(key, start, stop)` | Get range of elements |
| `lindex(key, index)` | Get element by index |
| `ltrim(key, start, stop)` | Trim to range |
| `lset(key, index, value)` | Set element at index |
| `lrem(key, count, value)` | Remove elements |
| `rpoplpush(src, dest)` | Pop and push |

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
| `hincrBy(key, field, delta)` | Increment field |
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

### Utility Operations

| Method | Description |
|--------|-------------|
| `info()` | Get cache statistics |
| `resetStats()` | Reset hit/miss counters |
| `flushall()` | Clear all data |
| `shutdown()` | Release resources |

## Building

Requirements:
- Java 8+
- Maven 3.x

```bash
# Build JAR
mvn clean package

# Run tests
mvn test

# Output: target/redis-cache-1.0.0.jar
```

## Pattern Matching

The `keys(pattern)` method supports Redis-style glob patterns:

- `*` - matches any sequence of characters
- `?` - matches a single character

```java
cache.set("user:1", "John");
cache.set("user:2", "Jane");
cache.set("session:1", "abc");

Set<String> userKeys = cache.keys("user:*"); // ["user:1", "user:2"]
Set<String> allKeys = cache.keys("*");       // all keys
```

## Statistics

```java
RedisCache cache = new RedisCache();

// ... perform operations ...

Map<String, Object> stats = cache.info();
// {
//   "string_keys": 5,
//   "list_keys": 2,
//   "set_keys": 1,
//   "hash_keys": 3,
//   "sorted_set_keys": 1,
//   "hit_count": 150,
//   "miss_count": 25,
//   "hit_ratio": 0.857
// }
```

## Thread Safety

All data structures use `ConcurrentHashMap` and thread-safe collections. Multiple threads can safely read and write simultaneously.

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

// Safe concurrent access
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> {
        cache.incr("counter");
        cache.set("key", "value");
    });
}
```

## License

MIT License - feel free to use in any project.
