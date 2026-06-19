package com.vbwd.plugin.stripe.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PADDING = 4.dp

/** Inline info shown when "Stripe" is the selected method. Port of the iOS view. */
@Composable
fun StripeRedirectSection() {
    Text(
        text = "After confirming your order, you will be redirected to Stripe to complete payment securely.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(PADDING).testTag("checkout_stripe_info"),
    )
}
