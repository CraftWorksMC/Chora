package com.craftworks.music.managers

import android.content.Context
import androidx.core.content.edit
import com.craftworks.music.migrations.*
import io.ktor.util.reflect.instanceOf

object MigrationManager {
    private const val MIGRATION_VERSION = "version"
    private val Migrations = listOf(
        ProvidersRefactorMigration::class,
    )
    fun init(context: Context) {
        val migrationStatus = context.getSharedPreferences("MigrationStatus", Context.MODE_PRIVATE)
        var version = migrationStatus.getInt(MIGRATION_VERSION, -1)
        if (version == -1) {
            // Check if a version before migration has been used
            if (
                context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE).contains("local_folders") ||
                context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE).contains("navidrome_servers")
            ) {
                version = 0
            }
        }

        // Migrate if needed
        if (version >= 0 && version < Migrations.size) {
            for (i in version..<Migrations.size) {
                Migrations[i].java.getDeclaredConstructor().newInstance().up(context);
            }
        }

        // Update migration version
        migrationStatus.edit {
            putInt(MIGRATION_VERSION, Migrations.size)
        }
    }
}