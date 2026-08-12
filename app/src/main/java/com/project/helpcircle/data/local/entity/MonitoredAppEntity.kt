package com.project.helpcircle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row for one package on the user's monitored-apps blacklist; a row's mere existence means it's monitored. */
@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey val packageName: String
)
