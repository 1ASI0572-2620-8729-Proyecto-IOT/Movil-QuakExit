package com.quakexit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import com.quakexit.app.data.remote.SessionEvent
import com.quakexit.app.data.remote.SessionEvents
import com.quakexit.app.ui.navigation.MainShell
import com.quakexit.app.ui.screens.AuthScreen
import com.quakexit.app.ui.theme.QuakExitTheme
import com.quakexit.app.ui.theme.QxColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuakExitTheme {
                val user by ServiceLocator.store.user.collectAsState()
                val simulated by ServiceLocator.repository.simulated.collectAsState()
                val snackbar = remember { SnackbarHostState() }

                LaunchedEffect(Unit) { ServiceLocator.messages.collect { snackbar.showSnackbar(it) } }
                LaunchedEffect(Unit) {
                    SessionEvents.events.collect {
                        when (it) {
                            SessionEvent.Unauthorized -> {
                                ServiceLocator.store.clearSession()
                                snackbar.showSnackbar("Tu sesión expiró. Inicia sesión nuevamente.")
                            }
                            SessionEvent.Forbidden -> snackbar.showSnackbar("Acción restringida por tu rol o plan.")
                        }
                    }
                }

                Box(Modifier.fillMaxSize().background(QxColors.Background)) {
                    val u = user
                    if (u == null) {
                        AuthScreen()
                        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
                    } else {
                        key(u.id, u.role) {
                            MainShell(u, simulated, onLogout = {}, snackbar = snackbar)
                        }
                    }
                }
            }
        }
    }
}
