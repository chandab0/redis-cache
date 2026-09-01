package com.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Thread-safe in-memory cache store with TTL support.
 * Supports generic key-value storage with expiration.
 */
public class CacheStore {
    private final ConcurrentHashMap<String, CacheEntry> store;
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final long cleanupIntervalMillis;
    private volatile boolean running = true;

    /**
     * Creates a cache store with default cleanup interval of 1 second.
     */
    public CacheStore() {
        this(1000);
    }

    /**
     * Creates a cache store with specified cleanup interval.
     * @param cleanupIntervalMillis interval for expired entry cleanup
     */
    public CacheStore(long cleanupIntervalMillis) {
        this.store = new ConcurrentHashMap<>();
        this.cleanupIntervalMillis = cleanupIntervalMillis;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleanup");
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

    /**
     * Stores a value without expiration.
     */
    public void put(String key, Object value) {
        store.put(key, new CacheEntry(value));
    }

    /**
     * Stores a value with TTL (time-to-live) in milliseconds.
     */
    public void put(String key, Object value, long ttlMillis) {
        store.put(key, new CacheEntry(value, ttlMillis));
    }

    /**
     * Stores a value only if key doesn't exist.
     * @return true if set, false if key already exists
     */
    public boolean setIfAbsent(String key, Object value) {
        return store.putIfAbsent(key, new CacheEntry(value)) == null;
    }

    /**
     * Stores a value only if key doesn't exist, with TTL.
     */
    public boolean setIfAbsent(String key, Object value, long ttlMillis) {
        return store.putIfAbsent(key, new CacheEntry(value, ttlMillis)) == null;
    }

    /**
     * Stores a value only if key already exists.
     * @return true if set, false if key doesn't exist
     */
    public boolean setIfExists(String key, Object value) {
        return store.computeIfPresent(key, (k, v) -> new CacheEntry(value)) != null;
    }

    /**
     * Stores a value only if key already exists, with TTL.
     */
    public boolean setIfExists(String key, Object value, long ttlMillis) {
        return store.computeIfPresent(key, (k, v) -> new CacheEntry(value, ttlMillis)) != null;
    }

    /**
     * Retrieves a value by key.
     * @return the value, or null if not found or expired
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            return null;
        }
        if (entry.isExpired()) {
            store.remove(key);
            missCount.incrementAndGet();
            return null;
        }
        hitCount.incrementAndGet();
        return (T) entry.getValue();
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
        CacheEntry entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            hitCount.incrementAndGet();
            return (T) entry.getValue();
        }

        T value = loader.apply(key);
        if (value != null) {
            store.put(key, new CacheEntry(value, ttlMillis));
        }
        missCount.incrementAndGet();
        return value;
    }

    /**
     * Deletes a key.
     * @return true if key was deleted
     */
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    /**
     * Deletes multiple keys.
     * @return number of keys deleted
     */
    public long delete(String... keys) {
        long count = 0;
        for (String key : keys) {
            if (store.remove(key) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if key exists and is not expired.
     */
    public boolean exists(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            store.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Sets TTL on an existing key.
     * @return true if key exists, false otherwise
     */
    public boolean expire(String key, long ttlMillis) {
        CacheEntry entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        store.put(key, new CacheEntry(entry.getValue(), ttlMillis));
        return true;
    }

    /**
     * Gets remaining TTL in milliseconds.
     * @return -1 if no TTL, -2 if key doesn't exist, otherwise remaining TTL
     */
    public long getTtl(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return -2;
        }
        if (entry.isExpired()) {
            store.remove(key);
            return -2;
        }
        long expireAt = entry.getExpireAt();
        if (expireAt == 0) {
            return -1;
        }
        return Math.max(0, expireAt - System.currentTimeMillis());
    }

    /**
     * Removes TTL from a key (makes it persistent).
     */
    public boolean persist(String key) {
        CacheEntry entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            return false;
        }
        store.put(key, new CacheEntry(entry.getValue()));
        return true;
    }

    /**
     * Increments a numeric value by 1.
     * @return the new value after increment
     */
    public long increment(String key) {
        return increment(key, 1);
    }

    /**
     * Increments a numeric value by delta.
     */
    public long increment(String key, long delta) {
        CacheEntry result = store.compute(key, (k, entry) -> {
            long current = 0;
            if (entry != null && !entry.isExpired()) {
                Object val = entry.getValue();
                if (val instanceof Number) {
                    current = ((Number) val).longValue();
                } else if (val instanceof String) {
                    try {
                        current = Long.parseLong((String) val);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Value is not a number");
                    }
                } else {
                    throw new IllegalArgumentException("Value is not a number");
                }
            }
            return new CacheEntry(current + delta);
        });
        return (Long) result.getValue();
    }

    /**
     * Decrements a numeric value by 1.
     */
    public long decrement(String key) {
        return increment(key, -1);
    }

    /**
     * Decrements a numeric value by delta.
     */
    public long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    /**
     * Returns all keys matching a pattern.
     * Pattern supports * (any chars) and ? (single char).
     */
    public Set<String> keys(String pattern) {
        Set<String> result = new HashSet<>();
        String regex = patternToRegex(pattern);
        for (String key : store.keySet()) {
            CacheEntry entry = store.get(key);
            if (entry != null && !entry.isExpired() && key.matches(regex)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Returns all keys.
     */
    public Set<String> keys() {
        return keys("*");
    }

    private String patternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
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

    /**
     * Clears all entries.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Returns number of entries (excluding expired).
     */
    public int size() {
        cleanupExpired();
        return store.size();
    }

    /**
     * Returns cache hit count.
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * Returns cache miss count.
     */
    public long getMissCount() {
        return missCount.get();
    }

    /**
     * Returns hit ratio (0.0 to 1.0).
     */
    public double getHitRatio() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }

    /**
     * Resets statistics.
     */
    public void resetStats() {
        hitCount.set(0);
        missCount.set(0);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> {
            long expireAt = entry.getValue().getExpireAt();
            return expireAt > 0 && now > expireAt;
        });
    }

    /**
     * Shuts down the cleanup executor.
     */
    public void shutdown() {
        running = false;
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
}
