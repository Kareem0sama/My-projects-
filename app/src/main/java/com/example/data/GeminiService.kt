package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiRepository {
    suspend fun getGeminiResponse(prompt: String, systemInstruction: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "عذراً، لم يتم ضبط مفتاح AI Studio API Key بشكل صحيح في الإعدادات. يرجى تزويد التطبيق بالمفتاح عبر لوحة الأسرار (Secrets)."
        }

        val contents = listOf(Content(parts = listOf(Part(text = prompt))))
        val instruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        val request = GenerateContentRequest(contents = contents, systemInstruction = instruction)

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "خطأ: لم يتم تلقي رد صحيح من الذكاء الاصطناعي."
        } catch (e: Exception) {
            "فشل الاتصال بمحرك الذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun analyzePhoto(prompt: String, base64Image: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "عذراً، لم يتم ضبط مفتاح AI Studio API Key بشكل صحيح في الإعدادات. يرجى تزويد التطبيق بالمفتاح عبر لوحة الأسرار (Secrets)."
        }

        val parts = listOf(
            Part(text = prompt),
            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
        )
        val contents = listOf(Content(parts = parts))
        val request = GenerateContentRequest(contents = contents)

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "لم نتمكن من تحليل تلك الصورة، يرجى المحاولة بصورة أوضح."
        } catch (e: Exception) {
            "خطأ أثناء معالجة الصورة بالذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
        }
    }
}
