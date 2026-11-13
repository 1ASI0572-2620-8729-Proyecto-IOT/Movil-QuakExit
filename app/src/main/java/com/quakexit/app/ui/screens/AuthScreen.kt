package com.quakexit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quakexit.app.R
import com.quakexit.app.data.model.Roles
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.util.Validators
import com.quakexit.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(vm: AuthViewModel = viewModel()) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Roles.OWNER) }
    var expanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val loading by vm.loading.collectAsState()

    val nameErr = if (submitted && register) Validators.notBlank(name, "Nombre") else null
    val emailErr = if (submitted) Validators.email(email.trim()) else null
    val passErr = if (submitted && (register || password.isBlank())) Validators.password(password) else null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.logo_quakexit), "QuakExit", Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit)
        Text("Monitoreo sísmico y evacuación asistida", color = QxColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        QxCard {
            Text(if (register) "Crear cuenta" else "Iniciar sesión", style = MaterialTheme.typography.titleLarge)
            if (register) QxField(name, { name = it }, "Nombre", error = nameErr)
            QxField(email, { email = it }, "Correo", error = emailErr, keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            QxField(password, { password = it }, "Contraseña", error = passErr, password = true)
            if (register) Text("Mín. 8 caracteres, una mayúscula y un número", color = QxColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(
                    value = Roles.label(role), onValueChange = {}, readOnly = true, label = { Text("Rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    Roles.all.forEach { r -> DropdownMenuItem(text = { Text(Roles.label(r)) }, onClick = { role = r; expanded = false }) }
                }
            }
            LoadingBar(loading)
            PrimaryButton(
                if (register) "Registrarme" else "Entrar", enabled = !loading, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    submitted = true
                    val valid = Validators.email(email.trim()) == null && Validators.password(password) == null &&
                        (!register || name.isNotBlank())
                    if (valid) { if (register) vm.register(name, email, password, role) else vm.login(email, password, role) }
                },
            )
            TextButton(onClick = { register = !register; submitted = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (register) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate", color = QxColors.Accent)
            }
        }
    }
}

