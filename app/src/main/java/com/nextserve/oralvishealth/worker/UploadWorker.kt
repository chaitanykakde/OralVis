package com.nextserve.oralvishealth.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.nextserve.oralvishealth.data.database.AppDatabase
import com.nextserve.oralvishealth.data.repository.SessionRepository
import com.nextserve.oralvishealth.service.FirebaseStorageService
import java.io.File

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
    }

    private val sessionRepository: SessionRepository
    private val firebaseStorageService = FirebaseStorageService()

    init {
        val sessionDao = AppDatabase.getDatabase(applicationContext).sessionDao()
        sessionRepository = SessionRepository(sessionDao)
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()

        return try {
            // Check Firebase Authentication
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                android.util.Log.e("UploadWorker", "User not authenticated")
                return Result.failure(workDataOf("error" to "User not authenticated"))
            }

            // Initialize Firebase Storage service
            try {
                firebaseStorageService.initialize()
                android.util.Log.d("UploadWorker", "Firebase Storage initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("UploadWorker", "Failed to initialize Firebase Storage", e)
                return Result.failure(workDataOf("error" to "Failed to initialize storage: ${e.message}"))
            }

            // Get session from database
            val session = sessionRepository.getSessionById(sessionId)
                ?: return Result.failure(workDataOf("error" to "Session not found"))

            // Get session images (using same path as SessionDetailsActivity)
            val sessionDir = File(applicationContext.getExternalFilesDir(null), "Sessions/$sessionId")
            val imageFiles = sessionDir.listFiles { file -> 
                file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
            }?.toList() ?: emptyList()

            if (imageFiles.isEmpty()) {
                return Result.failure(workDataOf("error" to "No images found for session"))
            }

            // Set initial progress
            setProgress(workDataOf(
                KEY_PROGRESS to 0,
                KEY_STATUS to "Starting upload..."
            ))

            // Upload session to Firebase Storage
            val uploadSuccess = firebaseStorageService.uploadSession(
                session = session,
                imageFiles = imageFiles
            ) { progress, status ->
                // Progress tracking - will be updated via logs for now
                android.util.Log.d("UploadWorker", "Upload progress: $progress% - $status")
            }

            if (uploadSuccess) {
                // Update final progress
                setProgress(workDataOf(
                    KEY_PROGRESS to 100,
                    KEY_STATUS to "Upload completed"
                ))
                
                // Update session as uploaded in database
                val updatedSession = session.copy(isUploaded = true)
                sessionRepository.updateSession(updatedSession)
                
                Result.success(workDataOf("message" to "Session uploaded successfully"))
            } else {
                Result.failure(workDataOf("error" to "Upload failed"))
            }

        } catch (e: Exception) {
            android.util.Log.e("UploadWorker", "Upload failed: ${e.message}", e)
            setProgress(workDataOf(
                KEY_PROGRESS to 0,
                KEY_STATUS to "Upload failed: ${e.message}"
            ))
            Result.failure(workDataOf("error" to e.message))
        }
    }
}
