package com.filmtrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filmtrack.app.ui.screens.camera.CameraScreen
import com.filmtrack.app.ui.screens.editroll.EditRollScreen
import com.filmtrack.app.ui.screens.rolldetail.RollDetailScreen
import com.filmtrack.app.ui.screens.rolls.RollListScreen

object Routes {
    const val ROLL_LIST = "roll_list"
    const val ROLL_DETAIL = "roll_detail/{rollId}"
    const val EDIT_ROLL = "edit_roll/{rollId}"
    const val NEW_ROLL = "new_roll"
    const val CAMERA = "camera/{rollId}"
    const val CAMERA_QUICK = "camera_quick"

    fun rollDetail(rollId: Long) = "roll_detail/$rollId"
    fun editRoll(rollId: Long) = "edit_roll/$rollId"
    fun camera(rollId: Long) = "camera/$rollId"
}

@Composable
fun FilmTrackNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.ROLL_LIST
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ROLL_LIST) {
            RollListScreen(
                onRollClick = { rollId ->
                    navController.navigate(Routes.rollDetail(rollId))
                },
                onNewRollClick = {
                    navController.navigate(Routes.NEW_ROLL)
                }
            )
        }

        composable(
            route = Routes.ROLL_DETAIL,
            arguments = listOf(navArgument("rollId") { type = NavType.LongType })
        ) {
            RollDetailScreen(
                onBackClick = { navController.popBackStack() },
                onEditClick = { rollId ->
                    navController.navigate(Routes.editRoll(rollId))
                },
                onCaptureClick = { rollId ->
                    navController.navigate(Routes.camera(rollId))
                }
            )
        }

        composable(
            route = Routes.EDIT_ROLL,
            arguments = listOf(navArgument("rollId") { type = NavType.LongType })
        ) {
            EditRollScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.NEW_ROLL) {
            EditRollScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { rollId ->
                    navController.popBackStack()
                    navController.navigate(Routes.rollDetail(rollId))
                },
                isNew = true
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(navArgument("rollId") { type = NavType.LongType })
        ) {
            CameraScreen(
                onBackClick = { navController.popBackStack() },
                onPhotoCaptured = { navController.popBackStack() }
            )
        }

        composable(Routes.CAMERA_QUICK) {
            CameraScreen(
                onBackClick = { rollId ->
                    if (rollId != null) {
                        navController.navigate(Routes.rollDetail(rollId)) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onPhotoCaptured = { rollId ->
                    if (rollId != null) {
                        navController.navigate(Routes.rollDetail(rollId)) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                isQuickCapture = true
            )
        }
    }
}
