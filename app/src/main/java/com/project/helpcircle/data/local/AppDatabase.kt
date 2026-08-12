package com.project.helpcircle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.project.helpcircle.data.local.dao.ActiveCommunityDao
import com.project.helpcircle.data.local.dao.AgencyStateDao
import com.project.helpcircle.data.local.dao.ChargeWalletDao
import com.project.helpcircle.data.local.dao.FocusSessionDao
import com.project.helpcircle.data.local.dao.MonitoredAppDao
import com.project.helpcircle.data.local.dao.UserIdentityDao
import com.project.helpcircle.data.local.dao.WeeklyHistoryDao
import com.project.helpcircle.data.local.entity.ActiveCommunityEntity
import com.project.helpcircle.data.local.entity.AgencyStateEntity
import com.project.helpcircle.data.local.entity.ChargeWalletEntity
import com.project.helpcircle.data.local.entity.CrisisEpisodeEntity
import com.project.helpcircle.data.local.entity.FocusSessionEntity
import com.project.helpcircle.data.local.entity.MonitoredAppEntity
import com.project.helpcircle.data.local.entity.UserIdentityEntity
import com.project.helpcircle.data.local.entity.WeeklySummaryEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** The app's single local database, encrypted at rest via SQLCipher's [SupportOpenHelperFactory]. */
@Database(
    entities = [
        FocusSessionEntity::class,
        ChargeWalletEntity::class,
        UserIdentityEntity::class,
        AgencyStateEntity::class,
        ActiveCommunityEntity::class,
        MonitoredAppEntity::class,
        CrisisEpisodeEntity::class,
        WeeklySummaryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun chargeWalletDao(): ChargeWalletDao
    abstract fun userIdentityDao(): UserIdentityDao
    abstract fun agencyStateDao(): AgencyStateDao
    abstract fun activeCommunityDao(): ActiveCommunityDao
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun weeklyHistoryDao(): WeeklyHistoryDao

    companion object {
        private const val DATABASE_NAME = "help_circle.db"

        init {
            // net.zetetic:sqlcipher-android bundles libsqlcipher.so but never loads it itself;
            // without this, SupportOpenHelperFactory's native calls fail with UnsatisfiedLinkError.
            System.loadLibrary("sqlcipher")
        }

        fun build(context: Context, passphrase: ByteArray): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
