package com.example.monitorapp.network

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

object NetworkClient {

    suspend inline fun <reified T> get(url: String, headers: Map<String, String> = emptyMap()): T? {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val request = requestBuilder.build()
        val response = OkHttpClient().newCall(request).execute()
        val body = response.body?.string()
        return if (response.isSuccessful && body != null) Gson().fromJson(body, T::class.java) else null
    }

    suspend inline fun <reified T> post(url: String, bodyObj: Any, headers: Map<String, String> = emptyMap()): T? {
        val json = Gson().toJson(bodyObj)
        val requestBody = RequestBody.create("application/json".toMediaType(), json)
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val request = requestBuilder.build()
        val response = OkHttpClient().newCall(request).execute()
        val body = response.body?.string()
        return if (response.isSuccessful && body != null) Gson().fromJson(body, T::class.java) else null
    }

    fun getRaw(url: String, headers: Map<String, String> = emptyMap()): String? {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val request = requestBuilder.build()
        OkHttpClient().newCall(request).execute().use { response ->
            return if (response.isSuccessful) response.body?.string() else null
        }
    }
} 