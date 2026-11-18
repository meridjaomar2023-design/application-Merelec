package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class DriveBackupFile(
    val id: String,
    val name: String,
    val createdTimeFormatted: String,
    val sizeBytes: Long
)

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsResolution(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Error(val message: String) : DriveAuthOutcome
}

object GoogleDriveService {

    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Request OAuth2 authorization for Google Drive (`drive.file` scope) via Google Identity Services.
     */
    suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome =
        suspendCancellableCoroutine { continuation ->
            try {
                val requestedScopes = listOf(Scope(DRIVE_FILE_SCOPE))
                val authRequest = AuthorizationRequest.builder()
                    .setRequestedScopes(requestedScopes)
                    .build()

                Identity.getAuthorizationClient(context)
                    .authorize(authRequest)
                    .addOnSuccessListener { result ->
                        if (!continuation.isActive) return@addOnSuccessListener
                        if (result.hasResolution()) {
                            val pendingIntent = result.pendingIntent
                            if (pendingIntent != null) {
                                continuation.resume(DriveAuthOutcome.NeedsResolution(pendingIntent))
                            } else {
                                continuation.resume(DriveAuthOutcome.Error("Autorisation requise mais l'écran de consentement est indisponible."))
                            }
                        } else {
                            val token = result.accessToken
                            if (!token.isNullOrBlank()) {
                                continuation.resume(DriveAuthOutcome.Authorized(token))
                            } else {
                                continuation.resume(DriveAuthOutcome.Error("Jeton d'accès Google Drive non reçu."))
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        if (!continuation.isActive) return@addOnFailureListener
                        continuation.resume(
                            DriveAuthOutcome.Error(
                                e.localizedMessage ?: "Échec de l'authentification Google Drive."
                            )
                        )
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(
                        DriveAuthOutcome.Error(
                            e.localizedMessage ?: "Erreur lors de l'initialisation Google Drive."
                        )
                    )
                }
            }
        }

    /**
     * Extract the OAuth2 access token after the user completes the consent resolution intent.
     */
    fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> {
        return try {
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("Jeton d'accès non trouvé après autorisation."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a JSON backup file directly to the user's Google Drive using Drive API v3 multipart upload.
     */
    suspend fun uploadBackupFile(
        accessToken: String,
        fileName: String,
        jsonContent: String
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
                put("description", "Sauvegarde automatique Gestion Merelec")
            }.toString()

            val metadataBody = metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val mediaBody = jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadataBody)
                .addPart(mediaBody)
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,createdTime,size")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Erreur Google Drive (${response.code}): ${extractDriveErrorMessage(bodyStr)}")
                    )
                }
                val obj = JSONObject(bodyStr)
                val file = DriveBackupFile(
                    id = obj.optString("id"),
                    name = obj.optString("name", fileName),
                    createdTimeFormatted = formatIsoDate(obj.optString("createdTime")),
                    sizeBytes = obj.optLong("size", jsonContent.toByteArray().size.toLong())
                )
                Result.success(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List existing backup files created by the app in the user's Google Drive (`drive.file` scope).
     */
    suspend fun listBackupFiles(accessToken: String): Result<List<DriveBackupFile>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files" +
                        "?q=mimeType%3D'application/json'%20and%20trashed%3Dfalse" +
                        "&orderBy=createdTime%20desc" +
                        "&pageSize=25" +
                        "&fields=files(id,name,createdTime,size)"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Erreur lors de la récupération des sauvegardes (${response.code}): ${extractDriveErrorMessage(bodyStr)}")
                        )
                    }

                    val root = JSONObject(bodyStr)
                    val filesArray = root.optJSONArray("files") ?: JSONArray()
                    val list = mutableListOf<DriveBackupFile>()
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        list.add(
                            DriveBackupFile(
                                id = f.optString("id"),
                                name = f.optString("name", "Sauvegarde.json"),
                                createdTimeFormatted = formatIsoDate(f.optString("createdTime")),
                                sizeBytes = f.optLong("size", 0L)
                            )
                        )
                    }
                    Result.success(list)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Download the JSON content of a specific backup file from Google Drive.
     */
    suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Impossible de télécharger la sauvegarde (${response.code}): ${extractDriveErrorMessage(bodyStr)}")
                        )
                    }
                    Result.success(bodyStr)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Delete a backup file from Google Drive.
     */
    suspend fun deleteBackupFile(accessToken: String, fileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 204) {
                        val bodyStr = response.body?.string().orEmpty()
                        return@withContext Result.failure(
                            IllegalStateException("Erreur de suppression (${response.code}): ${extractDriveErrorMessage(bodyStr)}")
                        )
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun extractDriveErrorMessage(bodyStr: String): String {
        return try {
            val json = JSONObject(bodyStr)
            json.optJSONObject("error")?.optString("message") ?: bodyStr.take(120)
        } catch (e: Exception) {
            bodyStr.take(120)
        }
    }

    private fun formatIsoDate(isoString: String): String {
        if (isoString.isBlank()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString) ?: return isoString
            DateUtils.formatDateTime(date.time)
        } catch (e: Exception) {
            try {
                val fallbackParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = fallbackParser.parse(isoString) ?: return isoString
                DateUtils.formatDateTime(date.time)
            } catch (ex: Exception) {
                isoString
            }
        }
    }
}
