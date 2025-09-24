package io.github.canvas.data.icons

/** Specifies that a search result should use the icon [drawable] from [iconPack] instead of its default icon */
data class CustomIcon(
    val iconPack: String,
    val drawable: String,
)
