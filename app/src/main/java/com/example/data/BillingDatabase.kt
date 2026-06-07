package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BillingRecord::class], version = 3, exportSchema = false)
abstract class BillingDatabase : RoomDatabase() {
    abstract val billingDao: BillingDao

    companion object {
        @Volatile
        private var INSTANCE: BillingDatabase? = null

        fun getDatabase(context: Context): BillingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillingDatabase::class.java,
                    "billing_auditor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
