package com.itheamc.customcomposables.ui.scaffold

import androidx.compose.material.*

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * State for [Scaffold] composable component.
 */
@Stable
class ResponsiveScaffoldState(
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState
)

/**
 * Creates a [ScaffoldState] with the default animation clock and memoizes it.
 */
@Composable
fun rememberResponsiveScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): ResponsiveScaffoldState = remember {
    ResponsiveScaffoldState(drawerState, snackbarHostState)
}

/**
 * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
 */
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        val Center = FabPosition(0)
        val End = FabPosition(1)
    }

    override fun toString(): String {
        return when (this) {
            Center -> "FabPosition.Center"
            else -> "FabPosition.End"
        }
    }
}

/**
 * Scaffold implements the basic material design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 */
@Composable
fun ResponsiveScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ResponsiveScaffoldState = rememberResponsiveScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier, color = backgroundColor, contentColor = contentColor) {
            ResponsiveScaffoldLayout(
                isFabDocked = isFloatingActionButtonDocked,
                fabPosition = floatingActionButtonPosition,
                topBar = topBar,
                content = content,
                snackbar = {
                    snackbarHost(scaffoldState.snackbarHostState)
                },
                fab = floatingActionButton,
                bottomBar = bottomBar
            )
        }
    }

    if (drawerContent != null) {
        ModalDrawer(
            modifier = modifier,
            drawerState = scaffoldState.drawerState,
            gesturesEnabled = drawerGesturesEnabled,
            drawerContent = drawerContent,
            drawerShape = drawerShape,
            drawerElevation = drawerElevation,
            drawerBackgroundColor = drawerBackgroundColor,
            drawerContentColor = drawerContentColor,
            scrimColor = drawerScrimColor,
            content = { child(Modifier) }
        )
    } else {
        child(modifier)
    }
}

/**
 * Layout for a [Scaffold]'s content.
 */
@Composable
private fun ResponsiveScaffoldLayout(
    isFabDocked: Boolean,
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    snackbar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val topBarPlaceables = subcompose(ScaffoldLayoutContent.TopBar, topBar).mapNotNull {
                it.measure(looseConstraints).takeIf { true }
            }

            val topBarHeight = topBarPlaceables.maxByOrNull { it.height }?.height ?: 0

            val snackbarPlaceables =
                subcompose(ScaffoldLayoutContent.Snackbar, snackbar).mapNotNull {
                    it.measure(looseConstraints)
                }

            val snackbarHeight = snackbarPlaceables.maxByOrNull { it.height }?.height ?: 0

            val fabPlaceables =
                subcompose(ScaffoldLayoutContent.Fab, fab).mapNotNull { measurable ->
                    measurable.measure(looseConstraints).takeIf { it.height != 0 && it.width != 0 }
                }

            val fabPlacement = if (fabPlaceables.isNotEmpty()) {
                val fabWidth = fabPlaceables.maxByOrNull { it.width }!!.width
                val fabHeight = fabPlaceables.maxByOrNull { it.height }!!.height
                // FAB distance from the left of the layout, taking into account LTR / RTL
                val fabLeftOffset = if (fabPosition == FabPosition.End) {
                    if (layoutDirection == LayoutDirection.Ltr) {
                        layoutWidth - FabSpacing.roundToPx() - fabWidth
                    } else {
                        FabSpacing.roundToPx()
                    }
                } else {
                    (layoutWidth - fabWidth) / 2
                }

                FabPlacement(
                    isDocked = isFabDocked,
                    left = fabLeftOffset,
                    width = fabWidth,
                    height = fabHeight
                )
            } else {
                null
            }

            val bottomBarPlaceables = subcompose(ScaffoldLayoutContent.BottomBar) {
                CompositionLocalProvider(
                    LocalFabPlacement provides fabPlacement,
                    content = bottomBar
                )
            }.mapNotNull {
                it.measure(looseConstraints).takeIf { true }
            }

            val bottomBarHeight = bottomBarPlaceables.maxByOrNull { it.height }?.height ?: 0
            val fabOffsetFromBottom = fabPlacement?.let {
                if (bottomBarHeight == 0) {
                    it.height + FabSpacing.roundToPx()
                } else {
                    if (isFabDocked) {
                        // Total height is the bottom bar height + half the FAB height
                        bottomBarHeight + (it.height / 2)
                    } else {
                        // Total height is the bottom bar height + the FAB height + the padding
                        // between the FAB and bottom bar
                        bottomBarHeight + it.height + FabSpacing.roundToPx()
                    }
                }
            }

            val snackbarOffsetFromBottom = if (snackbarHeight != 0) {
                snackbarHeight + (fabOffsetFromBottom ?: bottomBarHeight)
            } else {
                0
            }

            val bodyContentHeight = layoutHeight - topBarHeight

            val bodyContentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                val innerPadding = PaddingValues(bottom = bottomBarHeight.toDp())
                content(innerPadding)
            }.map { it.measure(looseConstraints.copy(maxHeight = bodyContentHeight)) }

            // Placing to control drawing order to match default elevation of each placeable

            bodyContentPlaceables.forEach {
                it.place(0, topBarHeight)
            }
            topBarPlaceables.forEach {
                it.place(0, 0)
            }
            snackbarPlaceables.forEach {
                it.place(0, layoutHeight - snackbarOffsetFromBottom)
            }
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceables.forEach {
                it.place(0, layoutHeight - bottomBarHeight)
            }
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlacement?.let { placement ->
                fabPlaceables.forEach {
                    it.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
                }
            }
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 */
@Immutable
internal class FabPlacement(
    val isDocked: Boolean,
    val left: Int,
    val width: Int,
    val height: Int
)

/**
 * CompositionLocal containing a [FabPlacement] that is read by [BottomAppBar] to calculate notch
 * location.
 */
internal val LocalFabPlacement = staticCompositionLocalOf<FabPlacement?> { null }

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent { TopBar, MainContent, Snackbar, Fab, BottomBar }
