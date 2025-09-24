package io.github.canvas.ui

import io.github.canvas.data.ListenerFlow
import io.github.canvas.data.MutableListenerFlow

/** Based on [androidx.compose.foundation.interaction.InteractionSource], but with fewer features */
typealias SimpleInteractionSource = ListenerFlow
/** Based on [androidx.compose.foundation.interaction.MutableInteractionSource], but with fewer features */
typealias MutableSimpleInteractionSource = MutableListenerFlow

fun MutableSimpleInteractionSource(): MutableSimpleInteractionSource =
    MutableListenerFlow(emitInitial = false)
