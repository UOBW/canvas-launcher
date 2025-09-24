package io.github.canvas.data

import android.content.Context
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.room.TypeConverter

sealed interface Tag {
    enum class Builtin : Tag {
        GAME, AUDIO, VIDEO, IMAGE, SOCIAL, NEWS, MAPS, PRODUCTIVITY, ACCESSIBILITY;
    }

    data class Custom(val name: String) : Tag
}


/** A search results that can have its [tags] changes */
interface TaggableSearchResult : SearchResult {
    fun setTagsAsync(tags: Set<Tag>, context: Context)
}

// TODO localize all usages
val Tag.unlocalizedName: String
    get() = when (this) {
        is Tag.Builtin -> this.name.lowercase()
        is Tag.Custom -> this.name
    }

// Tag savers
// Builtin tags are just stored using their english name
private fun Tag.saveToString() = when (this) {
    is Tag.Custom -> name
    is Tag.Builtin -> name.lowercase()
}

private fun String.restoreTag() =
    Tag.Builtin.entries.find { it.name.equals(this, ignoreCase = true) } ?: Tag.Custom(this)

// Room type converter (use # as delimiter as it is not allowed in tag names anyway)
internal class TagSetConverter {
    @TypeConverter
    fun toString(set: Set<Tag>): String = set.joinToString("#") { it.saveToString() }

    @Suppress("unused")
    @TypeConverter
    fun fromString(value: String): Set<Tag> =
        if (value.isEmpty()) emptySet() else value.split('#').map { it.restoreTag() }.toSet()
}

// Compose Saver
object TagSetSaver : Saver<Set<Tag>, List<String>> {
    override fun SaverScope.save(value: Set<Tag>): List<String> = value.map { it.saveToString() }
    override fun restore(value: List<String>): Set<Tag>? = value.map { it.restoreTag() }.toSet()
}
