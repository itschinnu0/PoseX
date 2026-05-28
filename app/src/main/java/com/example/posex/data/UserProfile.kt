package com.example.posex.data

data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val weightKg: Float,
    val heightCm: Float,
    val avatarUri: String? = null,
    val createdAt: Long,
    val lastActiveAt: Long
)

