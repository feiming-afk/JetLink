package com.example.jetlink.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.jetlink.data.dao.ChatDao
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.data.entity.UserEntity

@Database(entities = [UserEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "jetlink_database")
                    .fallbackToDestructiveMigration() // 开发阶段简单处理数据库升级
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
