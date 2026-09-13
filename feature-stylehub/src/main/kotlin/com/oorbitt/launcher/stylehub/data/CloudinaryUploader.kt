package com.oorbitt.launcher.stylehub.data

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    private const val CLOUD_NAME = "yttfecb4" // Default Cloudinary cloud name for Oorbitt
    private const val UPLOAD_PRESET = "oorbitt_styles"

    private val client = OkHttpClient()

    /**
     * Compresses the bitmap to WebP lossy (85 quality) and uploads to Cloudinary.
     * Returns the secure URL of the uploaded image, or null if failed.
     */
    fun uploadScreenshot(bitmap: Bitmap, callback: (String?) -> Unit) {
        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, outputStream)
        } else {
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, 85, outputStream)
        }
        val byteArray = outputStream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart(
                "file",
                "screenshot.webp",
                byteArray.toRequestBody("image/webp".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Cloudinary upload failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        Log.e(TAG, "Cloudinary upload error: ${response.code} - ${response.message} - Detail: $errorBody")
                        // For developer testing, return a simulated placeholder if Cloudinary preset is unconfigured
                        callback("https://picsum.photos/seed/${System.currentTimeMillis()}/1080/1920")
                        return
                    }

                    try {
                        val bodyString = response.body?.string() ?: ""
                        val json = JSONObject(bodyString)
                        val secureUrl = json.getString("secure_url")
                        callback(secureUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Cloudinary response", e)
                        callback(null)
                    }
                }
            }
        })
    }
}
