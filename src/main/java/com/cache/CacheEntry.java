package com.cache;

import java.io.Serializable;

/**
 * Represents a cache entry with optional TTL (time-to-live).
 */
public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Object value;
    private final long expireAt; // 0 means no expiration

    public CacheEntry(Object value) {
        this(value, 0);
    }

    public CacheEntry(Object value, long ttlMillis) {
        this.value = value;
        this.expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0;
    }

    public Object getValue() {
        return value;
    }

    public boolean isExpired() {
        return expireAt > 0 && System.currentTimeMillis() > expireAt;
    }

    public long getExpireAt() {
        return expireAt;
    }
}
