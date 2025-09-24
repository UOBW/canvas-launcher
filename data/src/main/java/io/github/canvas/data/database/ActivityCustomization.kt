package io.github.canvas.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.MapColumn
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import io.github.canvas.data.Tag
import io.github.canvas.data.icons.CustomIcon
import kotlinx.coroutines.flow.Flow

@Suppress("PropertyName")
@Entity
internal data class ActivityCustomization(
    @PrimaryKey val uid: String,
    val label: String? = null,
    val hidden: Boolean = false,
    @ColumnInfo(defaultValue = "false")
    val starred: Boolean = false,
    // Store the custom icon in two columns, either both or none are null
    val customIcon_iconPack: String? = null,
    val customIcon_drawable: String? = null,
    /** null - use default tag, empty - no tags (default tag removed) */
    val tags: Set<Tag>? = emptySet(),
    @ColumnInfo(defaultValue = "0") // Idk why this is required, but it won't compile otherwise
    val timesOpened: Int = 0,
) : Customization {
    override fun hasDefaultValues(): Boolean =
        label == null && !hidden && !starred && customIcon_iconPack == null && tags == null
                && timesOpened == 0

    val customIcon
        get() = if (customIcon_iconPack != null) {
            CustomIcon(customIcon_iconPack, customIcon_drawable!!)
        } else null
}

@Dao
internal abstract class ActivityCustomizationDao :
    CustomizationDao<String, ActivityCustomization>() {
    @Query("SELECT * FROM ActivityCustomization")
    abstract override fun all(): Flow<Map<@MapColumn("uid") String, ActivityCustomization>>

    @Query("SELECT * FROM ActivityCustomization")
    protected abstract fun rowsSnapshot(): List<ActivityCustomization>

    @Query("SELECT * FROM ActivityCustomization WHERE uid = :uid")
    abstract override suspend fun getFromDb(uid: String): ActivityCustomization?
    override fun getDefault(uid: String): ActivityCustomization = ActivityCustomization(uid)

    @Transaction
    open suspend fun deleteUsageData() {
        for (row in rowsSnapshot()) {
            if (row.timesOpened != 0) update(row.copy(timesOpened = 0))
        }
    }
}
