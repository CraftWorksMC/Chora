package com.craftworks.music.player

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Singleton class that manages a MediaController instance.
 *
 * This class observes the Remember lifecycle to release the MediaController when it's no longer needed.
 */
@Stable
class MediaControllerManager private constructor(context: Context) : RememberObserver {
    private val appContext = context.applicationContext
    @Volatile
    private var factory: ListenableFuture<MediaController>? = null
    private val factoryLock = Any()

    private val _currentMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentMetadata: StateFlow<MediaMetadata?> = _currentMetadata.asStateFlow()

    var controller = mutableStateOf<MediaController?>(null)

    // Track if we're in a configuration change to avoid premature release
    @Volatile
    private var isReleasing = false

    init { initialize() }

    private fun publishCurrentMetadata(controller: MediaController?) {
        _currentMetadata.value = controller?.currentMediaItem?.mediaMetadata
    }

    /**
     * Initializes the MediaController.
     *
     * If the MediaController has not been built or has been released, this method will build a new one.
     * This method is thread-safe and handles concurrent initialization attempts.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    internal fun initialize() {
        isReleasing = false

        synchronized(factoryLock) {
            // Only create a new factory if one doesn't exist or the previous one completed
            if (factory == null || factory?.isDone == true) {
                factory = MediaController.Builder(
                    appContext,
                    SessionToken(appContext, ComponentName(appContext, ChoraMediaLibraryService::class.java))
                ).buildAsync().also { newFactory ->
                    // Add listener only to newly created factory to prevent duplicate listeners
                    newFactory.addListener(
                        {
                            // MediaController is available here with controllerFuture.get()
                            // Don't update if we're in the middle of releasing (config change)
                            if (isReleasing) return@addListener

                            try {
                                val newController = if (newFactory.isDone && !newFactory.isCancelled) {
                                    newFactory.get()
                                } else {
                                    null
                                }
                                controller.value = newController
                                // Only publish metadata after controller is assigned to avoid race
                                publishCurrentMetadata(newController)
                            } catch (e: java.util.concurrent.CancellationException) {
                                // Expected when future is cancelled during shutdown
                                if (!isReleasing) controller.value = null
                            } catch (e: java.util.concurrent.ExecutionException) {
                                // Service may be unavailable
                                if (!isReleasing) controller.value = null
                            } catch (e: Exception) {
                                if (!isReleasing) controller.value = null
                            }
                        },
                        MoreExecutors.directExecutor()
                    )
                }
            }
        }
    }

    /**
     * Releases the MediaController.
     *
     * This method will release the MediaController and set the controller state to null.
     * Thread-safe and guards against race conditions during configuration changes.
     */
    internal fun release() {
        isReleasing = true
        synchronized(factoryLock) {
            factory?.let {
                MediaController.releaseFuture(it)
                controller.value = null
            }
            factory = null
        }
    }

    // Lifecycle methods for the RememberObserver interface.
    override fun onAbandoned() { }
    override fun onForgotten() { }
    override fun onRemembered() {}

    companion object {
        @Volatile
        private var instance: MediaControllerManager? = null

        /**
         * Returns the Singleton instance of the MediaControllerManager.
         *
         * @param context The context to use when creating the MediaControllerManager.
         * @return The Singleton instance of the MediaControllerManager.
         */
        fun getInstance(context: Context): MediaControllerManager {
            return instance ?: synchronized(this) {
                instance ?: MediaControllerManager(context).also { instance = it }
            }
        }

        /**
         * Releases the singleton instance. Call this when the app is shutting down
         * to prevent memory leaks.
         */
        fun releaseInstance() {
            synchronized(this) {
                instance?.release()
                instance = null
            }
        }
    }
}



/**
 * A Composable function that provides a managed MediaController instance.
 *
 * @param lifecycle The lifecycle of the owner of this MediaController. Defaults to the lifecycle of the LocalLifecycleOwner.
 * @return A State object containing the MediaController instance. The Composable will automatically re-compose whenever the state changes.
 */
@Composable
fun rememberManagedMediaController(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
): State<MediaController?> {
    // Application context is used to prevent memory leaks
    val appContext = LocalContext.current.applicationContext
    val controllerManager = remember { MediaControllerManager.getInstance(appContext) }

    // Observe the lifecycle to initialize and release the MediaController at the appropriate times.
    // Note: We initialize on ON_START but DO NOT release on ON_DESTROY because during
    // configuration changes (fold/unfold), the activity is destroyed and immediately recreated.
    // The MediaController is a singleton that should persist across config changes.
    // It will be released when the MediaControllerManager singleton is garbage collected
    // or when the app process is killed.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controllerManager.initialize()
                // Don't release on ON_DESTROY - the singleton MediaController should persist
                // across configuration changes. The service manages its own lifecycle.
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return controllerManager.controller
}

@Composable
fun rememberCurrentMetadata(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
): State<MediaMetadata?> {
    val appContext = LocalContext.current.applicationContext
    val manager = remember { MediaControllerManager.getInstance(appContext) }

    // Lifecycle-aware init - don't release on destroy to survive config changes
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> manager.initialize()
                // Don't release on ON_DESTROY - persist across config changes
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Collect the metadata flow once and expose it as a State
    return manager.currentMetadata.collectAsStateWithLifecycle()
}