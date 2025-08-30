package com.nextserve.oralvishealth.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    val sessionId: String,
    val name: String,
    val age: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUploaded: Boolean = false
)
