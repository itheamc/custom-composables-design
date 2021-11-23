package com.itheamc.customcomposables

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.itheamc.customcomposables.ui.grid.Grid
import com.itheamc.customcomposables.ui.grid.GridCells
import com.itheamc.customcomposables.ui.grid.GridDirection
import com.itheamc.customcomposables.ui.grid.GridType
import com.itheamc.customcomposables.ui.scaffold.FabPosition
import com.itheamc.customcomposables.ui.scaffold.ResponsiveScaffold
import com.itheamc.customcomposables.ui.staggeredgrid.StaggeredGridCells
import com.itheamc.customcomposables.ui.staggeredgrid.StaggeredGridDirection
import com.itheamc.customcomposables.ui.staggeredgrid.StaggeredGrid
import com.itheamc.customcomposables.ui.theme.CustomComposablesTheme
import kotlin.random.Random

@ExperimentalMaterialApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {


            val icons = listOf(
                Icons.Filled.Home,
                Icons.Filled.ShoppingCart,
                Icons.Filled.Notifications,
                Icons.Filled.Settings
            )
            var selected by remember {
                mutableStateOf(0)
            }

            CustomComposablesTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ResponsiveScaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(text = "Home")
                                },
                                navigationIcon = {
                                    IconButton(onClick = { }) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            BottomNavigation(
                                content = {
                                    icons.forEachIndexed { i, icon ->
                                        BottomNavigationItem(
                                            selected = i == selected,
                                            onClick = { selected = i },
                                            icon = {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = icon.name
                                                )
                                            },
                                            label = {
                                                Text(text = icon.name.replace("Filled.", ""))
                                            },
                                            alwaysShowLabel = false
                                        )
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "search"
                                )
                            }
                        },
//                        isFloatingActionButtonDocked = true,
                        floatingActionButtonPosition = FabPosition.End,
                    ) {
//                        StaggeredGrid(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .padding(it),
//                            staggeredGridDirection = StaggeredGridDirection.Vertical,
//                            cells = StaggeredGridCells.Adaptive(175.dp),
//                        ) {
//                            
//                        }
                        MyGrid(modifier = Modifier
                            .fillMaxSize()
                            .padding(it))
                    }

                }
            }
        }
    }
}


@ExperimentalMaterialApi
@Composable
fun MyGrid(
    modifier: Modifier
) {
    Grid(
        modifier = modifier,
        gridDirection = GridDirection.Vertical,
        gridType = GridType.Staggered,
        cells = GridCells.Adaptive(220.dp)
//        cells = GridCells.Fixed(4)
    ) {
        (1 until 31).forEach { n ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((Random.nextInt(250) + 50).dp),
                onClick = { /*TODO*/ },
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(
                    red = Random.nextInt(256),
                    green = Random.nextInt(256),
                    blue = Random.nextInt(256),
                    alpha = 255
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "This is cell $n")
                }
            }
        }
    }
}
