package com.notivas.data.remote

import android.util.Log
import com.notivas.data.local.prefs.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        val universityUrl = runBlocking { preferencesManager.universityUrl.first() }
        Log.d("UrlInterceptor", "Intercepting request to: ${request.url}, preferred host: $universityUrl")
        
        if (!universityUrl.isNullOrBlank()) {
            try {
                val cleanHost = universityUrl
                    .replace("https://", "")
                    .replace("http://", "")
                    .split("/")[0]
                
                val newUrl = request.url.newBuilder()
                    .scheme("https")
                    .host(cleanHost)
                    .build()
                request = request.newBuilder().url(newUrl).build()
                Log.d("UrlInterceptor", "New URL: ${request.url}")
            } catch (e: Exception) {
                Log.e("UrlInterceptor", "Error building new URL", e)
            }
        }
        
        return chain.proceed(request)
    }
}
