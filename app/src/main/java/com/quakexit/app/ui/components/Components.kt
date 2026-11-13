package com.quakexit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.quakexit.app.ui.theme.QxColors

@Composable
fun QxCard(modifier: Modifier = Modifier, borderColor: Color = QxColors.Border, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = QxColors.Panel),
        border = BorderStroke(1.dp, borderColor),
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(top = 8.dp))

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun QxField(
    value: String, onChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, error: String? = null, password: Boolean = false, singleLine: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = QxColors.Accent, unfocusedBorderColor = QxColors.Border,
            focusedContainerColor = QxColors.PanelAlt, unfocusedContainerColor = QxColors.PanelAlt,
            focusedLabelColor = QxColors.Accent, cursorColor = QxColors.Accent,
        ),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = QxColors.Accent, contentColor = Color(0xFF1A1200)),
    ) {
        icon?.let { Icon(it, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = QxColors.Accent, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.6f) else QxColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun BatteryBar(percent: Int, modifier: Modifier = Modifier) {
    val color = when { percent < 15 -> QxColors.Danger; percent < 40 -> QxColors.Warning; else -> QxColors.Success }
    LinearProgressIndicator(
        progress = { percent.coerceIn(0, 100) / 100f },
        modifier = modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
        color = color, trackColor = QxColors.PanelAlt, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

@Composable
fun EmptyState(text: String) = Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(text, color = QxColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun LoadingBar(loading: Boolean) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = QxColors.Accent, trackColor = QxColors.PanelAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QxTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { onBack?.let { IconButton(onClick = it) { Icon(Icons.Filled.ArrowBack, "Volver") } } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = QxColors.Background, titleContentColor = QxColors.TextPrimary),
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = QxColors.PanelAlt,
        title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("Confirmar", color = QxColors.Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
