package io.github.canvas.data.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

internal interface Customization {
    fun hasDefaultValues(): Boolean
}

/** An attempt to factor out some of the common database logic */
internal sealed class CustomizationDao<K, V : Customization> {
    /** Returns a map of uids to customizations */
    abstract fun all(): Flow<Map<K, V>>

    // For some reason, Room doesn't allow this method to be abstract
    protected open fun getDefault(uid: K): V = error("getDefault() not overridden")
    protected abstract suspend fun getFromDb(uid: K): V?
    suspend fun get(uid: K): V = getFromDb(uid) ?: getDefault(uid)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun set(customization: V)

    @Delete
    protected abstract suspend fun delete(customization: V)

    suspend fun update(customization: V) {
        when {
            customization.hasDefaultValues() -> delete(customization)
            else -> set(customization)
        }
    }
}
