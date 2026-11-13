package com.quakexit.app.data.remote

import com.quakexit.app.data.local.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
    data object Forbidden : SessionEvent
}

object SessionEvents {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SessionEvent> = _events
    fun emit(e: SessionEvent) { _events.tryEmit(e) }
}

/** Reescribe host/prefijo según la URL configurable, sin reconstruir Retrofit. */
class BaseUrlInterceptor(private val store: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val rest = req.url.encodedPathSegments.drop(2).joinToString("/")
        val query = req.url.encodedQuery?.let { "?$it" } ?: ""
        val target = (store.baseUrl.trimEnd('/') + "/" + rest + query).toHttpUrl()
        return chain.proceed(req.newBuilder().url(target).build())
    }
}

/** Adjunta Bearer JWT en rutas protegidas; 401 cierra sesión y 403 avisa restricción. */
class AuthInterceptor(private val store: SessionStore) : Interceptor {
    private fun isPublic(path: String) = path.contains("/auth/") || path.endsWith("/system/health")

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val protectedRoute = !isPublic(req.url.encodedPath)
        val token = store.token
        val builder = req.newBuilder().header("Accept", "application/json")
        if (protectedRoute && token != null) builder.header("Authorization", "Bearer $token")
        val response = chain.proceed(builder.build())
        if (protectedRoute) when (response.code) {
            401 -> if (token != null) SessionEvents.emit(SessionEvent.Unauthorized)
            403 -> SessionEvents.emit(SessionEvent.Forbidden)
        }
        return response
    }
}

object NetworkModule {
    fun createApi(store: SessionStore): QuakApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(BaseUrlInterceptor(store))
            .addInterceptor(AuthInterceptor(store))
            .build()
        return Retrofit.Builder()
            .baseUrl("http://placeholder.local/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuakApi::class.java)
    }
}
