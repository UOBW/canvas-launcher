package io.github.canvas.data

import android.content.ContentResolver
import android.net.Uri
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

abstract class ContentResolverBackedRepository(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) {
    // Register the observer only once
    @OptIn(ExperimentalAtomicApi::class)
    private var observerRegistered = AtomicBoolean(false)

    protected val contentObserverListener: MutableListenerFlow =
        MutableListenerFlow(emitInitial = true)

    /**
     * Registers the content observer to listen to changes to the content resolver
     * Assumes that permission has been granted
     */
    @OptIn(ExperimentalAtomicApi::class)
    protected suspend fun tryRegisterContentObserver() {
        val alreadyRegistered = observerRegistered.exchange(true)
        if (alreadyRegistered) return
        io {
            contentResolver.registerContentObserver(
                uri, /* notifyForDescendants = */ true,
                object : ContentObserverAdapter() {
                    override fun onChange() {
                        contentObserverListener.tryEmit()
                    }
                }
            )
        }
    }
}
