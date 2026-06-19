package com.vbwd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.session.AuthSession
import com.vbwd.core.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host. `@AndroidEntryPoint` lets the Hilt graph inject into
 * this activity and the ViewModels it hosts. The app-wide [AuthSession] and
 * [PluginHost] singletons are field-injected and handed to `AppRoot`, which
 * bootstraps the plugins and drives the `AuthState` shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var session: AuthSession

    @Inject
    lateinit var pluginHost: PluginHost

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(session = session, pluginHost = pluginHost, themeManager = themeManager)
        }
    }
}
