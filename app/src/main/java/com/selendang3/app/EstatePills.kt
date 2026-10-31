package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainHeight

internal val EstateControlHeight = 88.dp

/** Fill every row, including the final row. Measure all labels before choosing a common height. */
@Composable
internal fun EstatePillGrid(
    count: Int,
    modifier: Modifier = Modifier,
    minimumWidth: Dp = 140.dp,
    maxColumns: Int = Int.MAX_VALUE,
    content: @Composable (Int) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Layout(content = { repeat(count) { index -> Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) { content(index) } } }, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.maxWidth, 0) {}
        } else {
            val gap = 8.dp.roundToPx()
            val columns = ((constraints.maxWidth + gap) / ((minimumWidth * fontScale).roundToPx() + gap))
                .coerceIn(1, minOf(maxColumns, count))
            val rows = (count + columns - 1) / columns
            val widths = measurables.indices.map { index ->
                val rowCount = minOf(columns, count - (index / columns) * columns)
                val available = (constraints.maxWidth - gap * (rowCount - 1)).coerceAtLeast(rowCount)
                val column = index % columns
                val width = available / rowCount + if (column < available % rowCount) 1 else 0
                width
            }
            val height = maxOf(EstateControlHeight.roundToPx(), measurables.indices.maxOf { measurables[it].maxIntrinsicHeight(widths[it]) })
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixed(widths[index], height))
            }
            layout(constraints.maxWidth, constraints.constrainHeight(rows * height + (rows - 1) * gap)) {
                placeables.forEachIndexed { index, placeable ->
                    val rowCount = minOf(columns, count - (index / columns) * columns)
                    val available = (constraints.maxWidth - gap * (rowCount - 1)).coerceAtLeast(rowCount)
                    val column = index % columns
                    val x = column * (available / rowCount + gap) + minOf(column, available % rowCount)
                    placeable.placeRelative(x, (index / columns) * (height + gap) + (height - placeable.height) / 2)
                }
            }
        }
    }
}

@Composable
internal fun <T> EstatePillSelector(options: List<T>, selection: T, onSelect: (T) -> Unit, label: (T) -> String) {
    EstatePillGrid(options.size, Modifier.selectableGroup()) { index ->
        val option = options[index]
        FilledTonalButton(onClick = { onSelect(option) },
            modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight).semantics {
                selected = option == selection
                role = Role.RadioButton
            }, shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (option == selection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (option == selection) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) {
            Text(label(option), textAlign = TextAlign.Center)
        }
    }
}
