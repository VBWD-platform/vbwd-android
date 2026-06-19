package com.vbwd.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vbwd.core.plugins.Navigator
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.plugins.PluginRoute
import com.vbwd.core.plugins.RouteResolution
import com.vbwd.core.plugins.registries.MenuItem
import com.vbwd.core.session.AuthSession
import com.vbwd.core.session.AuthState
import kotlinx.coroutines.launch

private val DRAWER_ITEM_PADDING = 8.dp

/** Sentinel selections for the built-in (non-plugin) destinations. */
private const val PROFILE_ROUTE = "vbwd:profile"
private const val SETTINGS_ROUTE = "vbwd:settings"

/**
 * The plugin-host app shell. Port of the iOS `AppShellView` + `SideMenu`: a
 * navigation drawer with the built-in Dashboard/Profile/Settings entries plus
 * plugin-contributed menu items, and a content area that resolves the selected
 * route through [Navigator]. The built-in screens are passed as slots so `:core`
 * stays decoupled from how the host constructs their (Hilt) view models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellView(
    host: PluginHost,
    session: AuthSession,
    dashboardContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val selectedRoute by host.selectedRoute.collectAsState()
    val authState by session.state.collectAsState()

    val isAuthenticated = authState is AuthState.Authenticated
    val userPermissions = (authState as? AuthState.Authenticated)?.user?.userPermissions ?: emptyList()

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ShellDrawer(
                menuItems = host.sdk.getMenuItems(),
                selectedRoute = selectedRoute,
                onSelect = { path ->
                    host.select(path)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VBWD") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("☰")
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                ShellContent(
                    host = host,
                    selectedRoute = selectedRoute,
                    isAuthenticated = isAuthenticated,
                    userPermissions = userPermissions,
                    dashboardContent = dashboardContent,
                    profileContent = profileContent,
                    settingsContent = settingsContent,
                )
            }
        }
    }
}

@Composable
private fun ShellDrawer(
    menuItems: List<MenuItem>,
    selectedRoute: String?,
    onSelect: (String?) -> Unit,
) {
    ModalDrawerSheet {
        DrawerEntry("Dashboard", selected = selectedRoute == null) { onSelect(null) }
        DrawerEntry("Profile", selected = selectedRoute == PROFILE_ROUTE) { onSelect(PROFILE_ROUTE) }
        DrawerEntry("Settings", selected = selectedRoute == SETTINGS_ROUTE) { onSelect(SETTINGS_ROUTE) }
        menuItems.forEach { item ->
            DrawerEntry(item.title, selected = selectedRoute == item.routePath) {
                item.action()
                onSelect(item.routePath)
            }
        }
    }
}

@Composable
private fun DrawerEntry(label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = DRAWER_ITEM_PADDING),
    )
}

@Composable
private fun ShellContent(
    host: PluginHost,
    selectedRoute: String?,
    isAuthenticated: Boolean,
    userPermissions: List<String>,
    dashboardContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
) {
    when (selectedRoute) {
        null -> dashboardContent()
        PROFILE_ROUTE -> profileContent()
        SETTINGS_ROUTE -> settingsContent()
        else -> PluginRouteContent(host, selectedRoute, isAuthenticated, userPermissions)
    }
}

@Composable
private fun PluginRouteContent(
    host: PluginHost,
    selectedRoute: String,
    isAuthenticated: Boolean,
    userPermissions: List<String>,
) {
    when (Navigator.resolve(selectedRoute, host.routes, isAuthenticated, userPermissions)) {
        RouteResolution.Allow -> {
            val route = matchingRoute(host.routes, selectedRoute)
            if (route != null) route.content() else CenteredMessage("Page not found.")
        }
        RouteResolution.RedirectToLogin -> CenteredMessage("Sign in to view this page.")
        RouteResolution.Forbidden -> CenteredMessage("You don't have access to this page.")
        RouteResolution.NotFound -> CenteredMessage("Page not found.")
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(DRAWER_ITEM_PADDING)) {
        Text(message)
    }
}

private fun matchingRoute(routes: List<PluginRoute>, path: String): PluginRoute? =
    routes.firstOrNull { it.path == path || (it.matchPrefix && path.startsWith(it.path)) }
