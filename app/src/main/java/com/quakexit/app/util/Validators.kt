package com.quakexit.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Validators {
    /** Mínimo 8 caracteres, una mayúscula y un número. */
    fun password(p: String): String? = when {
        p.length < 8 -> "Mínimo 8 caracteres"
        p.none { it.isUpperCase() } -> "Debe incluir una mayúscula"
        p.none { it.isDigit() } -> "Debe incluir un número"
        else -> null
    }
    fun email(e: String): String? = if (android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) null else "Correo inválido"
    fun mac(m: String): String? = if (Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$").matches(m)) null else "Formato AA:BB:CC:DD:EE:FF"
    fun phone(p: String): String? = if (Regex("^\\+?[0-9]{7,15}$").matches(p)) null else "Teléfono inválido"
    fun notBlank(v: String, label: String): String? = if (v.isBlank()) "$label es obligatorio" else null
}

private val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val dateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
fun Long.fmt(): String = dateTime.format(Date(this))
fun Long.fmtDate(): String = dateOnly.format(Date(this))
fun parseDate(s: String): Long? = runCatching { dateOnly.parse(s)?.time }.getOrNull()
