/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is based on the LineageOS Contacts app contact icon generation, available at
 * https://github.com/LineageOS/android_packages_apps_Contacts/blob/lineage-21.0/src/com/android/contacts/lettertiles/LetterTileDrawable.java
 * Changes include support for adaptive icons and non-latin letters in the icon.
 */

package io.github.canvas.data.contacts

import android.content.res.Resources
import android.icu.text.BreakIterator
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color.Companion.White
import io.github.canvas.data.R
import io.github.canvas.data.icons.AdaptiveIcon
import io.github.canvas.data.icons.ColorBackground
import io.github.canvas.data.icons.ComposePainter
import io.github.canvas.data.icons.LetterPainter
import io.github.canvas.data.log
import io.github.canvas.data.toComposeColor
import kotlin.math.abs

internal fun generateContactIcon(
    contactName: String, lookupKey: String, res: Resources,
): AdaptiveIcon {
    if (!::cache.isInitialized) cache = ResourceCache(res)

    return AdaptiveIcon(
        foreground = ComposePainter(LetterPainter(contactName.firstGrapheme(), White)),
        background = ColorBackground(pickColor(lookupKey).toComposeColor()),
        monochrome = null
    )
}

/** Returns a deterministic color based on the provided contact lookup key */
@ColorInt
private fun pickColor(lookupKey: String): Int {
    if (lookupKey.isEmpty()) {
        return cache.defaultColor
    }
    // String.hashCode() implementation is not supposed to change across java versions, so
    // this should guarantee the same lookup key always maps to the same color.
    @ColorInt val color = abs(lookupKey.hashCode()) % cache.colors.length()
    return try {
        cache.colors.getColor(color, cache.defaultColor)
    } catch (e: UnsupportedOperationException) {
        log.e("Failed to load background color for contact icon $lookupKey", e)
        cache.defaultColor
    }
}

/** Returns the first grapheme (unicode character) of the string */
private fun String.firstGrapheme(): String {
    if (isEmpty()) return ""
    // Use BreakIterator to support Unicode grapheme points (i.e. emojis, other scripts, …)
    val iterator = BreakIterator.getCharacterInstance().apply { setText(this@firstGrapheme) }
    return this.take(n = iterator.next())
}

private lateinit var cache: ResourceCache

private class ResourceCache(res: Resources) {
    /** Letter tile */
    val colors = res.obtainTypedArray(R.array.letter_tile_colors)
    val defaultColor = res.getColor(R.color.letter_tile_default_color, null)
}
