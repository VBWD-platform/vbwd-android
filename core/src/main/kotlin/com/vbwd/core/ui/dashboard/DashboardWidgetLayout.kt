package com.vbwd.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vbwd.core.plugins.ComponentFactory

private const val FULL_WEIGHT = 1f

/**
 * Grid arithmetic + layout for plugin-contributed dashboard widgets. Port of the
 * iOS `DashboardWidgetLayout` (2-column, 16dp spacing, 120dp tall cards). The
 * arithmetic ([rowCount]) is pure so it is unit-testable without rendering.
 */
object DashboardWidgetLayout {
    const val COLUMN_COUNT = 2
    val itemHeight = 120.dp
    val spacing = 16.dp
    val cornerRadius = 12.dp

    /** Rows needed to lay [itemCount] widgets out in [columns] columns. */
    fun rowCount(itemCount: Int, columns: Int = COLUMN_COUNT): Int =
        if (itemCount <= 0 || columns <= 0) 0 else (itemCount + columns - 1) / columns
}

/**
 * Renders [widgets] in a fixed 2-column grid (chunked rows — safe to embed in a
 * scrolling column, unlike a nested lazy grid). Each cell is a themed card.
 */
@Composable
fun DashboardWidgetGrid(
    widgets: List<Pair<String, ComponentFactory>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DashboardWidgetLayout.spacing)) {
        widgets.chunked(DashboardWidgetLayout.COLUMN_COUNT).forEach { rowWidgets ->
            Row(horizontalArrangement = Arrangement.spacedBy(DashboardWidgetLayout.spacing)) {
                rowWidgets.forEach { (_, factory) ->
                    Box(
                        modifier = Modifier
                            .weight(FULL_WEIGHT)
                            .height(DashboardWidgetLayout.itemHeight)
                            .clip(RoundedCornerShape(DashboardWidgetLayout.cornerRadius))
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        factory()
                    }
                }
                repeat(DashboardWidgetLayout.COLUMN_COUNT - rowWidgets.size) {
                    Spacer(modifier = Modifier.weight(FULL_WEIGHT))
                }
            }
        }
    }
}
