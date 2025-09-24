package io.github.canvas.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.MapColumn
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
internal data class ShortcutCustomization(
    @PrimaryKey val uid: String,
    val label: String? = null,
    val hidden: Boolean = false,
) : Customization {
    override fun hasDefaultValues(): Boolean = label == null && !hidden
}

@Dao
internal abstract class ShortcutCustomizationDao :
    CustomizationDao<String, ShortcutCustomization>() {
    @Query("SELECT * FROM ShortcutCustomization")
    abstract override fun all(): Flow<Map<@MapColumn("uid") String, ShortcutCustomization>>

    @Query("SELECT * FROM ShortcutCustomization WHERE uid = :uid")
    abstract override suspend fun getFromDb(uid: String): ShortcutCustomization?
    override fun getDefault(uid: String): ShortcutCustomization = ShortcutCustomization(uid)
}
