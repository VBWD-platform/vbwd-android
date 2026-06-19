package com.vbwd.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.theme.LocalAppTheme

private val SCREEN_PADDING = 16.dp
private val SECTION_SPACING = 16.dp
private val AVATAR_SIZE = 48.dp
private val CARD_PADDING = 16.dp

/**
 * Generic user dashboard. Port of the iOS `DashboardView`: profile header,
 * permission-gated token/invoice cards, then the plugin `Dashboard*` widget grid
 * (the extension seam). The full billing card UI lands with A04.
 */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        ProfileHeader(viewModel)

        if (viewModel.showTokenCard) {
            DashboardCard(title = "Tokens", value = state.tokenBalance.toString())
        }
        if (viewModel.showInvoicesCard) {
            DashboardCard(title = "Invoices", value = "${state.recentInvoices.size} recent")
        }

        val widgets = viewModel.pluginWidgets
        if (widgets.isEmpty()) {
            Text("No widgets yet.", modifier = Modifier.testTag("dashboard_empty"))
        } else {
            DashboardWidgetGrid(widgets)
        }
    }
}

@Composable
private fun ProfileHeader(viewModel: DashboardViewModel) {
    val theme = LocalAppTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(theme.avatarBackground),
        ) {
            Text(viewModel.userInitials)
        }
        Column {
            Text(viewModel.userName, style = MaterialTheme.typography.titleMedium)
            Text(viewModel.userEmail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardCard(title: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(CARD_PADDING)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
