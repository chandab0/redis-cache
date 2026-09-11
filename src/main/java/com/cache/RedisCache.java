package com.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import com.cache.lock.DistributedLock;
import com.cache.lock.RedisLock;
import com.cache.lock.Redlock;
import com.cache.ratelimit.RateLimiter;
import com.cache.ratelimit.SlidingWindowRateLimiter;
import com.cache.ratelimit.TokenBucketRateLimiter;

/**
 * Redis-like in-memory cache with support for multiple data structures,
 * universal TTL across all types, true thread-safety, real blocking queues,
 * and configurable eviction policies.
 *
 * <p>Supported data types:
 * <ul>
 *   <li>Strings - simple key-value pairs with optional TTL</li>
 *   <li>Lists - ordered collections with push/pop and blocking operations</li>
 *   <li>Sets - unordered unique collections with set algebra</li>
 *   <li>Sorted Sets - ordered collections scored by double values</li>
 *   <li>Hashes - field-value maps</li>
 * </ul>
 *
 * @author Biswajit Chanda
 * @version 1.0.3
 */
public class RedisCache {
    private final CacheStore stringStore;
    private final ConcurrentHashMap<String, LinkedList<Object>> lists;
    private final ConcurrentHashMap<String, Set<Object>> sets;
    private final ConcurrentHashMap<String, Map<String, Object>> hashes;
    private final ConcurrentHashMap<String, SortedSetContainer> sortedSets;

    private final ConcurrentHashMap<String, Long> keyExpirations;
    private final ConcurrentHashMap<String, Long> lastAccessTimes;
    private final ConcurrentHashMap<String, Long> creationTimes;
    private final Set<String> allKeys;
    private final ConcurrentHashMap<String, Object> listLocks;

    private final ScheduledExecutorService cleanupExecutor;
    private final int maxCapacity;
    private final EvictionPolicy evictionPolicy;
    private final AtomicLong evictedCount;
    private final AtomicLong expiredCount;
    private volatile boolean running = true;

    /**
     * Creates an unbounded cache with default cleanup interval of 1 second and NO_EVICTION.
     */
    public RedisCache() {
        this(1000, 0, EvictionPolicy.NO_EVICTION);
    }

    /**
     * Creates an unbounded cache with specified cleanup interval.
     *
     * @param cleanupIntervalMillis interval in ms for cleaning expired keys
     */
    public RedisCache(long cleanupIntervalMillis) {
        this(cleanupIntervalMillis, 0, EvictionPolicy.NO_EVICTION);
    }

    /**
     * Creates a bounded cache with specified capacity and eviction policy.
     *
     * @param maxCapacity maximum number of keys allowed in the cache (0 for unbounded)
     * @param evictionPolicy policy to use when cache reaches max capacity
     */
    public RedisCache(int maxCapacity, EvictionPolicy evictionPolicy) {
        this(1000, maxCapacity, evictionPolicy);
    }

    /**
     * Creates a cache with specified cleanup interval, capacity, and eviction policy.
     *
     * @param cleanupIntervalMillis interval in ms for cleaning expired keys
     * @param maxCapacity maximum number of keys allowed in the cache (0 for unbounded)
     * @param evictionPolicy policy to use when cache reaches max capacity
     */
    public RedisCache(long cleanupIntervalMillis, int maxCapacity, EvictionPolicy evictionPolicy) {
        this.maxCapacity = Math.max(0, maxCapacity);
        this.evictionPolicy = evictionPolicy != null ? evictionPolicy : EvictionPolicy.NO_EVICTION;
        this.stringStore = new CacheStore(cleanupIntervalMillis);
        this.lists = new ConcurrentHashMap<>();
        this.sets = new ConcurrentHashMap<>();
        this.hashes = new ConcurrentHashMap<>();
        this.sortedSets = new ConcurrentHashMap<>();

        this.keyExpirations = new ConcurrentHashMap<>();
        this.lastAccessTimes = new ConcurrentHashMap<>();
        this.creationTimes = new ConcurrentHashMap<>();
        this.allKeys = ConcurrentHashMap.newKeySet();
        this.listLocks = new ConcurrentHashMap<>();

        this.evictedCount = new AtomicLong(0);
        this.expiredCount = new AtomicLong(0);

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpired,
            cleanupIntervalMillis,
            cleanupIntervalMillis,
            TimeUnit.MILLISECONDS
        );
    }

    // ==================== INTERNAL HELPERS ====================

    private Object getListLock(String key) {
        return listLocks.computeIfAbsent(key, k -> new Object());
    }

    private void recordAccess(String key) {
        lastAccessTimes.put(key, System.nanoTime());
    }

    private void recordCreation(String key) {
        allKeys.add(key);
        creationTimes.putIfAbsent(key, System.currentTimeMillis());
        recordAccess(key);
    }

    private boolean checkExpired(String key) {
        Long expireAt = keyExpirations.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            delInternal(key);
            expiredCount.incrementAndGet();
            return true;
        }
        return false;
    }

    private void delInternal(String key) {
        stringStore.delete(key);
        lists.remove(key);
        sets.remove(key);
        hashes.remove(key);
        sortedSets.remove(key);
        keyExpirations.remove(key);
        lastAccessTimes.remove(key);
        creationTimes.remove(key);
        allKeys.remove(key);
        Object lock = listLocks.get(key);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private synchronized void ensureCapacity(String key) {
        if (maxCapacity <= 0 || allKeys.contains(key)) {
            return;
        }

        if (evictionPolicy == EvictionPolicy.NO_EVICTION) {
            if (allKeys.size() >= maxCapacity) {
                throw new IllegalStateException("OOM: cache is full (maxCapacity=" + maxCapacity + ") with NO_EVICTION policy");
            }
            return;
        }

        while (allKeys.size() >= maxCapacity && !allKeys.isEmpty()) {
            String victim = selectEvictionCandidate();
            if (victim == null) {
                break;
            }
            delInternal(victim);
            evictedCount.incrementAndGet();
        }

        if (allKeys.size() >= maxCapacity) {
            throw new IllegalStateException("OOM: unable to evict any keys under policy " + evictionPolicy);
        }
    }

    private String selectEvictionCandidate() {
        switch (evictionPolicy) {
            case ALLKEYS_LRU:
                return findMinKey(allKeys, lastAccessTimes);
            case VOLATILE_LRU:
                return findMinKey(keyExpirations.keySet(), lastAccessTimes);
            case VOLATILE_TTL:
                return findMinKey(keyExpirations.keySet(), keyExpirations);
            case ALLKEYS_FIFO:
                return findMinKey(allKeys, creationTimes);
            default:
                return null;
        }
    }

    private String findMinKey(Collection<String> pool, Map<String, ? extends Number> metricMap) {
        if (pool.isEmpty()) return null;
        String bestKey = null;
        double minVal = Double.MAX_VALUE;

        Iterable<String> candidates;
        if (pool.size() <= 200) {
            candidates = pool;
        } else {
            List<String> sampleList = new ArrayList<>(pool);
            Collections.shuffle(sampleList);
            candidates = sampleList.subList(0, Math.min(10, sampleList.size()));
        }

        for (String k : candidates) {
            Number val = metricMap.get(k);
            if (val != null && val.doubleValue() < minVal) {
                minVal = val.doubleValue();
                bestKey = k;
            }
        }
        return bestKey != null ? bestKey : (pool.isEmpty() ? null : pool.iterator().next());
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : keyExpirations.entrySet()) {
            if (entry.getValue() > 0 && now > entry.getValue()) {
                delInternal(entry.getKey());
                expiredCount.incrementAndGet();
            }
        }
    }

    // ==================== STRING OPERATIONS ====================

    /**
     * Sets a string value.
     */
    public void set(String key, Object value) {
        checkExpired(key);
        ensureCapacity(key);
        stringStore.put(key, value);
        keyExpirations.remove(key);
        recordCreation(key);
    }

    /**
     * Sets a string value with TTL in milliseconds.
     */
    public void set(String key, Object value, long ttlMillis) {
        checkExpired(key);
        ensureCapacity(key);
        stringStore.put(key, value, ttlMillis);
        if (ttlMillis > 0) {
            keyExpirations.put(key, System.currentTimeMillis() + ttlMillis);
        } else {
            keyExpirations.remove(key);
        }
        recordCreation(key);
    }

    /**
     * Sets a value only if key doesn't exist (SETNX).
     * @return true if set, false if key exists
     */
    public boolean setnx(String key, Object value) {
        checkExpired(key);
        if (exists(key)) return false;
        ensureCapacity(key);
        boolean result = stringStore.setIfAbsent(key, value);
        if (result) {
            keyExpirations.remove(key);
            recordCreation(key);
        }
        return result;
    }

    /**
     * Sets a value only if key doesn't exist, with TTL.
     */
    public boolean setnx(String key, Object value, long ttlMillis) {
        checkExpired(key);
        if (exists(key)) return false;
        ensureCapacity(key);
        boolean result = stringStore.setIfAbsent(key, value, ttlMillis);
        if (result) {
            if (ttlMillis > 0) {
                keyExpirations.put(key, System.currentTimeMillis() + ttlMillis);
            }
            recordCreation(key);
        }
        return result;
    }

    /**
     * Sets a value only if key exists (SETXX).
     * @return true if set, false if key doesn't exist
     */
    public boolean setxx(String key, Object value) {
        checkExpired(key);
        if (!exists(key)) return false;
        boolean result = stringStore.setIfExists(key, value);
        if (result) {
            recordAccess(key);
        }
        return result;
    }

    /**
     * Gets a string value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (checkExpired(key)) return null;
        T value = stringStore.get(key);
        if (value != null) {
            recordAccess(key);
        }
        return value;
    }

    /**
     * Gets a value, returns default if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a value or computes it if missing.
     */
    public <T> T getOrCompute(String key, Function<String, T> loader) {
        return getOrCompute(key, loader, 0);
    }

    /**
     * Gets a value or computes it if missing, with TTL.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Function<String, T> loader, long ttlMillis) {
        T value = get(key);
        if (value != null) {
            return value;
        }
        ensureCapacity(key);
        value = loader.apply(key);
        if (value != null) {
            if (ttlMillis > 0) {
                set(key, value, ttlMillis);
            } else {
                set(key, value);
            }
        }
        return value;
    }

    /**
     * Gets the value and deletes the key (GETDEL).
     */
    public <T> T getDel(String key) {
        T value = get(key);
        if (value != null) {
            del(key);
        }
        return value;
    }

    /**
     * Sets a new value and returns the old value (GETSET).
     */
    @SuppressWarnings("unchecked")
    public <T> T getSet(String key, Object newValue) {
        T oldValue = get(key);
        set(key, newValue);
        return oldValue;
    }

    /**
     * Sets multiple keys at once (MSET).
     */
    public void mset(Map<String, Object> keyValueMap) {
        keyValueMap.forEach(this::set);
    }

    /**
     * Gets multiple keys at once (MGET).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> mget(String... keys) {
        List<T> result = new ArrayList<>();
        for (String key : keys) {
            result.add(get(key));
        }
        return result;
    }

    /**
     * Increments a numeric value by 1.
     */
    public long incr(String key) {
        return incrBy(key, 1);
    }

    /**
     * Increments a numeric value by delta.
     */
    public long incrBy(String key, long delta) {
        checkExpired(key);
        ensureCapacity(key);
        long result = stringStore.increment(key, delta);
        recordCreation(key);
        return result;
    }

    /**
     * Decrements a numeric value by 1.
     */
    public long decr(String key) {
        return incrBy(key, -1);
    }

    /**
     * Decrements a numeric value by delta.
     */
    public long decrBy(String key, long delta) {
        return incrBy(key, -delta);
    }

    /**
     * Appends to a string value.
     * @return new length of the string
     */
    public long append(String key, String value) {
        checkExpired(key);
        ensureCapacity(key);
        String existing = get(key);
        String newValue = existing != null ? existing + value : value;
        set(key, newValue);
        return newValue.length();
    }

    /**
     * Gets substring of a string value.
     */
    public String getRange(String key, int start, int end) {
        String value = get(key);
        if (value == null) {
            return "";
        }
        if (start < 0) start = Math.max(0, value.length() + start);
        if (end < 0) end = value.length() + end;
        if (start > end || start >= value.length()) {
            return "";
        }
        end = Math.min(end, value.length() - 1);
        return value.substring(start, end + 1);
    }

    /**
     * Returns length of string value.
     */
    public long strlen(String key) {
        String value = get(key);
        return value != null ? value.length() : 0;
    }

    // ==================== KEY OPERATIONS (UNIVERSAL) ====================

    /**
     * Deletes one or more keys.
     * @return number of keys deleted
     */
    public long del(String... keys) {
        long count = 0;
        for (String key : keys) {
            if (exists(key)) {
                delInternal(key);
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if key exists.
     */
    public boolean exists(String key) {
        if (checkExpired(key)) {
            return false;
        }
        return stringStore.exists(key) ||
               lists.containsKey(key) ||
               sets.containsKey(key) ||
               hashes.containsKey(key) ||
               sortedSets.containsKey(key);
    }

    /**
     * Sets TTL on any key in milliseconds.
     */
    public boolean pexpire(String key, long milliseconds) {
        if (checkExpired(key)) return false;
        if (!exists(key)) return false;
        long expireAt = System.currentTimeMillis() + milliseconds;
        keyExpirations.put(key, expireAt);
        if (stringStore.exists(key)) {
            stringStore.expire(key, milliseconds);
        }
        return true;
    }

    /**
     * Sets TTL on any key in seconds.
     */
    public boolean expire(String key, long seconds) {
        return pexpire(key, seconds * 1000);
    }

    /**
     * Gets remaining TTL in milliseconds.
     */
    public long pttl(String key) {
        if (checkExpired(key)) return -2;
        if (!exists(key)) return -2;
        Long expireAt = keyExpirations.get(key);
        if (expireAt == null) return -1;
        long remaining = expireAt - System.currentTimeMillis();
        return remaining <= 0 ? -2 : remaining;
    }

    /**
     * Gets remaining TTL in seconds.
     */
    public long ttl(String key) {
        long ms = pttl(key);
        if (ms <= 0) return ms;
        return (ms + 999) / 1000;
    }

    /**
     * Removes TTL from any key.
     */
    public boolean persist(String key) {
        if (checkExpired(key)) return false;
        if (!exists(key)) return false;
        boolean removed = keyExpirations.remove(key) != null;
        stringStore.persist(key);
        return removed;
    }

    /**
     * Atomically deletes a key only if its current value equals expectedValue.
     * Useful for safe distributed lock release (fencing token verification).
     *
     * @param key cache key
     * @param expectedValue expected current value (e.g. lock owner token)
     * @return true if deleted, false if key did not exist or value did not match
     */
    public synchronized boolean compareAndDelete(String key, Object expectedValue) {
        if (checkExpired(key)) {
            return false;
        }
        Object current = stringStore.get(key);
        if (current != null && Objects.equals(current, expectedValue)) {
            delInternal(key);
            return true;
        }
        return false;
    }

    /**
     * Atomically updates the expiration time on a key only if its current value equals expectedValue.
     * Useful for distributed lock watchdog / heartbeat renewal.
     *
     * @param key cache key
     * @param expectedValue expected current value (e.g. lock owner token)
     * @param ttlMillis new TTL in milliseconds
     * @return true if TTL was renewed, false if key did not exist or value did not match
     */
    public synchronized boolean compareAndExpire(String key, Object expectedValue, long ttlMillis) {
        if (checkExpired(key)) {
            return false;
        }
        Object current = stringStore.get(key);
        if (current != null && Objects.equals(current, expectedValue)) {
            long expireAt = System.currentTimeMillis() + ttlMillis;
            keyExpirations.put(key, expireAt);
            stringStore.expire(key, ttlMillis);
            return true;
        }
        return false;
    }

    /**
     * Creates or retrieves a distributed lock for the given lock name with default 30-second watchdog.
     *
     * @param lockName name of the lock
     * @return DistributedLock instance
     */
    public DistributedLock getLock(String lockName) {
        return new RedisLock(this, lockName);
    }

    /**
     * Creates or retrieves a distributed lock with custom watchdog timeout.
     *
     * @param lockName name of the lock
     * @param watchdogTimeoutMillis watchdog timeout in milliseconds
     * @return DistributedLock instance
     */
    public DistributedLock getLock(String lockName, long watchdogTimeoutMillis) {
        return new RedisLock(this, lockName, watchdogTimeoutMillis);
    }

    /**
     * Creates a multi-instance Redlock distributed lock across this cache and other caches.
     *
     * @param lockName name of the lock
     * @param otherCaches other RedisCache instances to participate in the quorum
     * @return Redlock instance
     */
    public DistributedLock getRedlock(String lockName, RedisCache... otherCaches) {
        List<RedisCache> all = new ArrayList<>();
        all.add(this);
        if (otherCaches != null) {
            Collections.addAll(all, otherCaches);
        }
        return new Redlock(lockName, all);
    }

    /**
     * Executes a function atomically with exclusive synchronization on the specified key.
     * Provides embedded atomic transaction capabilities analogous to Redis Lua scripts (EVAL).
     *
     * @param key cache key to synchronize on
     * @param action function receiving this cache instance and returning a result
     * @param <T> result type
     * @return result returned by the action
     */
    public <T> T executeAtomic(String key, Function<RedisCache, T> action) {
        Object lock = getListLock("atomic:" + key);
        synchronized (lock) {
            return action.apply(this);
        }
    }

    /**
     * Creates or retrieves a sliding window log rate limiter for the specified resource.
     *
     * @param name unique resource name
     * @param maxPermits maximum permits allowed within the window
     * @param windowDuration length of the window
     * @param unit time unit of window duration
     * @return RateLimiter instance
     */
    public RateLimiter getSlidingWindowRateLimiter(String name, long maxPermits, long windowDuration, TimeUnit unit) {
        return new SlidingWindowRateLimiter(this, name, maxPermits, windowDuration, unit);
    }

    /**
     * Creates or retrieves a token bucket rate limiter for the specified resource.
     *
     * @param name unique resource name
     * @param capacity maximum token capacity (burst limit)
     * @param refillRatePerSecond number of tokens added per second
     * @return RateLimiter instance
     */
    public RateLimiter getTokenBucketRateLimiter(String name, long capacity, double refillRatePerSecond) {
        return new TokenBucketRateLimiter(this, name, capacity, refillRatePerSecond);
    }



    /**
     * Finds all keys matching pattern.
     */
    public Set<String> keys(String pattern) {
        Set<String> activeKeys = new HashSet<>();
        for (String key : allKeys) {
            if (!checkExpired(key)) {
                activeKeys.add(key);
            }
        }

        String regex = patternToRegex(pattern);
        Set<String> matched = new HashSet<>();
        for (String key : activeKeys) {
            if (key.matches(regex)) {
                matched.add(key);
            }
        }
        return matched;
    }

    /**
     * Returns all keys.
     */
    public Set<String> keys() {
        return keys("*");
    }

    /**
     * Renames a key.
     */
    public void rename(String oldKey, String newKey) {
        if (checkExpired(oldKey)) return;
        checkExpired(newKey);

        Long oldTtl = keyExpirations.get(oldKey);

        // String store
        Object strValue = stringStore.get(oldKey);
        if (strValue != null) {
            stringStore.put(newKey, strValue);
            stringStore.delete(oldKey);
        }

        // Lists
        Object oldLock = getListLock(oldKey);
        Object newLock = getListLock(newKey);
        synchronized (oldLock) {
            synchronized (newLock) {
                LinkedList<Object> list = lists.remove(oldKey);
                if (list != null) {
                    lists.put(newKey, list);
                }
            }
        }

        // Sets
        Set<Object> set = sets.remove(oldKey);
        if (set != null) {
            sets.put(newKey, set);
        }

        // Hashes
        Map<String, Object> hash = hashes.remove(oldKey);
        if (hash != null) {
            hashes.put(newKey, hash);
        }

        // Sorted Sets
        SortedSetContainer zset = sortedSets.remove(oldKey);
        if (zset != null) {
            sortedSets.put(newKey, zset);
        }

        keyExpirations.remove(oldKey);
        lastAccessTimes.remove(oldKey);
        creationTimes.remove(oldKey);
        allKeys.remove(oldKey);

        if (oldTtl != null) {
            keyExpirations.put(newKey, oldTtl);
        }
        recordCreation(newKey);
    }

    private String patternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*': regex.append(".*"); break;
                case '?': regex.append("."); break;
                default:
                    if (Character.isLetterOrDigit(c)) {
                        regex.append(c);
                    } else {
                        regex.append("\\").append(c);
                    }
            }
        }
        return regex.toString();
    }

    // ==================== LIST OPERATIONS (THREAD-SAFE & BLOCKING) ====================

    /**
     * Pushes elements to the left of a list (LPUSH).
     * @return length of list after push
     */
    public long lpush(String key, Object... values) {
        checkExpired(key);
        ensureCapacity(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.computeIfAbsent(key, k -> new LinkedList<>());
            for (Object value : values) {
                list.addFirst(value);
            }
            recordCreation(key);
            lock.notifyAll();
            return list.size();
        }
    }

    /**
     * Pushes elements to the right of a list (RPUSH).
     * @return length of list after push
     */
    public long rpush(String key, Object... values) {
        checkExpired(key);
        ensureCapacity(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.computeIfAbsent(key, k -> new LinkedList<>());
            for (Object value : values) {
                list.addLast(value);
            }
            recordCreation(key);
            lock.notifyAll();
            return list.size();
        }
    }

    /**
     * Pushes to left only if list exists (LPUSHX).
     */
    public long lpushx(String key, Object value) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return 0;
            list.addFirst(value);
            recordAccess(key);
            lock.notifyAll();
            return list.size();
        }
    }

    /**
     * Pushes to right only if list exists (RPUSHX).
     */
    public long rpushx(String key, Object value) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return 0;
            list.addLast(value);
            recordAccess(key);
            lock.notifyAll();
            return list.size();
        }
    }

    /**
     * Pops from the left of a list (LPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T lpop(String key) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null || list.isEmpty()) return null;
            recordAccess(key);
            T value = (T) list.removeFirst();
            if (list.isEmpty()) {
                delInternal(key);
            }
            return value;
        }
    }

    /**
     * Pops from the right of a list (RPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T rpop(String key) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null || list.isEmpty()) return null;
            recordAccess(key);
            T value = (T) list.removeLast();
            if (list.isEmpty()) {
                delInternal(key);
            }
            return value;
        }
    }

    /**
     * Gets element by index (LINDEX).
     */
    @SuppressWarnings("unchecked")
    public <T> T lindex(String key, long index) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return null;
            recordAccess(key);
            if (index < 0) index = list.size() + index;
            if (index < 0 || index >= list.size()) return null;
            return (T) list.get((int) index);
        }
    }

    /**
     * Returns length of list (LLEN).
     */
    public long llen(String key) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return 0;
            recordAccess(key);
            return list.size();
        }
    }

    /**
     * Gets a range of elements (LRANGE).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> lrange(String key, long start, long stop) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return Collections.emptyList();
            recordAccess(key);

            int size = list.size();
            if (start < 0) start = Math.max(0, size + start);
            if (stop < 0) stop = size + stop;
            if (start > stop || start >= size) return Collections.emptyList();

            stop = Math.min(stop, size - 1);
            List<T> result = new ArrayList<>();
            for (int i = (int) start; i <= stop; i++) {
                result.add((T) list.get(i));
            }
            return result;
        }
    }

    /**
     * Trims list to specified range (LTRIM).
     */
    public void ltrim(String key, long start, long stop) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return;
            recordAccess(key);

            int size = list.size();
            if (start < 0) start = Math.max(0, size + start);
            if (stop < 0) stop = size + stop;
            if (start > stop) {
                delInternal(key);
                return;
            }

            stop = Math.min(stop, size - 1);
            LinkedList<Object> newList = new LinkedList<>();
            for (int i = (int) start; i <= stop; i++) {
                newList.add(list.get(i));
            }
            lists.put(key, newList);
        }
    }

    /**
     * Sets element at index (LSET).
     */
    public boolean lset(String key, long index, Object value) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return false;
            if (index < 0) index = list.size() + index;
            if (index < 0 || index >= list.size()) return false;
            list.set((int) index, value);
            recordAccess(key);
            return true;
        }
    }

    /**
     * Removes elements matching value (LREM).
     */
    public long lrem(String key, long count, Object value) {
        checkExpired(key);
        Object lock = getListLock(key);
        synchronized (lock) {
            LinkedList<Object> list = lists.get(key);
            if (list == null) return 0;
            recordAccess(key);

            long removed = 0;
            if (count == 0) {
                removed = list.stream().filter(v -> Objects.equals(v, value)).count();
                list.removeIf(v -> Objects.equals(v, value));
            } else if (count > 0) {
                Iterator<Object> iter = list.iterator();
                while (iter.hasNext() && removed < count) {
                    if (Objects.equals(iter.next(), value)) {
                        iter.remove();
                        removed++;
                    }
                }
            } else {
                Iterator<Object> iter = list.descendingIterator();
                while (iter.hasNext() && removed < -count) {
                    if (Objects.equals(iter.next(), value)) {
                        iter.remove();
                        removed++;
                    }
                }
            }
            if (list.isEmpty()) delInternal(key);
            return removed;
        }
    }

    /**
     * Pops from right of source and pushes to left of destination (RPOPLPUSH).
     */
    public <T> T rpoplpush(String source, String destination) {
        checkExpired(source);
        checkExpired(destination);
        String first = source.compareTo(destination) <= 0 ? source : destination;
        String second = source.compareTo(destination) <= 0 ? destination : source;
        synchronized (getListLock(first)) {
            synchronized (getListLock(second)) {
                T value = rpop(source);
                if (value != null) {
                    lpush(destination, value);
                }
                return value;
            }
        }
    }

    /**
     * Blocking pop from left of list (BLPOP).
     * Blocks up to timeoutSeconds until an element is available.
     */
    @SuppressWarnings("unchecked")
    public <T> T blpop(String key, long timeoutSeconds) {
        Object lock = getListLock(key);
        long deadline = timeoutSeconds > 0 ? System.currentTimeMillis() + (timeoutSeconds * 1000) : Long.MAX_VALUE;
        synchronized (lock) {
            while (true) {
                checkExpired(key);
                T val = lpop(key);
                if (val != null) {
                    return val;
                }
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return null;
                }
                try {
                    lock.wait(Math.min(remaining, 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    /**
     * Blocking pop from right of list (BRPOP).
     * Blocks up to timeoutSeconds until an element is available.
     */
    @SuppressWarnings("unchecked")
    public <T> T brpop(String key, long timeoutSeconds) {
        Object lock = getListLock(key);
        long deadline = timeoutSeconds > 0 ? System.currentTimeMillis() + (timeoutSeconds * 1000) : Long.MAX_VALUE;
        synchronized (lock) {
            while (true) {
                checkExpired(key);
                T val = rpop(key);
                if (val != null) {
                    return val;
                }
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return null;
                }
                try {
                    lock.wait(Math.min(remaining, 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }

    // ==================== SET OPERATIONS ====================

    /**
     * Adds members to a set (SADD).
     */
    public long sadd(String key, Object... members) {
        checkExpired(key);
        ensureCapacity(key);
        Set<Object> set = sets.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        long added = 0;
        for (Object member : members) {
            if (set.add(member)) added++;
        }
        recordCreation(key);
        return added;
    }

    /**
     * Removes members from a set (SREM).
     */
    public long srem(String key, Object... members) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null) return 0;
        recordAccess(key);
        long removed = 0;
        for (Object member : members) {
            if (set.remove(member)) removed++;
        }
        if (set.isEmpty()) delInternal(key);
        return removed;
    }

    /**
     * Gets all members of a set (SMEMBERS).
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> smembers(String key) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null) return Collections.emptySet();
        recordAccess(key);
        return new HashSet<>((Set<T>) set);
    }

    /**
     * Checks if member exists in set (SISMEMBER).
     */
    public boolean sismember(String key, Object member) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null) return false;
        recordAccess(key);
        return set.contains(member);
    }

    /**
     * Returns number of members in set (SCARD).
     */
    public long scard(String key) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null) return 0;
        recordAccess(key);
        return set.size();
    }

    /**
     * Pops a random member from set (SPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T spop(String key) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null || set.isEmpty()) return null;
        recordAccess(key);
        Iterator<Object> iter = set.iterator();
        T value = (T) iter.next();
        iter.remove();
        if (set.isEmpty()) delInternal(key);
        return value;
    }

    /**
     * Returns a random member from set (SRANDMEMBER).
     */
    @SuppressWarnings("unchecked")
    public <T> T srandmember(String key) {
        checkExpired(key);
        Set<Object> set = sets.get(key);
        if (set == null || set.isEmpty()) return null;
        recordAccess(key);
        return (T) set.iterator().next();
    }

    /**
     * Moves member from one set to another (SMOVE).
     */
    public boolean smove(String source, String destination, Object member) {
        checkExpired(source);
        checkExpired(destination);
        Set<Object> srcSet = sets.get(source);
        if (srcSet == null || !srcSet.contains(member)) return false;
        srcSet.remove(member);
        if (srcSet.isEmpty()) delInternal(source);
        sadd(destination, member);
        return true;
    }

    /**
     * Returns difference between sets (SDIFF).
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> sdiff(String... keys) {
        if (keys.length == 0) return Collections.emptySet();
        Set<Object> result = new HashSet<>(smembers(keys[0]));
        for (int i = 1; i < keys.length; i++) {
            result.removeAll(smembers(keys[i]));
        }
        return (Set<T>) result;
    }

    /**
     * Returns intersection of sets (SINTER).
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> sinter(String... keys) {
        if (keys.length == 0) return Collections.emptySet();
        Set<Object> result = new HashSet<>(smembers(keys[0]));
        for (int i = 1; i < keys.length; i++) {
            result.retainAll(smembers(keys[i]));
        }
        return (Set<T>) result;
    }

    /**
     * Returns union of sets (SUNION).
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> sunion(String... keys) {
        Set<Object> result = new HashSet<>();
        for (String key : keys) {
            result.addAll(smembers(key));
        }
        return (Set<T>) result;
    }

    // ==================== HASH OPERATIONS ====================

    /**
     * Sets field in hash (HSET).
     * @return 1 if new field, 0 if updated
     */
    public long hset(String key, String field, Object value) {
        checkExpired(key);
        ensureCapacity(key);
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        long result = hash.put(field, value) == null ? 1 : 0;
        recordCreation(key);
        return result;
    }

    /**
     * Sets multiple fields in hash (HMSET).
     */
    public void hmset(String key, Map<String, Object> fieldValueMap) {
        checkExpired(key);
        ensureCapacity(key);
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        hash.putAll(fieldValueMap);
        recordCreation(key);
    }

    /**
     * Gets field from hash (HGET).
     */
    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String field) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return null;
        recordAccess(key);
        return (T) hash.get(field);
    }

    /**
     * Gets multiple fields from hash (HMGET).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hmget(String key, String... fields) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        List<T> result = new ArrayList<>();
        if (hash == null) {
            for (int i = 0; i < fields.length; i++) result.add(null);
            return result;
        }
        recordAccess(key);
        for (String field : fields) {
            result.add((T) hash.get(field));
        }
        return result;
    }

    /**
     * Gets all fields and values (HGETALL).
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hgetall(String key) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return Collections.emptyMap();
        recordAccess(key);
        return new HashMap<>((Map<String, T>) hash);
    }

    /**
     * Deletes fields from hash (HDEL).
     * @return number of fields removed
     */
    public long hdel(String key, String... fields) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return 0;
        recordAccess(key);
        long removed = 0;
        for (String field : fields) {
            if (hash.remove(field) != null) removed++;
        }
        if (hash.isEmpty()) delInternal(key);
        return removed;
    }

    /**
     * Checks if field exists in hash (HEXISTS).
     */
    public boolean hexists(String key, String field) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return false;
        recordAccess(key);
        return hash.containsKey(field);
    }

    /**
     * Returns all fields in hash (HKEYS).
     */
    public Set<String> hkeys(String key) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return Collections.emptySet();
        recordAccess(key);
        return new HashSet<>(hash.keySet());
    }

    /**
     * Returns all values in hash (HVALS).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hvals(String key) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return Collections.emptyList();
        recordAccess(key);
        return new ArrayList<>((Collection<T>) hash.values());
    }

    /**
     * Returns number of fields in hash (HLEN).
     */
    public long hlen(String key) {
        checkExpired(key);
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return 0;
        recordAccess(key);
        return hash.size();
    }

    /**
     * Increments hash field by value (HINCRBY) atomically.
     */
    public long hincrBy(String key, String field, long increment) {
        checkExpired(key);
        ensureCapacity(key);
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        Object result = hash.compute(field, (f, oldVal) -> {
            long current = 0;
            if (oldVal instanceof Number) {
                current = ((Number) oldVal).longValue();
            } else if (oldVal instanceof String) {
                current = Long.parseLong((String) oldVal);
            }
            return current + increment;
        });
        recordCreation(key);
        return ((Number) result).longValue();
    }

    /**
     * Increments hash field by float (HINCRBYFLOAT) atomically.
     */
    public double hincrByFloat(String key, String field, double increment) {
        checkExpired(key);
        ensureCapacity(key);
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        Object result = hash.compute(field, (f, oldVal) -> {
            double current = 0.0;
            if (oldVal instanceof Number) {
                current = ((Number) oldVal).doubleValue();
            } else if (oldVal instanceof String) {
                current = Double.parseDouble((String) oldVal);
            }
            return current + increment;
        });
        recordCreation(key);
        return ((Number) result).doubleValue();
    }

    /**
     * Sets field only if it doesn't exist (HSETNX).
     */
    public boolean hsetnx(String key, String field, Object value) {
        checkExpired(key);
        ensureCapacity(key);
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        boolean set = hash.putIfAbsent(field, value) == null;
        if (set) {
            recordCreation(key);
        }
        return set;
    }

    // ==================== SORTED SET OPERATIONS (THREAD-SAFE) ====================

    /**
     * Adds member with score to sorted set (ZADD).
     * @return 1 if new member, 0 if score updated
     */
    public long zadd(String key, double score, String member) {
        checkExpired(key);
        ensureCapacity(key);
        SortedSetContainer zset = sortedSets.computeIfAbsent(key, k -> new SortedSetContainer());
        long result = zset.add(score, member);
        recordCreation(key);
        return result;
    }

    /**
     * Adds multiple members with scores.
     * @return number of new members added
     */
    public long zadd(String key, Map<String, Double> memberScores) {
        long added = 0;
        for (Map.Entry<String, Double> entry : memberScores.entrySet()) {
            added += zadd(key, entry.getValue(), entry.getKey());
        }
        return added;
    }

    /**
     * Removes member from sorted set (ZREM).
     */
    public long zrem(String key, String... members) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return 0;
        recordAccess(key);
        long removed = zset.remove(members);
        if (zset.isEmpty()) {
            delInternal(key);
        }
        return removed;
    }

    /**
     * Returns score of member (ZSCORE).
     */
    public Double zscore(String key, String member) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return null;
        recordAccess(key);
        return zset.score(member);
    }

    /**
     * Returns number of members (ZCARD).
     */
    public long zcard(String key) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return 0;
        recordAccess(key);
        return zset.size();
    }

    /**
     * Increments member score (ZINCRBY).
     */
    public double zincrby(String key, double increment, String member) {
        checkExpired(key);
        ensureCapacity(key);
        SortedSetContainer zset = sortedSets.computeIfAbsent(key, k -> new SortedSetContainer());
        double result = zset.incrBy(increment, member);
        recordCreation(key);
        return result;
    }

    /**
     * Returns rank of member (0-based, lowest score first) (ZRANK).
     */
    public Long zrank(String key, String member) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return null;
        recordAccess(key);
        return zset.rank(member);
    }

    /**
     * Returns rank of member (0-based, highest score first) (ZREVRANK).
     */
    public Long zrevrank(String key, String member) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return null;
        recordAccess(key);
        return zset.revRank(member);
    }

    /**
     * Returns members in range (by rank, lowest first) (ZRANGE).
     */
    public List<String> zrange(String key, long start, long stop) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return Collections.emptyList();
        recordAccess(key);
        return zset.range(start, stop);
    }

    /**
     * Returns members in reverse range (highest first) (ZREVRANGE).
     */
    public List<String> zrevrange(String key, long start, long stop) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return Collections.emptyList();
        recordAccess(key);
        return zset.revRange(start, stop);
    }

    /**
     * Returns members with scores in range (ZRANGE ... WITHSCORES).
     */
    public List<Map.Entry<String, Double>> zrangeWithScores(String key, long start, long stop) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return Collections.emptyList();
        recordAccess(key);
        return zset.rangeWithScores(start, stop);
    }

    /**
     * Counts members with scores between min and max (ZCOUNT).
     */
    public long zcount(String key, double min, double max) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return 0;
        recordAccess(key);
        return zset.count(min, max);
    }

    /**
     * Removes members in range by rank (ZREMRANGEBYRANK).
     */
    public long zremrangebyrank(String key, long start, long stop) {
        List<String> toRemove = zrange(key, start, stop);
        return zrem(key, toRemove.toArray(new String[0]));
    }

    /**
     * Removes all members with scores between min and max inclusive (ZREMRANGEBYSCORE).
     *
     * @param key sorted set key
     * @param min minimum score (inclusive)
     * @param max maximum score (inclusive)
     * @return number of members removed
     */
    public long zremrangebyscore(String key, double min, double max) {
        checkExpired(key);
        SortedSetContainer zset = sortedSets.get(key);
        if (zset == null) return 0;
        recordAccess(key);
        return zset.remRangeByScore(min, max);
    }


    // ==================== UTILITY & METRICS METHODS ====================

    /**
     * Clears all data from all data structures.
     */
    public void flushall() {
        stringStore.clear();
        lists.clear();
        sets.clear();
        hashes.clear();
        sortedSets.clear();
        keyExpirations.clear();
        lastAccessTimes.clear();
        creationTimes.clear();
        allKeys.clear();
        for (Object lock : listLocks.values()) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Returns cache statistics.
     */
    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("string_keys", stringStore.size());
        info.put("list_keys", lists.size());
        info.put("set_keys", sets.size());
        info.put("hash_keys", hashes.size());
        info.put("sorted_set_keys", sortedSets.size());
        info.put("total_keys", allKeys.size());
        info.put("hit_count", stringStore.getHitCount());
        info.put("miss_count", stringStore.getMissCount());
        info.put("hit_ratio", stringStore.getHitRatio());
        info.put("max_capacity", maxCapacity);
        info.put("eviction_policy", evictionPolicy.name());
        info.put("evicted_keys", evictedCount.get());
        info.put("expired_keys", expiredCount.get());
        return info;
    }

    /**
     * Resets statistics.
     */
    public void resetStats() {
        stringStore.resetStats();
        evictedCount.set(0);
        expiredCount.set(0);
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    public long getEvictedCount() {
        return evictedCount.get();
    }

    public long getExpiredCount() {
        return expiredCount.get();
    }

    /**
     * Shuts down the cache and releases resources.
     */
    public void shutdown() {
        running = false;
        stringStore.shutdown();
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ==================== SORTED SET CONTAINER CLASS ====================

    private static class SortedSetContainer {
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final TreeMap<Double, Set<String>> scoreToMembers = new TreeMap<>();
        private final Map<String, Double> memberToScores = new HashMap<>();

        int size() {
            rwLock.readLock().lock();
            try {
                return memberToScores.size();
            } finally {
                rwLock.readLock().unlock();
            }
        }

        boolean isEmpty() {
            return size() == 0;
        }

        long add(double score, String member) {
            rwLock.writeLock().lock();
            try {
                Double oldScore = memberToScores.get(member);
                if (oldScore != null) {
                    if (Double.compare(oldScore, score) == 0) {
                        return 0;
                    }
                    Set<String> oldBucket = scoreToMembers.get(oldScore);
                    if (oldBucket != null) {
                        oldBucket.remove(member);
                        if (oldBucket.isEmpty()) {
                            scoreToMembers.remove(oldScore);
                        }
                    }
                }
                memberToScores.put(member, score);
                scoreToMembers.computeIfAbsent(score, s -> new LinkedHashSet<>()).add(member);
                return oldScore == null ? 1 : 0;
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        long remove(String... members) {
            rwLock.writeLock().lock();
            try {
                long removed = 0;
                for (String member : members) {
                    Double score = memberToScores.remove(member);
                    if (score != null) {
                        Set<String> bucket = scoreToMembers.get(score);
                        if (bucket != null) {
                            bucket.remove(member);
                            if (bucket.isEmpty()) {
                                scoreToMembers.remove(score);
                            }
                        }
                        removed++;
                    }
                }
                return removed;
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        Double score(String member) {
            rwLock.readLock().lock();
            try {
                return memberToScores.get(member);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        double incrBy(double delta, String member) {
            rwLock.writeLock().lock();
            try {
                Double current = memberToScores.get(member);
                double newScore = (current != null ? current : 0.0) + delta;
                add(newScore, member);
                return newScore;
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        Long rank(String member) {
            rwLock.readLock().lock();
            try {
                Double memberScore = memberToScores.get(member);
                if (memberScore == null) return null;
                long rank = 0;
                for (Map.Entry<Double, Set<String>> entry : scoreToMembers.entrySet()) {
                    if (entry.getKey() < memberScore) {
                        rank += entry.getValue().size();
                    } else if (Double.compare(entry.getKey(), memberScore) == 0) {
                        for (String m : entry.getValue()) {
                            if (m.equals(member)) return rank;
                            rank++;
                        }
                    }
                }
                return rank;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        Long revRank(String member) {
            rwLock.readLock().lock();
            try {
                Long r = rank(member);
                if (r == null) return null;
                return memberToScores.size() - 1 - r;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        List<String> range(long start, long stop) {
            rwLock.readLock().lock();
            try {
                List<String> all = new ArrayList<>();
                for (Set<String> bucket : scoreToMembers.values()) {
                    all.addAll(bucket);
                }
                long size = all.size();
                if (start < 0) start = Math.max(0, size + start);
                if (stop < 0) stop = size + stop;
                if (start > stop || start >= size) return Collections.emptyList();
                stop = Math.min(stop, size - 1);
                return new ArrayList<>(all.subList((int) start, (int) stop + 1));
            } finally {
                rwLock.readLock().unlock();
            }
        }

        List<String> revRange(long start, long stop) {
            rwLock.readLock().lock();
            try {
                List<String> all = new ArrayList<>();
                for (Set<String> bucket : scoreToMembers.descendingMap().values()) {
                    all.addAll(bucket);
                }
                long size = all.size();
                if (start < 0) start = Math.max(0, size + start);
                if (stop < 0) stop = size + stop;
                if (start > stop || start >= size) return Collections.emptyList();
                stop = Math.min(stop, size - 1);
                return new ArrayList<>(all.subList((int) start, (int) stop + 1));
            } finally {
                rwLock.readLock().unlock();
            }
        }

        List<Map.Entry<String, Double>> rangeWithScores(long start, long stop) {
            rwLock.readLock().lock();
            try {
                List<String> members = range(start, stop);
                List<Map.Entry<String, Double>> result = new ArrayList<>();
                for (String member : members) {
                    result.add(new AbstractMap.SimpleEntry<>(member, memberToScores.get(member)));
                }
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        long count(double min, double max) {
            rwLock.readLock().lock();
            try {
                long c = 0;
                for (Map.Entry<Double, Set<String>> entry : scoreToMembers.entrySet()) {
                    if (entry.getKey() >= min && entry.getKey() <= max) {
                        c += entry.getValue().size();
                    }
                }
                return c;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        long remRangeByScore(double min, double max) {
            rwLock.writeLock().lock();
            try {
                long removed = 0;
                Iterator<Map.Entry<Double, Set<String>>> it = scoreToMembers.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Double, Set<String>> entry = it.next();
                    double score = entry.getKey();
                    if (score >= min && score <= max) {
                        for (String m : entry.getValue()) {
                            memberToScores.remove(m);
                            removed++;
                        }
                        it.remove();
                    }
                }
                return removed;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
}
