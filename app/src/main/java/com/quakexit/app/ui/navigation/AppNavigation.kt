package com.quakexit.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quakexit.app.ServiceLocator
import com.quakexit.app.data.model.Capabilities
import com.quakexit.app.data.model.Roles
import com.quakexit.app.data.model.User
import com.quakexit.app.ui.components.QxCard
import com.quakexit.app.ui.components.QxTopBar
import com.quakexit.app.ui.components.StatusChip
import com.quakexit.app.ui.screens.*
import com.quakexit.app.ui.theme.QxColors

enum class Dest(val route: String, val title: String, val icon: ImageVector) {
    Dashboard("dashboard", "Panel", Icons.Filled.Dashboard),
    Devices("devices", "Equipos", Icons.Filled.Sensors),
    Home("home", "Hogar", Icons.Filled.Home),
    Security("security", "Seguridad", Icons.Filled.Shield),
    More("more", "Más", Icons.Filled.Menu),
    Notifications("notifications", "Notificaciones", Icons.Filled.Notifications),
    Reports("reports", "Reportes", Icons.Filled.Assessment),
    Maintenance("maintenance", "Mantenimiento", Icons.Filled.Build),
    Settings("settings", "Ajustes", Icons.Filled.Settings),
    Buildings("buildings", "Edificios y unidades", Icons.Filled.Apartment),
    Plans("plans", "Planes y suscripción", Icons.Filled.Payments),
    System("system", "Auditoría y salud", Icons.Filled.MonitorHeart),
}

private val tabs = listOf(Dest.Dashboard, Dest.Devices, Dest.Home, Dest.Security, Dest.More)

/** Entradas del menú "Más" según rol y capacidades del plan. */
fun moreEntries(user: User): List<Dest> = buildList {
    add(Dest.Notifications); add(Dest.Reports); add(Dest.Maintenance)
    if (user.role == Roles.ADMIN || user.role == Roles.OWNER) add(Dest.Plans)
    if (user.role == Roles.ADMIN || Capabilities.BUILDINGS in user.capabilities) add(Dest.Buildings)
    if (user.role == Roles.ADMIN || Capabilities.SYSTEM in user.capabilities) add(Dest.System)
    add(Dest.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(user: User, simulated: Boolean, onLogout: () -> Unit, snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val dest = Dest.entries.firstOrNull { d -> current?.hierarchy?.any { it.route == d.route } == true } ?: Dest.Dashboard
    val isTab = dest in tabs

    Scaffold(
        containerColor = QxColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            QxTopBar(dest.title, onBack = if (isTab) null else ({ nav.popBackStack() }), actions = {
                if (simulated) { StatusChip("SIMULADO", QxColors.Warning); Spacer(Modifier.width(12.dp)) }
            })
        },
        bottomBar = {
            if (isTab) NavigationBar(containerColor = QxColors.Panel) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = dest == t,
                        onClick = { nav.navigate(t.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(t.icon, t.title) }, label = { Text(t.title, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = QxColors.Background, indicatorColor = QxColors.Accent, unselectedIconColor = QxColors.TextMuted, selectedTextColor = QxColors.Accent, unselectedTextColor = QxColors.TextMuted),
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Dest.Dashboard.route, modifier = Modifier.padding(pad)) {
            composable(Dest.Dashboard.route) { DashboardScreen() }
            composable(Dest.Devices.route) { DevicesScreen() }
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Security.route) { SecurityScreen() }
            composable(Dest.More.route) { MoreMenu(user) { nav.navigate(it.route) } }
            composable(Dest.Notifications.route) { NotificationsScreen() }
            composable(Dest.Reports.route) { ReportsScreen() }
            composable(Dest.Maintenance.route) { MaintenanceScreen() }
            composable(Dest.Settings.route) { SettingsScreen(onLogout) }
            // Rutas administrativas: solo se registran si el usuario tiene acceso.
            if (Dest.Plans in moreEntries(user)) composable(Dest.Plans.route) { PlansScreen() }
            if (Dest.Buildings in moreEntries(user)) composable(Dest.Buildings.route) { BuildingsScreen() }
            if (Dest.System in moreEntries(user)) composable(Dest.System.route) { SystemScreen() }
        }
    }
}

@Composable
private fun MoreMenu(user: User, onOpen: (Dest) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            QxCard {
                Text(user.name, style = MaterialTheme.typography.titleLarge)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(Roles.label(user.role), QxColors.Info)
                    StatusChip("Plan ${user.plan}", QxColors.Accent)
                }
            }
        }
        items(moreEntries(user)) { d ->
            QxCard(Modifier.clickable { onOpen(d) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(d.icon, null, tint = QxColors.Accent)
                    Text(d.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = QxColors.TextMuted)
                }
            }
        }
    }
}
