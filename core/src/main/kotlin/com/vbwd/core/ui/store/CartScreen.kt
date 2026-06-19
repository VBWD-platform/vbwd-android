package com.vbwd.core.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.cart.Cart

private val PADDING = 16.dp
private const val FULL_WEIGHT = 1f

/** Cart contents + checkout entry. Port of the iOS `CartView`. */
@Composable
fun CartScreen(cart: Cart, onCheckout: () -> Unit) {
    val items by cart.items.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(PADDING).testTag("cart_screen")) {
        if (items.isEmpty()) {
            Text("Your cart is empty.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(FULL_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(PADDING),
            ) {
                items(items, key = { it.id }) { item ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(FULL_WEIGHT)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text("${item.currency} ${item.price} × ${item.quantity}")
                        }
                        TextButton(onClick = { cart.remove(item.id) }) { Text("Remove") }
                    }
                }
            }
            Text("Total: ${cart.total}", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().testTag("cart_checkout")) {
                Text("Checkout")
            }
        }
    }
}
