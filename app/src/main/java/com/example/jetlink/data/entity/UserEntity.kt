package com.example.jetlink.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val userName: String,
    val avatarUrl: String? = null
)
