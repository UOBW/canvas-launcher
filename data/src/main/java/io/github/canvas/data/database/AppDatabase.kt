package io.github.canvas.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.canvas.data.TagSetConverter

@Database(
    version = 8,
    entities = [
        WidgetDb::class,
        ActivityCustomization::class,
        ShortcutCustomization::class,
        ContactCustomization::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8)
    ]
)
@TypeConverters(TagSetConverter::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun getWidgetDao(): WidgetDao
    abstract fun getActivityCustomizationDao(): ActivityCustomizationDao
    abstract fun getShortcutCustomizationDao(): ShortcutCustomizationDao
    abstract fun getContactCustomizationDao(): ContactCustomizationDao
}
