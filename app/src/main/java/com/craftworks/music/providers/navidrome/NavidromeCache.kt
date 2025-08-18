package com.craftworks.music.providers.navidrome

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

data class CachedResponse(val data: List<Any>, val timestamp: Long)

object NavidromeCache {
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    val cacheDuration = 15.minutes

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
        cache[key] = CachedResponse(data, System.currentTimeMillis())
    }

    fun delByPrefix(prefix: String) {
        // Iterate over the keys in the cache
        val keysToRemove = cache.keys.filter { it.startsWith(prefix) }

        // Remove all the keys that match the prefix
        keysToRemove.forEach { cache.remove(it) }
    }
}