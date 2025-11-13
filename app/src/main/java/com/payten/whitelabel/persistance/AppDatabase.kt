package com.payten.whitelabel.persistance

import androidx.room.Database
import androidx.room.RoomDatabase
import com.payten.whitelabel.persistance.user.User
import com.payten.whitelabel.persistance.user.UserDao

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}