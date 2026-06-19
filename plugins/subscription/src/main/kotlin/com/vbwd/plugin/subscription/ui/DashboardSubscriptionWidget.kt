package com.vbwd.plugin.subscription.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService

private val PADDING = 12.dp

/**
 * `Dashboard*` widget for the subscription plugin. Port of the iOS
 * `DashboardSubscriptionWidget`: shows the active plan + status, or a prompt.
 */
@Composable
fun DashboardSubscriptionWidget(service: SubscriptionService) {
    val subscription by produceState<Subscription?>(initialValue = null, service) {
        value = runCatching { service.fetchActiveSub() }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().padding(PADDING).testTag("dashboard_subscription_widget")) {
        Text("Subscription", style = MaterialTheme.typography.labelMedium)
        val current = subscription
        if (current == null) {
            Text("No active subscription")
        } else {
            Text(current.plan?.name ?: "Active", style = MaterialTheme.typography.titleSmall)
            Text(current.statusLabel)
        }
    }
}
