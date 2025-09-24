package io.github.canvas.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity
internal data class WidgetDb(
    @PrimaryKey val id: Int,
    /** The page the widget is on */
    val page: Int,
    /** The vertical position of the widget */
    val position: Int,
    /** The vertical size of the widget, works similar to the compose weight() modifier */
    val verticalWeight: Float,
    /** True if the initial configuration of the widget is currently in progress */
    val configurationPending: Boolean,
    val scrollable: Boolean,
)

@Dao
internal abstract class WidgetDao {
    @Query("SELECT * FROM WidgetDb")
    abstract fun all(): Flow<List<WidgetDb>>

    @Query("SELECT * FROM WidgetDb WHERE id = :widgetId")
    abstract suspend fun get(widgetId: Int): WidgetDb

    @Insert
    abstract suspend fun insert(widget: WidgetDb)

    @Update
    abstract suspend fun update(widget: WidgetDb)

    @Upsert
    abstract suspend fun upsert(widget: WidgetDb)

    @Delete
    abstract suspend fun delete(widget: WidgetDb)

    @Query("DELETE FROM WidgetDb WHERE id = :widgetId")
    abstract suspend fun delete(widgetId: Int)

    @Transaction
    open suspend fun transaction(transaction: suspend () -> Unit): Unit = transaction()
}
