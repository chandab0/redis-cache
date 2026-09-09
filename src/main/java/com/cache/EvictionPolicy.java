package com.cache;

/**
 * Cache eviction policies when maximum capacity is reached.
 */
public enum EvictionPolicy {
    /**
     * Do not evict keys. Writes that exceed capacity throw {@link IllegalStateException}.
     */
    NO_EVICTION,

    /**
     * Evict the least recently used (LRU) keys among all keys.
     */
    ALLKEYS_LRU,

    /**
     * Evict the least recently used (LRU) keys only among keys with an active TTL.
     */
    VOLATILE_LRU,

    /**
     * Evict the keys with the shortest time-to-live (TTL) remaining.
     */
    VOLATILE_TTL,

    /**
     * Evict the oldest inserted keys (First-In, First-Out) among all keys.
     */
    ALLKEYS_FIFO
}
