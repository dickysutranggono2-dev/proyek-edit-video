package com.dik.videoedit.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dik.videoedit.screens.EditorScreen
import com.dik.videoedit.screens.ExportScreen
import com.dik.videoedit.screens.MainScreen

object Routes {
    const val MAIN = "main"
    const val EDITOR = "editor/{id}"
    const val EXPORT = "export/{id}"
    fun editor(id: String) = "editor/$id"
    fun export(id: String) = "export/$id"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val fadeSpec = tween<Float>(120) // animasi minimal
    NavHost(
        navController = nav,
        startDestination = Routes.MAIN,
        enterTransition = { fadeIn(fadeSpec) },
        exitTransition = { fadeOut(fadeSpec) },
        popEnterTransition = { fadeIn(fadeSpec) },
        popExitTransition = { fadeOut(fadeSpec) },
    ) {
        composable(Routes.MAIN) {
            MainScreen(onOpenProject = { nav.navigate(Routes.editor(it)) })
        }
        composable(Routes.EDITOR, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            EditorScreen(
                projectId = id,
                onBack = { nav.popBackStack() },
                onExport = { nav.navigate(Routes.export(id)) },
            )
        }
        composable(Routes.EXPORT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            ExportScreen(
                projectId = id,
                onBack = { nav.popBackStack() },
                onFinish = { nav.popBackStack(Routes.MAIN, inclusive = false) },
            )
        }
    }
}
