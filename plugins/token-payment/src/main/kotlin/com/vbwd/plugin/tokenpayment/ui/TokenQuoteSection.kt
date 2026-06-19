package com.vbwd.plugin.tokenpayment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.ui.checkout.LocalCheckoutInfo
import com.vbwd.plugin.tokenpayment.TokenPaymentService
import com.vbwd.plugin.tokenpayment.TokenQuote

private val PADDING = 8.dp
private val ROW_SPACING = 8.dp

/**
 * Checkout detail section shown when "Token Balance" is the selected method.
 * Reads the order amount from [LocalCheckoutInfo] and quotes the token cost.
 * Port of the iOS `TokenQuoteSection`.
 */
@Composable
fun TokenQuoteSection(api: ApiClient) {
    val info = LocalCheckoutInfo.current
    val quote by produceState<TokenQuote?>(initialValue = null, info) {
        value = if (info.amount > 0) {
            runCatching { TokenPaymentService(api).fetchQuote(info.amount, info.currency) }.getOrNull()
        } else {
            null
        }
    }

    Column(
        modifier = Modifier.padding(PADDING).testTag("checkout_token_quote"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        val current = quote
        if (current?.available == true) {
            Text("Pay with tokens", style = MaterialTheme.typography.titleSmall)
            Text("Token balance now: ${formatTokens(current.balance)}")
            Text("Tokens to pay: ${formatTokens(current.tokensNeeded)}")
            Text("Balance after payment: ${formatTokens(current.balanceAfter)}")
            if (current.sufficient == false) {
                Text("Not enough tokens to pay this order.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatTokens(value: Double?): String {
    if (value == null) return "—"
    return if (value == value.toLong().toDouble()) "${value.toLong()} tokens" else "$value tokens"
}
