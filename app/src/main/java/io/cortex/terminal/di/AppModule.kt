package io.cortex.terminal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.cortex.terminal.data.api.AIService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        // Mock Interceptor for AI Service
        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.encodedPath

            if (url.endsWith("suggest")) {
                // Simulate network delay
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    // Ignore
                }

                // Return mock response
                val json = """
                    {
                        "command": "tar -xvf archive.tar.gz",
                        "explanation": "This command extracts the archive."
                    }
                """.trimIndent()

                return@Interceptor Response.Builder()
                    .code(200)
                    .message("OK")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .addHeader("content-type", "application/json")
                    .build()
            }

            try {
                chain.proceed(request)
            } catch (e: Exception) {
                // Fallback for demo if network fails
                Response.Builder()
                    .code(500)
                    .message("Network Error")
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(mockInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.cortex.io/v1/") // Placeholder base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAIService(retrofit: Retrofit): AIService {
        return retrofit.create(AIService::class.java)
    }
}
