package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.MediaData
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

data class CachedResponse(val data: List<MediaData>, val timestamp: Long)

object NavidromeCache {
    private val cache = ConcurrentHashMap<String, CachedResponse>()
    val cacheDuration = 15.minutes

    fun get(key: String) : List<MediaData>? {
        val cachedResponse = cache[key] ?: return null
        val now = System.currentTimeMillis()
        if (now - cachedResponse.timestamp > cacheDuration.inWholeMilliseconds) {
            cache.remove(key)
            return null // Cache expired
        }
        return cachedResponse.data
    }

    fun put(key: String, data: List<MediaData>) {
        cache[key] = CachedResponse(data, System.currentTimeMillis())
    }
}