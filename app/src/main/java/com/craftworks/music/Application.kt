package com.craftworks.music

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageResult
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class ChoraApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NavidromeManager.init(this)
        LocalProviderManager.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // 20% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(NavidromePlaceholderInterceptor())
                    .build()
            }
            .respectCacheHeaders(false) // Don't respect server cache headers for artwork
            .crossfade(true)
            .build()
    }
}

/**
 * OkHttp interceptor that detects Navidrome placeholder images.
 * Navidrome returns image/webp for placeholder images and image/jpeg for real embedded art.
 * This interceptor returns a 404 error for webp responses on coverArt URLs,
 * which triggers Coil's error fallback to show generated artwork.
 */
class NavidromePlaceholderInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Only check for Navidrome cover art URLs
        val url = request.url.toString()
        if (url.contains("getCoverArt") || url.contains("coverArt")) {
            val contentType = response.header("Content-Type", "")

            // Navidrome returns webp for placeholder images, jpeg for real art
            if (contentType?.contains("image/webp") == true) {
                response.close()
                return okhttp3.Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(404)
                    .message("Navidrome placeholder detected")
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }
        }

        return response
    }
}
