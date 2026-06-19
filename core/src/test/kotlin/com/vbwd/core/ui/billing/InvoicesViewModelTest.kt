package com.vbwd.core.ui.billing

import com.vbwd.core.domain.Invoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvoicesViewModelTest {
    private val invoices = listOf(
        Invoice(id = "1", invoiceNumber = "2026-001", status = "paid"),
        Invoice(id = "2", invoiceNumber = "2026-002", status = "pending"),
    )

    @Test
    fun `filteredInvoices returns all when the query is empty`() {
        val state = InvoicesViewModel.UiState(invoices = invoices, searchText = "")
        assertEquals(2, state.filteredInvoices.size)
    }

    @Test
    fun `filteredInvoices matches the query case-insensitively across fields`() {
        val byNumber = InvoicesViewModel.UiState(invoices = invoices, searchText = "001").filteredInvoices
        assertEquals(listOf("1"), byNumber.map { it.id })

        val byStatus = InvoicesViewModel.UiState(invoices = invoices, searchText = "PENDING").filteredInvoices
        assertEquals(listOf("2"), byStatus.map { it.id })
    }
}
