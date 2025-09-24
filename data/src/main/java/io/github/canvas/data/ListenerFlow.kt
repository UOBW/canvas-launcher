package io.github.canvas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** A listener flow is a flow that does not carry any data but only notifies its subscribers */
typealias ListenerFlow = Flow<Unit>
typealias MutableListenerFlow = MutableSharedFlow<Unit>

/** @param emitInitial if true, emits once every time collection is started */
fun MutableListenerFlow(emitInitial: Boolean): MutableListenerFlow =
    MutableSharedFlow<Unit>(
        replay = if (emitInitial) 1 else 0, // replay one notification to new subscribers
        extraBufferCapacity = if (emitInitial) 0 else 1 // without replay, extra buffering is required for tryEmit()
    ).apply { if (emitInitial) tryEmit() }

/** @see MutableSharedFlow.tryEmit */
fun MutableListenerFlow.tryEmit(): Boolean = tryEmit(Unit)

/** @see MutableSharedFlow.emit */
suspend fun MutableListenerFlow.emit(): Unit = emit(Unit)
