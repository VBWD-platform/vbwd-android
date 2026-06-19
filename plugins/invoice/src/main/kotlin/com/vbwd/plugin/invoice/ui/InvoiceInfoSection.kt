package com.vbwd.plugin.invoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PADDING = 4.dp
private val ROW_SPACING = 6.dp

/** Inline info shown when "Invoice" is the selected method. Port of the iOS view. */
@Composable
fun InvoiceInfoSection() {
    Column(
        modifier = Modifier.padding(PADDING).testTag("checkout_invoice_info"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text(
            text = "An invoice will be sent to your email address. Payment is due within the period " +
                "specified on the invoice.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "You will receive payment instructions by email after checkout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
