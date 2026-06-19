package com.vbwd.core.ui.checkout

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.networking.ApiError
import com.vbwd.core.plugins.PaymentAction
import com.vbwd.core.plugins.registries.ComponentRegistry
import com.vbwd.core.store.CheckoutInvoice
import com.vbwd.core.store.CheckoutResult
import com.vbwd.core.testutil.FakeApiClient
import com.vbwd.core.testutil.FakeCheckoutSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheckoutViewModelTest {
    private val item = CartItem(type = "token_bundle", id = "b1", name = "Starter", price = 10.0)
    private val context = CheckoutContext(source = "token_bundle", isCart = false)

    private fun registryWith(source: FakeCheckoutSource) =
        CheckoutSourceRegistry().apply { register(source) }

    @Test
    fun `loadForContext loads the matching source line items`() = runTest {
        val source = FakeCheckoutSource("token_bundle", items = listOf(item))
        val vm = CheckoutViewModel(FakeApiClient(), context, Cart(), registryWith(source))
        vm.loadForContext()
        assertEquals(listOf("b1"), vm.uiState.value.lineItems.map { it.id })
        assertTrue(source.loaded)
    }

    @Test
    fun `submit with no payment handler goes straight to confirmation and clears the cart`() = runTest {
        val cart = Cart().apply { add(item) }
        val source = FakeCheckoutSource(
            "token_bundle",
            items = listOf(item),
            submitResult = CheckoutResult(invoice = CheckoutInvoice(id = "inv1")),
        )
        val vm = CheckoutViewModel(FakeApiClient(), context, cart, registryWith(source))
        vm.loadForContext()
        vm.selectMethod("invoice")

        vm.submit()

        assertInstanceOf(CheckoutPhase.Confirmation::class.java, vm.uiState.value.phase)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun `submit routes an open-url payment action to the processing phase`() = runTest {
        val source = FakeCheckoutSource(
            "token_bundle",
            items = listOf(item),
            submitResult = CheckoutResult(invoice = CheckoutInvoice(id = "inv1")),
        )
        val components = ComponentRegistry().apply {
            addPaymentAction("stripe") { PaymentAction.OpenUrl("https://pay.example/x") }
        }
        val vm = CheckoutViewModel(FakeApiClient(), context, Cart(), registryWith(source), components = components)
        vm.loadForContext()
        vm.selectMethod("stripe")

        vm.submit()

        val phase = vm.uiState.value.phase
        assertInstanceOf(CheckoutPhase.ProcessingPayment::class.java, phase)
        assertEquals("https://pay.example/x", (phase as CheckoutPhase.ProcessingPayment).url)
    }

    @Test
    fun `a failing submit surfaces the error and stays on the form`() = runTest {
        val source = FakeCheckoutSource(
            "token_bundle",
            items = listOf(item),
            submitError = ApiError.Http(400, "declined"),
        )
        val vm = CheckoutViewModel(FakeApiClient(), context, Cart(), registryWith(source))
        vm.loadForContext()
        vm.selectMethod("invoice")

        vm.submit()

        assertEquals("declined", vm.uiState.value.errorMessage)
        assertInstanceOf(CheckoutPhase.Form::class.java, vm.uiState.value.phase)
    }
}
