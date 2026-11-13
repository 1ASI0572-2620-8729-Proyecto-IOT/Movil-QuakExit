package com.quakexit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quakexit.app.R

object QxColors {
    val Background = Color(0xFF0B1220)
    val Panel = Color(0xFF111C2E)
    val PanelAlt = Color(0xFF17243A)
    val Border = Color(0xFF263650)
    val Accent = Color(0xFFF5B942)
    val Danger = Color(0xFFDC2626)
    val Warning = Color(0xFFF59E0B)
    val Success = Color(0xFF16A34A)
    val Info = Color(0xFF2563EB)
    val TextPrimary = Color(0xFFE8EEF8)
    val TextMuted = Color(0xFF8FA3C2)
}

@OptIn(ExperimentalTextApi::class)
val PlusJakartaSans = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.plus_jakarta_sans, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private fun ts(size: Int, weight: FontWeight, line: Int) =
    TextStyle(fontFamily = PlusJakartaSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val QxTypography = Typography(
    headlineLarge = ts(28, FontWeight.ExtraBold, 36),
    headlineMedium = ts(24, FontWeight.Bold, 32),
    titleLarge = ts(20, FontWeight.Bold, 28),
    titleMedium = ts(16, FontWeight.SemiBold, 22),
    titleSmall = ts(14, FontWeight.SemiBold, 20),
    bodyLarge = ts(16, FontWeight.Normal, 24),
    bodyMedium = ts(14, FontWeight.Normal, 20),
    bodySmall = ts(12, FontWeight.Normal, 16),
    labelLarge = ts(14, FontWeight.SemiBold, 20),
    labelMedium = ts(12, FontWeight.Medium, 16),
    labelSmall = ts(11, FontWeight.Medium, 16),
)

private val QxScheme = darkColorScheme(
    primary = QxColors.Accent, onPrimary = Color(0xFF1A1200),
    secondary = QxColors.Info, onSecondary = Color.White,
    tertiary = QxColors.Success,
    error = QxColors.Danger, onError = Color.White,
    background = QxColors.Background, onBackground = QxColors.TextPrimary,
    surface = QxColors.Panel, onSurface = QxColors.TextPrimary,
    surfaceVariant = QxColors.PanelAlt, onSurfaceVariant = QxColors.TextMuted,
    surfaceContainer = QxColors.Panel, surfaceContainerHigh = QxColors.PanelAlt,
    outline = QxColors.Border, outlineVariant = QxColors.Border,
)

private val QxShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun QuakExitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = QxScheme, typography = QxTypography, shapes = QxShapes, content = content)
}
