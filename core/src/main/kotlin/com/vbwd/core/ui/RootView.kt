package com.vbwd.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.session.AuthSession

/**
 * App entry view. Auth guard (port of the iOS `RootView` / the web router):
 * the routing decision is delegated to [RootRouter], and the authenticated
 * branch is a slot so `:core` stays free of the host app's shell — the real
 * dashboard/plugin host is supplied by `:app` (A01.4) and grows in A02.
 *
 * Restores the persisted session once on first composition (web `initAuth`).
 */
@Composable
fun RootView(
    session: AuthSession,
    loginContent: @Composable () -> Unit,
    authenticatedContent: @Composable (AuthUser) -> Unit,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { DefaultLoading() },
) {
    val state by session.state.collectAsState()

    LaunchedEffect(session) { session.start() }

    Box(modifier = modifier.fillMaxSize()) {
        when (val route = RootRouter.route(state)) {
            RootRoute.Login -> loginContent()
            RootRoute.Loading -> loadingContent()
            is RootRoute.Authenticated -> authenticatedContent(route.user)
        }
    }
}

@Composable
private fun DefaultLoading() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}
