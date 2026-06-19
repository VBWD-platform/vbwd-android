package com.vbwd.core.ui.dashboard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DashboardWidgetLayoutTest {
    @Test
    fun `rowCount rounds up to whole rows for the default 2 columns`() {
        assertEquals(0, DashboardWidgetLayout.rowCount(0))
        assertEquals(1, DashboardWidgetLayout.rowCount(1))
        assertEquals(1, DashboardWidgetLayout.rowCount(2))
        assertEquals(2, DashboardWidgetLayout.rowCount(3))
        assertEquals(2, DashboardWidgetLayout.rowCount(4))
        assertEquals(3, DashboardWidgetLayout.rowCount(5))
    }

    @Test
    fun `rowCount honours a custom column count`() {
        assertEquals(1, DashboardWidgetLayout.rowCount(3, columns = 3))
        assertEquals(2, DashboardWidgetLayout.rowCount(4, columns = 3))
    }

    @Test
    fun `default grid is two columns`() {
        assertEquals(2, DashboardWidgetLayout.COLUMN_COUNT)
    }
}
