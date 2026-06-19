package com.vbwd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.session.AuthSession
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.VbwdTheme
import com.vbwd.core.ui.AppShellView
import com.vbwd.core.ui.RootView
import com.vbwd.core.ui.dashboard.DashboardScreen
import com.vbwd.core.ui.login.LoginScreen
import com.vbwd.core.ui.profile.ProfileEditScreen
import com.vbwd.core.ui.settings.SettingsScreen

/**
 * App root (A03): themed via [VbwdTheme], bootstraps the [PluginHost], then
 * drives the `AuthState` shell — [LoginScreen] when signed out, the plugin-host
 * [AppShellView] (Dashboard/Profile/Settings + plugin routes) once authenticated.
 */
@Composable
fun AppRoot(session: AuthSession, pluginHost: PluginHost, themeManager: ThemeManager) {
    var booted by remember { mutableStateOf(false) }
    LaunchedEffect(pluginHost) {
        pluginHost.bootstrap()
        booted = true
    }

    VbwdTheme(themeManager) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (!booted) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            } else {
                RootView(
                    session = session,
                    loginContent = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            LoginScreen(viewModel = hiltViewModel())
                        }
                    },
                    authenticatedContent = {
                        AppShellView(
                            host = pluginHost,
                            session = session,
                            dashboardContent = { DashboardScreen(viewModel = hiltViewModel()) },
                            profileContent = {
                                ProfileEditScreen(
                                    viewModel = hiltViewModel(),
                                    profileSections = pluginHost.components.profileComponents(),
                                )
                            },
                            settingsContent = { SettingsScreen(themeManager = themeManager) },
                        )
                    },
                )
            }
        }
    }
}
