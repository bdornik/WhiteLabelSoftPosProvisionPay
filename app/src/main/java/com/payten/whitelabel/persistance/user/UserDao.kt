package com.payten.whitelabel.persistance.user

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable

@Dao
interface UserDao {

    @Query("SELECT * FROM Users")
    fun getAllUsers(): LiveData<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User): Completable

    @Query("DELETE FROM Users")
    fun deleteAllUsers()
}