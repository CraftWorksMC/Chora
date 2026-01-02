package com.craftworks.music.providers.navidrome

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

data class CachedResponse(val data: List<Any>, val timestamp: Long)

object NavidromeCache {
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    val cacheDuration = 15.minutes

    // Limit cache to prevent unbounded memory growth
    private const val MAX_CACHE_ENTRIES = 500

    fun get(key: String) : List<Any>? {
        val cachedResponse = cache[key] ?: return null
        val now = System.currentTimeMillis()
        if (now - cachedResponse.timestamp > cacheDuration.inWholeMilliseconds) {
            cache.remove(key)
            return null // Cache expired
        }
        return cachedResponse.data
    }

    fun put(key: String, data: List<Any>) {
        // Evict oldest entries if cache is full
        if (cache.size >= MAX_CACHE_ENTRIES) {
            evictOldestEntries()
        }
        cache[key] = CachedResponse(data, System.currentTimeMillis())
    }

    private fun evictOldestEntries() {
        // Remove oldest 20% of entries when cache is full
        val entriesToRemove = (MAX_CACHE_ENTRIES * 0.2).toInt().coerceAtLeast(1)
        cache.entries
            .sortedBy { it.value.timestamp }
            .take(entriesToRemove)
            .forEach { cache.remove(it.key) }
    }

    fun delByPrefix(prefix: String) {
        // Iterate over the keys in the cache
        val keysToRemove = cache.keys.filter { it.startsWith(prefix) }

        // Remove all the keys that match the prefix
        keysToRemove.forEach { cache.remove(it) }
    }

    fun clear() {
        cache.clear()
    }
}