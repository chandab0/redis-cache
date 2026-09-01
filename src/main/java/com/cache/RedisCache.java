package com.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Redis-like in-memory cache with support for multiple data structures.
 *
 * <p>Supported data types:
 * <ul>
 *   <li>Strings - simple key-value pairs with optional TTL</li>
 *   <li>Lists - ordered collections with push/pop operations</li>
 *   <li>Sets - unordered unique collections</li>
 *   <li>Sorted Sets - ordered unique collections with scores</li>
 *   <li>Hashes - field-value maps</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * RedisCache cache = new RedisCache();
 *
 * // String operations
 * cache.set("user:1", "John");
 * cache.set("session:abc", token, 3600000); // 1 hour TTL
 * String name = cache.get("user:1");
 *
 * // List operations
 * cache.lpush("queue", "task1", "task2");
 * String task = cache.rpop("queue");
 *
 * // Set operations
 * cache.sadd("tags", "java", "cache", "redis");
 * boolean isMember = cache.sismember("tags", "java");
 *
 * // Hash operations
 * cache.hset("user:1:profile", "name", "John");
 * cache.hset("user:1:profile", "age", "30");
 * String name = cache.hget("user:1:profile", "name");
 *
 * // Sorted Set operations
 * cache.zadd("leaderboard", 100, "player1");
 * cache.zadd("leaderboard", 200, "player2");
 * Set<String> top = cache.zrange("leaderboard", 0, 9);
 *
 * cache.shutdown();
 * </pre>
 *
 * @author Redis-Cache
 * @version 1.0.0
 */
public class RedisCache {
    private final CacheStore stringStore;
    private final ConcurrentHashMap<String, LinkedList<Object>> lists;
    private final ConcurrentHashMap<String, Set<Object>> sets;
    private final ConcurrentHashMap<String, Map<String, Object>> hashes;
    private final ConcurrentHashMap<String, TreeMap<Double, Set<String>>> sortedSets;
    private final ConcurrentHashMap<String, Map<String, Double>> sortedSetScores;

    private final long defaultCleanupInterval;

    public RedisCache() {
        this(1000);
    }

    public RedisCache(long cleanupIntervalMillis) {
        this.defaultCleanupInterval = cleanupIntervalMillis;
        this.stringStore = new CacheStore(cleanupIntervalMillis);
        this.lists = new ConcurrentHashMap<>();
        this.sets = new ConcurrentHashMap<>();
        this.hashes = new ConcurrentHashMap<>();
        this.sortedSets = new ConcurrentHashMap<>();
        this.sortedSetScores = new ConcurrentHashMap<>();
    }

    // ==================== STRING OPERATIONS ====================

    /**
     * Sets a string value.
     */
    public void set(String key, Object value) {
        stringStore.put(key, value);
    }

    /**
     * Sets a string value with TTL in milliseconds.
     */
    public void set(String key, Object value, long ttlMillis) {
        stringStore.put(key, value, ttlMillis);
    }

    /**
     * Sets a value only if key doesn't exist (SETNX).
     * @return true if set, false if key exists
     */
    public boolean setnx(String key, Object value) {
        return stringStore.setIfAbsent(key, value);
    }

    /**
     * Sets a value only if key doesn't exist, with TTL.
     */
    public boolean setnx(String key, Object value, long ttlMillis) {
        return stringStore.setIfAbsent(key, value, ttlMillis);
    }

    /**
     * Sets a value only if key exists (SETXX).
     * @return true if set, false if key doesn't exist
     */
    public boolean setxx(String key, Object value) {
        return stringStore.setIfExists(key, value);
    }

    /**
     * Gets a string value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return stringStore.get(key);
    }

    /**
     * Gets a value, returns default if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T value = stringStore.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a value or computes it if missing.
     */
    public <T> T getOrCompute(String key, Function<String, T> loader) {
        return stringStore.getOrCompute(key, loader);
    }

    /**
     * Gets a value or computes it if missing, with TTL.
     */
    public <T> T getOrCompute(String key, Function<String, T> loader, long ttlMillis) {
        return stringStore.getOrCompute(key, loader, ttlMillis);
    }

    /**
     * Gets the value and deletes the key (GETDEL).
     */
    public <T> T getDel(String key) {
        T value = stringStore.get(key);
        if (value != null) {
            stringStore.delete(key);
        }
        return value;
    }

    /**
     * Sets a new value and returns the old value (GETSET).
     */
    @SuppressWarnings("unchecked")
    public <T> T getSet(String key, Object newValue) {
        T oldValue = stringStore.get(key);
        stringStore.put(key, newValue);
        return oldValue;
    }

    /**
     * Sets multiple keys at once (MSET).
     */
    public void mset(Map<String, Object> keyValueMap) {
        keyValueMap.forEach(stringStore::put);
    }

    /**
     * Gets multiple keys at once (MGET).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> mget(String... keys) {
        List<T> result = new ArrayList<>();
        for (String key : keys) {
            result.add((T) stringStore.get(key));
        }
        return result;
    }

    /**
     * Increments a numeric value by 1.
     */
    public long incr(String key) {
        return stringStore.increment(key);
    }

    /**
     * Increments a numeric value by delta.
     */
    public long incrBy(String key, long delta) {
        return stringStore.increment(key, delta);
    }

    /**
     * Decrements a numeric value by 1.
     */
    public long decr(String key) {
        return stringStore.decrement(key);
    }

    /**
     * Decrements a numeric value by delta.
     */
    public long decrBy(String key, long delta) {
        return stringStore.decrement(key, delta);
    }

    /**
     * Appends to a string value.
     * @return new length of the string
     */
    public long append(String key, String value) {
        String existing = stringStore.get(key);
        String newValue = existing != null ? existing + value : value;
        stringStore.put(key, newValue);
        return newValue.length();
    }

    /**
     * Gets substring of a string value.
     */
    public String getRange(String key, int start, int end) {
        String value = stringStore.get(key);
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
        String value = stringStore.get(key);
        return value != null ? value.length() : 0;
    }

    // ==================== KEY OPERATIONS ====================

    /**
     * Deletes one or more keys.
     * @return number of keys deleted
     */
    public long del(String... keys) {
        return stringStore.delete(keys);
    }

    /**
     * Checks if key exists.
     */
    public boolean exists(String key) {
        return stringStore.exists(key) ||
               lists.containsKey(key) ||
               sets.containsKey(key) ||
               hashes.containsKey(key) ||
               sortedSets.containsKey(key);
    }

    /**
     * Sets TTL on a key in milliseconds.
     */
    public boolean pexpire(String key, long milliseconds) {
        return stringStore.expire(key, milliseconds);
    }

    /**
     * Sets TTL on a key in seconds.
     */
    public boolean expire(String key, long seconds) {
        return pexpire(key, seconds * 1000);
    }

    /**
     * Gets remaining TTL in milliseconds.
     */
    public long pttl(String key) {
        return stringStore.getTtl(key);
    }

    /**
     * Gets remaining TTL in seconds.
     */
    public long ttl(String key) {
        long ms = pttl(key);
        return ms < 0 ? ms : ms / 1000;
    }

    /**
     * Removes TTL from a key.
     */
    public boolean persist(String key) {
        return stringStore.persist(key);
    }

    /**
     * Finds all keys matching pattern.
     */
    public Set<String> keys(String pattern) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(stringStore.keys(pattern));

        String regex = patternToRegex(pattern);
        lists.keySet().stream().filter(k -> k.matches(regex)).forEach(allKeys::add);
        sets.keySet().stream().filter(k -> k.matches(regex)).forEach(allKeys::add);
        hashes.keySet().stream().filter(k -> k.matches(regex)).forEach(allKeys::add);
        sortedSets.keySet().stream().filter(k -> k.matches(regex)).forEach(allKeys::add);

        return allKeys;
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
        // String store
        Object value = stringStore.get(oldKey);
        if (value != null) {
            stringStore.put(newKey, value);
            stringStore.delete(oldKey);
        }

        // Lists
        LinkedList<Object> list = lists.remove(oldKey);
        if (list != null) {
            lists.put(newKey, list);
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
        TreeMap<Double, Set<String>> sortedSet = sortedSets.remove(oldKey);
        Map<String, Double> scores = sortedSetScores.remove(oldKey);
        if (sortedSet != null) {
            sortedSets.put(newKey, sortedSet);
            sortedSetScores.put(newKey, scores);
        }
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

    // ==================== LIST OPERATIONS ====================

    /**
     * Pushes elements to the left of a list (LPUSH).
     * @return length of list after push
     */
    public long lpush(String key, Object... values) {
        LinkedList<Object> list = lists.computeIfAbsent(key, k -> new LinkedList<>());
        for (Object value : values) {
            list.addFirst(value);
        }
        return list.size();
    }

    /**
     * Pushes elements to the right of a list (RPUSH).
     * @return length of list after push
     */
    public long rpush(String key, Object... values) {
        LinkedList<Object> list = lists.computeIfAbsent(key, k -> new LinkedList<>());
        for (Object value : values) {
            list.addLast(value);
        }
        return list.size();
    }

    /**
     * Pushes to left only if list exists (LPUSHX).
     */
    public long lpushx(String key, Object value) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return 0;
        list.addFirst(value);
        return list.size();
    }

    /**
     * Pushes to right only if list exists (RPUSHX).
     */
    public long rpushx(String key, Object value) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return 0;
        list.addLast(value);
        return list.size();
    }

    /**
     * Pops from the left of a list (LPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T lpop(String key) {
        LinkedList<Object> list = lists.get(key);
        if (list == null || list.isEmpty()) return null;
        T value = (T) list.removeFirst();
        if (list.isEmpty()) lists.remove(key);
        return value;
    }

    /**
     * Pops from the right of a list (RPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T rpop(String key) {
        LinkedList<Object> list = lists.get(key);
        if (list == null || list.isEmpty()) return null;
        T value = (T) list.removeLast();
        if (list.isEmpty()) lists.remove(key);
        return value;
    }

    /**
     * Gets element by index (LINDEX).
     */
    @SuppressWarnings("unchecked")
    public <T> T lindex(String key, long index) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return null;
        if (index < 0) index = list.size() + index;
        if (index < 0 || index >= list.size()) return null;
        return (T) list.get((int) index);
    }

    /**
     * Returns length of list (LLEN).
     */
    public long llen(String key) {
        LinkedList<Object> list = lists.get(key);
        return list != null ? list.size() : 0;
    }

    /**
     * Gets a range of elements (LRANGE).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> lrange(String key, long start, long stop) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return Collections.emptyList();

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

    /**
     * Trims list to specified range (LTRIM).
     */
    public void ltrim(String key, long start, long stop) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return;

        int size = list.size();
        if (start < 0) start = Math.max(0, size + start);
        if (stop < 0) stop = size + stop;
        if (start > stop) {
            lists.remove(key);
            return;
        }

        stop = Math.min(stop, size - 1);
        LinkedList<Object> newList = new LinkedList<>();
        for (int i = (int) start; i <= stop; i++) {
            newList.add(list.get(i));
        }
        lists.put(key, newList);
    }

    /**
     * Sets element at index (LSET).
     */
    public boolean lset(String key, long index, Object value) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return false;
        if (index < 0) index = list.size() + index;
        if (index < 0 || index >= list.size()) return false;
        list.set((int) index, value);
        return true;
    }

    /**
     * Removes elements matching value (LREM).
     * @param count positive: remove from head; negative: from tail; zero: all
     * @return number of elements removed
     */
    public long lrem(String key, long count, Object value) {
        LinkedList<Object> list = lists.get(key);
        if (list == null) return 0;

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
        if (list.isEmpty()) lists.remove(key);
        return removed;
    }

    /**
     * Pops from right and pushes to left of another list (RPOPLPUSH).
     */
    public <T> T rpoplpush(String source, String destination) {
        T value = rpop(source);
        if (value != null) {
            lpush(destination, value);
        }
        return value;
    }

    /**
     * Blocks until pop from left (BLPOP) - simplified non-blocking version.
     * Returns immediately if list is empty.
     */
    public <T> T blpop(String key, long timeoutSeconds) {
        return lpop(key);
    }

    /**
     * Blocks until pop from right (BRPOP) - simplified non-blocking version.
     */
    public <T> T brpop(String key, long timeoutSeconds) {
        return rpop(key);
    }

    // ==================== SET OPERATIONS ====================

    /**
     * Adds members to a set (SADD).
     * @return number of members added (not already present)
     */
    public long sadd(String key, Object... members) {
        Set<Object> set = sets.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        long added = 0;
        for (Object member : members) {
            if (set.add(member)) added++;
        }
        return added;
    }

    /**
     * Removes members from a set (SREM).
     * @return number of members removed
     */
    public long srem(String key, Object... members) {
        Set<Object> set = sets.get(key);
        if (set == null) return 0;
        long removed = 0;
        for (Object member : members) {
            if (set.remove(member)) removed++;
        }
        if (set.isEmpty()) sets.remove(key);
        return removed;
    }

    /**
     * Gets all members of a set (SMEMBERS).
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> smembers(String key) {
        Set<Object> set = sets.get(key);
        return set != null ? new HashSet<>((Set<T>) set) : Collections.emptySet();
    }

    /**
     * Checks if member exists in set (SISMEMBER).
     */
    public boolean sismember(String key, Object member) {
        Set<Object> set = sets.get(key);
        return set != null && set.contains(member);
    }

    /**
     * Returns number of members in set (SCARD).
     */
    public long scard(String key) {
        Set<Object> set = sets.get(key);
        return set != null ? set.size() : 0;
    }

    /**
     * Pops a random member from set (SPOP).
     */
    @SuppressWarnings("unchecked")
    public <T> T spop(String key) {
        Set<Object> set = sets.get(key);
        if (set == null || set.isEmpty()) return null;
        Iterator<Object> iter = set.iterator();
        T value = (T) iter.next();
        iter.remove();
        if (set.isEmpty()) sets.remove(key);
        return value;
    }

    /**
     * Returns a random member from set (SRANDMEMBER).
     */
    @SuppressWarnings("unchecked")
    public <T> T srandmember(String key) {
        Set<Object> set = sets.get(key);
        if (set == null || set.isEmpty()) return null;
        return (T) set.iterator().next();
    }

    /**
     * Moves member from one set to another (SMOVE).
     */
    public boolean smove(String source, String destination, Object member) {
        Set<Object> srcSet = sets.get(source);
        if (srcSet == null || !srcSet.contains(member)) return false;
        srcSet.remove(member);
        if (srcSet.isEmpty()) sets.remove(source);
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
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        return hash.put(field, value) == null ? 1 : 0;
    }

    /**
     * Sets multiple fields in hash (HMSET).
     */
    public void hmset(String key, Map<String, Object> fieldValueMap) {
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        hash.putAll(fieldValueMap);
    }

    /**
     * Gets field from hash (HGET).
     */
    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String field) {
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return null;
        return (T) hash.get(field);
    }

    /**
     * Gets multiple fields from hash (HMGET).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hmget(String key, String... fields) {
        Map<String, Object> hash = hashes.get(key);
        List<T> result = new ArrayList<>();
        if (hash == null) {
            for (int i = 0; i < fields.length; i++) result.add(null);
            return result;
        }
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
        Map<String, Object> hash = hashes.get(key);
        return hash != null ? new HashMap<>((Map<String, T>) hash) : Collections.emptyMap();
    }

    /**
     * Deletes fields from hash (HDEL).
     * @return number of fields removed
     */
    public long hdel(String key, String... fields) {
        Map<String, Object> hash = hashes.get(key);
        if (hash == null) return 0;
        long removed = 0;
        for (String field : fields) {
            if (hash.remove(field) != null) removed++;
        }
        if (hash.isEmpty()) hashes.remove(key);
        return removed;
    }

    /**
     * Checks if field exists in hash (HEXISTS).
     */
    public boolean hexists(String key, String field) {
        Map<String, Object> hash = hashes.get(key);
        return hash != null && hash.containsKey(field);
    }

    /**
     * Returns all fields in hash (HKEYS).
     */
    public Set<String> hkeys(String key) {
        Map<String, Object> hash = hashes.get(key);
        return hash != null ? new HashSet<>(hash.keySet()) : Collections.emptySet();
    }

    /**
     * Returns all values in hash (HVALS).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hvals(String key) {
        Map<String, Object> hash = hashes.get(key);
        return hash != null ? new ArrayList<>((Collection<T>) hash.values()) : Collections.emptyList();
    }

    /**
     * Returns number of fields in hash (HLEN).
     */
    public long hlen(String key) {
        Map<String, Object> hash = hashes.get(key);
        return hash != null ? hash.size() : 0;
    }

    /**
     * Increments hash field by value (HINCRBY).
     */
    public long hincrBy(String key, String field, long increment) {
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        Object value = hash.get(field);
        long current = 0;
        if (value instanceof Number) {
            current = ((Number) value).longValue();
        } else if (value instanceof String) {
            current = Long.parseLong((String) value);
        }
        long newValue = current + increment;
        hash.put(field, newValue);
        return newValue;
    }

    /**
     * Increments hash field by float (HINCRBYFLOAT).
     */
    public double hincrByFloat(String key, String field, double increment) {
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        Object value = hash.get(field);
        double current = 0;
        if (value instanceof Number) {
            current = ((Number) value).doubleValue();
        } else if (value instanceof String) {
            current = Double.parseDouble((String) value);
        }
        double newValue = current + increment;
        hash.put(field, newValue);
        return newValue;
    }

    /**
     * Sets field only if it doesn't exist (HSETNX).
     */
    public boolean hsetnx(String key, String field, Object value) {
        Map<String, Object> hash = hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        if (hash.containsKey(field)) return false;
        hash.put(field, value);
        return true;
    }

    // ==================== SORTED SET OPERATIONS ====================

    /**
     * Adds member with score to sorted set (ZADD).
     * @return 1 if new member, 0 if score updated
     */
    public long zadd(String key, double score, String member) {
        TreeMap<Double, Set<String>> sortedSet = sortedSets.computeIfAbsent(key, k -> new TreeMap<>());
        Map<String, Double> scores = sortedSetScores.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        Double oldScore = scores.get(member);
        if (oldScore != null) {
            // Remove from old score bucket
            Set<String> bucket = sortedSet.get(oldScore);
            if (bucket != null) {
                bucket.remove(member);
                if (bucket.isEmpty()) sortedSet.remove(oldScore);
            }
        }

        // Add to new score bucket
        sortedSet.computeIfAbsent(score, s -> ConcurrentHashMap.newKeySet()).add(member);
        scores.put(member, score);

        return oldScore == null ? 1 : 0;
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
        TreeMap<Double, Set<String>> sortedSet = sortedSets.get(key);
        Map<String, Double> scores = sortedSetScores.get(key);
        if (sortedSet == null || scores == null) return 0;

        long removed = 0;
        for (String member : members) {
            Double score = scores.remove(member);
            if (score != null) {
                Set<String> bucket = sortedSet.get(score);
                if (bucket != null) {
                    bucket.remove(member);
                    if (bucket.isEmpty()) sortedSet.remove(score);
                }
                removed++;
            }
        }

        if (scores.isEmpty()) {
            sortedSets.remove(key);
            sortedSetScores.remove(key);
        }
        return removed;
    }

    /**
     * Returns score of member (ZSCORE).
     */
    public Double zscore(String key, String member) {
        Map<String, Double> scores = sortedSetScores.get(key);
        return scores != null ? scores.get(member) : null;
    }

    /**
     * Returns number of members (ZCARD).
     */
    public long zcard(String key) {
        Map<String, Double> scores = sortedSetScores.get(key);
        return scores != null ? scores.size() : 0;
    }

    /**
     * Increments member score (ZINCRBY).
     */
    public double zincrby(String key, double increment, String member) {
        Double currentScore = zscore(key, member);
        double newScore = (currentScore != null ? currentScore : 0) + increment;
        zadd(key, newScore, member);
        return newScore;
    }

    /**
     * Returns rank of member (0-based, lowest score first) (ZRANK).
     */
    public Long zrank(String key, String member) {
        Map<String, Double> scores = sortedSetScores.get(key);
        TreeMap<Double, Set<String>> sortedSet = sortedSets.get(key);
        if (scores == null || sortedSet == null || !scores.containsKey(member)) return null;

        double memberScore = scores.get(member);
        long rank = 0;
        for (Map.Entry<Double, Set<String>> entry : sortedSet.entrySet()) {
            if (entry.getKey() < memberScore) {
                rank += entry.getValue().size();
            } else if (entry.getKey() == memberScore) {
                // Count members before this one in same score bucket
                for (String m : entry.getValue()) {
                    if (m.equals(member)) return rank;
                    rank++;
                }
            }
        }
        return rank;
    }

    /**
     * Returns rank of member (0-based, highest score first) (ZREVRANK).
     */
    public Long zrevrank(String key, String member) {
        Long rank = zrank(key, member);
        if (rank == null) return null;
        return zcard(key) - 1 - rank;
    }

    /**
     * Returns members in range (by rank, lowest first) (ZRANGE).
     */
    public List<String> zrange(String key, long start, long stop) {
        TreeMap<Double, Set<String>> sortedSet = sortedSets.get(key);
        if (sortedSet == null) return Collections.emptyList();

        List<String> allMembers = new ArrayList<>();
        for (Set<String> bucket : sortedSet.values()) {
            allMembers.addAll(bucket);
        }

        long size = allMembers.size();
        if (start < 0) start = Math.max(0, size + start);
        if (stop < 0) stop = size + stop;
        if (start > stop || start >= size) return Collections.emptyList();
        stop = Math.min(stop, size - 1);

        return allMembers.subList((int) start, (int) stop + 1);
    }

    /**
     * Returns members in reverse range (highest first) (ZREVRANGE).
     */
    public List<String> zrevrange(String key, long start, long stop) {
        TreeMap<Double, Set<String>> sortedSet = sortedSets.get(key);
        if (sortedSet == null) return Collections.emptyList();

        List<String> allMembers = new ArrayList<>();
        for (Set<String> bucket : sortedSet.descendingMap().values()) {
            allMembers.addAll(bucket);
        }

        long size = allMembers.size();
        if (start < 0) start = Math.max(0, size + start);
        if (stop < 0) stop = size + stop;
        if (start > stop || start >= size) return Collections.emptyList();
        stop = Math.min(stop, size - 1);

        return allMembers.subList((int) start, (int) stop + 1);
    }

    /**
     * Returns members with scores in range (ZRANGE ... WITHSCORES).
     */
    public List<Map.Entry<String, Double>> zrangeWithScores(String key, long start, long stop) {
        List<String> members = zrange(key, start, stop);
        List<Map.Entry<String, Double>> result = new ArrayList<>();
        Map<String, Double> scores = sortedSetScores.get(key);
        if (scores == null) return result;
        for (String member : members) {
            result.add(new AbstractMap.SimpleEntry<>(member, scores.get(member)));
        }
        return result;
    }

    /**
     * Counts members with scores between min and max (ZCOUNT).
     */
    public long zcount(String key, double min, double max) {
        TreeMap<Double, Set<String>> sortedSet = sortedSets.get(key);
        if (sortedSet == null) return 0;

        long count = 0;
        for (Map.Entry<Double, Set<String>> entry : sortedSet.entrySet()) {
            if (entry.getKey() >= min && entry.getKey() <= max) {
                count += entry.getValue().size();
            }
        }
        return count;
    }

    /**
     * Removes members in range by rank (ZREMRANGEBYRANK).
     */
    public long zremrangebyrank(String key, long start, long stop) {
        List<String> toRemove = zrange(key, start, stop);
        return zrem(key, toRemove.toArray(new String[0]));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clears all data from all data structures.
     */
    public void flushall() {
        stringStore.clear();
        lists.clear();
        sets.clear();
        hashes.clear();
        sortedSets.clear();
        sortedSetScores.clear();
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
        info.put("hit_count", stringStore.getHitCount());
        info.put("miss_count", stringStore.getMissCount());
        info.put("hit_ratio", stringStore.getHitRatio());
        return info;
    }

    /**
     * Resets statistics.
     */
    public void resetStats() {
        stringStore.resetStats();
    }

    /**
     * Shuts down the cache and releases resources.
     */
    public void shutdown() {
        stringStore.shutdown();
    }
}
