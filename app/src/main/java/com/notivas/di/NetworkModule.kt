package com.notivas.di

import com.notivas.data.remote.CanvasApiService
import com.notivas.data.remote.UrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(urlInterceptor: UrlInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(urlInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // Default Retrofit instance. Note: Base URL will be replaced in the Repository if needed
    // or we can use a dynamic approach.
    @Provides
    @Singleton
    fun provideCanvasApiService(okHttpClient: OkHttpClient): CanvasApiService {
        return Retrofit.Builder()
            .baseUrl("https://canvas.instructure.com/") // Placeholder
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CanvasApiService::class.java)
    }
}
