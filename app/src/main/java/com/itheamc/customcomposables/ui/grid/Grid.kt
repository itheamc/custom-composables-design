package com.itheamc.customcomposables.ui.grid

import androidx.compose.foundation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composable to place items in staggered grid
 */
@Composable
fun Grid(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    gridType: GridType = GridType.Normal,
    gridDirection: GridDirection = GridDirection.Vertical,
    cells: GridCells = GridCells.Fixed(rowsOrCols = 2),
    gap: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val newModifier = if (gridDirection == GridDirection.Vertical) {
        modifier.verticalScroll(
            state = scrollState
        )
    } else {
        modifier.horizontalScroll(
            scrollState
        )
    }

    GridLayout(
        modifier = newModifier,
        gridType = gridType,
        gridDirection = gridDirection,
        cells = cells,
        gap = gap,
        content = content
    )
}


/**
 * Staggered Grid Layout Composable
 */
@Composable
private fun GridLayout(
    modifier: Modifier = Modifier,
    gridType: GridType,
    gridDirection: GridDirection,
    cells: GridCells,
    gap: Dp,
    content: @Composable () -> Unit
) {
    val measurePolicy =
        rememberGridMeasurePolicy(
            gridType = gridType,
            gridDirection = gridDirection,
            cells = cells,
            gap = gap
        )
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = measurePolicy
    )
}


/**
 * Composable to remember grid measure policy
 */
@Composable
private fun rememberGridMeasurePolicy(
    gridType: GridType,
    gridDirection: GridDirection,
    cells: GridCells,
    gap: Dp
) =
    remember(gridType, gridDirection, cells, gap) {
        if (gridDirection == GridDirection.Vertical) {
            columnGridMeasurePolicy(gridType = gridType, cells = cells, gap = gap)
        } else {
            rowGridMeasurePolicy(gridType = gridType, cells = cells, gap = gap)
        }
    }


/**
 * Column Staggered Grid Measure Policy
 */
internal fun columnGridMeasurePolicy(gridType: GridType, cells: GridCells, gap: Dp) =
    MeasurePolicy { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@MeasurePolicy layout(
                constraints.minWidth,
                constraints.minHeight
            ) {}
        }

        // Maximum width of each column i.e. item
        val pixelGap = gap.toPx().toInt()

        // Calculating number of columns
        val cols = when (cells) {
            is GridCells.Fixed ->
                if (cells.rowsOrCols <= 0) 2 else cells.rowsOrCols
            is GridCells.Adaptive -> {
                val c =
                    (((constraints.maxWidth) - pixelGap) / (cells.maxWidthOrHeight.toPx() + pixelGap)).toInt()
                if (c <= 0) 1 else c
            }
        }

        // Calculating item width
        val itemWidth = when (cells) {
            is GridCells.Adaptive -> {
                val cellWidth = cells.maxWidthOrHeight.toPx()
                if (cellWidth.toInt() < constraints.maxWidth) {
                    val extraWidth = (constraints.maxWidth - pixelGap) % (cellWidth + pixelGap)
                    (cellWidth + (extraWidth / cols)).toInt()
                } else {
                    (constraints.maxWidth - ((cols + 1) * pixelGap)) / cols
                }
            }
            else ->
                (constraints.maxWidth - ((cols + 1) * pixelGap)) / cols
        }

        // Redefining the constraints by setting minWidth., minHeight and maxWidth
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = if (itemWidth > 0) itemWidth else 1
            )


        // Arrays to store the height of the each column
        val itemsHeights = IntArray(measurables.size) { 0 }


        // List of placeable by measuring each item
        val placeables = measurables.mapIndexed { index, measurable ->
            // Measure each child
            val placeable = measurable.measure(looseConstraints)

            // Tracking the height of each item
            itemsHeights[index] = placeable.height

            placeable
        }

        // Calculating the XY coordinate for each item
        // and adding to the list coordinates
        val coordinates = mutableListOf<Coordinate>()

        itemsHeights.forEachIndexed { index, _ ->
            // Coordinate X
            val x = if (index % cols == 0) {
                pixelGap
            } else {
                pixelGap + ((itemWidth + pixelGap) * (index % cols))
            }

            // Coordinate Y
            val y = if (index == 0) {
                pixelGap
            } else if (gridType == GridType.Staggered) {
                pixelGap + (itemsHeights.filterIndexed { ind, _ ->
                    ind < index && (ind % cols == index % cols)
                }.sumOf { it + pixelGap })
            } else {
                if (index % cols == 0) {
                    (index - cols until index).map { i ->
                        coordinates[i].y + itemsHeights[i] + pixelGap
                    }.maxOf { it }

                } else {
                    coordinates[coordinates.size - 1].y
                }
            }

            coordinates.add(index, Coordinate(x = x, y = y))

        }

        // Calculating the max width and height for the layout
        val width = constraints.maxWidth
        val height = if (gridType == GridType.Staggered) {
            (0 until cols).map { n ->
                itemsHeights.filterIndexed { ind, _ -> ind % cols == n }
                    .sumOf { it + pixelGap } + pixelGap
            }.maxOf { it }
        } else {
            (itemsHeights.size - cols until itemsHeights.size).map { i ->
                coordinates[i].y + itemsHeights[i] + pixelGap
            }.maxOf { it }
        }

        // Set the size of the parent layout
        layout(width = width, height = height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = coordinates[index].x,
                    y = coordinates[index].y
                )
            }
        }

    }


/**
 * --------------------------------------------------------------
 * Row Staggered Grid
 */
internal fun rowGridMeasurePolicy(gridType: GridType, cells: GridCells, gap: Dp) =
    MeasurePolicy { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@MeasurePolicy layout(
                constraints.minWidth,
                constraints.minHeight
            ) {}
        }

        // Maximum width of each column i.e. item
        val pixelGap = gap.toPx().toInt()

        // Calculating the number of rows
        val rows = when (cells) {
            is GridCells.Fixed ->
                if (cells.rowsOrCols <= 0) 2 else cells.rowsOrCols
            is GridCells.Adaptive -> {
                val c =
                    (((constraints.maxHeight) - pixelGap) / (cells.maxWidthOrHeight.toPx() + pixelGap)).toInt()
                if (c <= 0) 1 else c
            }
        }

        // Calculating the item height
        val itemHeight = when (cells) {
            is GridCells.Adaptive -> {
                val cellHeight = cells.maxWidthOrHeight.toPx()
                if (cellHeight.toInt() < constraints.maxHeight) {
                    val extraHeight = (constraints.maxHeight - pixelGap) % (cellHeight + pixelGap)
                    (cellHeight + (extraHeight / rows)).toInt()
                } else {
                    (constraints.maxHeight - ((rows + 1) * pixelGap)) / rows
                }
            }
            else ->
                (constraints.maxHeight - ((rows + 1) * pixelGap)) / rows
        }

        // Redefining the constraints by setting minWidth, minHeight and maxWidth
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = if (itemHeight > 0) itemHeight else 1
            )


        // Arrays to store the height of the each column
        val itemsWidths = IntArray(measurables.size) { 0 }


        // List of placeable by measuring each item
        val placeables = measurables.mapIndexed { index, measurable ->
            // Measure each child
            val placeable = measurable.measure(looseConstraints)

            // Tracking the height of each item
            itemsWidths[index] = placeable.width

            placeable
        }

        // Calculating the XY coordinate for each item
        // and adding to the list coordinates
        val coordinates = mutableListOf<Coordinate>()

        itemsWidths.forEachIndexed { index, _ ->
            // Coordinate X
            val x = if (index == 0) {
                pixelGap
            } else if (gridType == GridType.Staggered) {
                pixelGap + (itemsWidths.filterIndexed { ind, _ ->
                    ind < index && (ind % rows == index % rows)
                }.sumOf { it + pixelGap })
            } else {
                if (index % rows == 0) {
                    (index - rows until index).map { i ->
                        coordinates[i].x + itemsWidths[i] + pixelGap
                    }.maxOf { it }

                } else {
                    coordinates[coordinates.size - 1].x
                }
            }


            // Coordinate Y
            val y = if (index % rows == 0) {
                pixelGap
            } else {
                pixelGap + ((itemHeight + pixelGap) * (index % rows))
            }

            coordinates.add(index, Coordinate(x = x, y = y))

        }

        // Calculating the max width and height for the layout
        val width = if (gridType == GridType.Staggered) {
            (0 until rows).map { n ->
                itemsWidths.filterIndexed { ind, _ -> ind % rows == n }
                    .sumOf { it + pixelGap } + pixelGap
            }.maxOf { it }
        } else {
            (itemsWidths.size - rows until itemsWidths.size).map { i ->
                coordinates[i].x + itemsWidths[i] + pixelGap
            }.maxOf { it }
        }

        val height = constraints.maxHeight

        // Set the size of the parent layout
        layout(width = width, height = height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = coordinates[index].x,
                    y = coordinates[index].y
                )
            }
        }

    }


private data class Coordinate(val x: Int = 0, val y: Int = 0)
enum class GridDirection {
    Horizontal,
    Vertical
}

enum class GridType {
    Staggered,
    Normal
}

sealed class GridCells {
    class Fixed(val rowsOrCols: Int) : GridCells()
    class Adaptive(val maxWidthOrHeight: Dp) : GridCells()
}