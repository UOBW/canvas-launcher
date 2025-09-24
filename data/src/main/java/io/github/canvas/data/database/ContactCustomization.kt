package io.github.canvas.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.MapColumn
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
internal data class ContactCustomization(
    /** [android.provider.ContactsContract.Contacts._ID] */
    @PrimaryKey val uid: Long,
    val label: String? = null,
    val hidden: Boolean = false,
) : Customization {
    override fun hasDefaultValues(): Boolean = label == null && !hidden
}

@Dao
internal abstract class ContactCustomizationDao : CustomizationDao<Long, ContactCustomization>() {
    @Query("SELECT * FROM ContactCustomization")
    abstract override fun all(): Flow<Map<@MapColumn("uid") Long, ContactCustomization>>

    @Query("SELECT * FROM ContactCustomization WHERE uid = :uid")
    abstract override suspend fun getFromDb(uid: Long): ContactCustomization?
    override fun getDefault(uid: Long): ContactCustomization = ContactCustomization(uid)
}
