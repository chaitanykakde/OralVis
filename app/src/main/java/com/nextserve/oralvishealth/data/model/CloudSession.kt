package com.nextserve.oralvishealth.data.model

data class CloudSession(
    val sessionId: String,
    val name: String,
    val age: String,
    val timestamp: Long,
    val folderId: String
)
