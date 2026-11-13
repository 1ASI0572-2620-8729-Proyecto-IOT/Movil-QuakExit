package com.quakexit.app

import android.app.Application
import android.content.Context
import com.quakexit.app.data.local.SessionStore
import com.quakexit.app.data.mock.MockDataSource
import com.quakexit.app.data.remote.NetworkModule
import com.quakexit.app.data.remote.RemoteDataSource
import com.quakexit.app.data.repository.QuakRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class QuakExitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}

object ServiceLocator {
    lateinit var store: SessionStore private set
    lateinit var repository: QuakRepository private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages
    fun toast(msg: String) { _messages.tryEmit(msg) }

    fun init(context: Context) {
        store = SessionStore(context.applicationContext)
        repository = QuakRepository(RemoteDataSource(NetworkModule.createApi(store)), MockDataSource(), store)
    }
}
