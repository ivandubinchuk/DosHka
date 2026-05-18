package com.example.doshka.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.doshka.ui.screens.auth.LoginScreen
import com.example.doshka.ui.screens.auth.RegisterScreen
import com.example.doshka.ui.screens.board.BoardScreen
import com.example.doshka.ui.screens.chat.TaskChatScreen
import com.example.doshka.ui.screens.dashboard.DashboardScreen
import com.example.doshka.ui.screens.settings.SettingsScreen
import com.example.doshka.ui.screens.task.TaskDetailsScreen
import com.example.doshka.ui.screens.task.TaskEditScreen

/**
 * Маршрути навігації
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register?inviteToken={inviteToken}") {
        fun createRoute(inviteToken: String? = null): String {
            return if (inviteToken != null) {
                "register?inviteToken=$inviteToken"
            } else {
                "register"
            }
        }
    }
    data object Board : Screen("board")
    data object TaskDetails : Screen("task/{taskId}") {
        fun createRoute(taskId: String) = "task/$taskId"
    }
    data object TaskChat : Screen("task/{taskId}/chat") {
        fun createRoute(taskId: String) = "task/$taskId/chat"
    }
    data object TaskEdit : Screen("task/{taskId}/edit") {
        fun createRoute(taskId: String) = "task/$taskId/edit"
    }
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
    data object TeamMembers : Screen("team/members")
    data object CreateTask : Screen("task/create?columnId={columnId}") {
        fun createRoute(columnId: String) = "task/create?columnId=$columnId"
    }
}

@Composable
fun DoshkaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Екран входу
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.createRoute())
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Board.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Екран реєстрації
        composable(
            route = Screen.Register.route,
            arguments = listOf(
                navArgument("inviteToken") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val inviteToken = backStackEntry.arguments?.getString("inviteToken")
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Board.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                inviteToken = inviteToken
            )
        }

        // Канбан-дошка
        composable(Screen.Board.route) {
            BoardScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToTaskDetails = { taskId ->
                    navController.navigate(Screen.TaskDetails.createRoute(taskId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Дашборд
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Налаштування
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Деталі задачі
        composable(
            route = Screen.TaskDetails.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailsScreen(
                taskId = taskId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { id ->
                    navController.navigate(Screen.TaskChat.createRoute(id))
                },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.TaskEdit.createRoute(id))
                }
            )
        }

        // Редагування задачі
        composable(
            route = Screen.TaskEdit.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskEditScreen(
                taskId = taskId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { id ->
                    navController.navigate(Screen.TaskChat.createRoute(id))
                }
            )
        }

        // Чат задачі
        composable(
            route = Screen.TaskChat.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskChatScreen(
                taskId = taskId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

    }
}

/**
 * Контролер навігації для використання з ViewModel
 */
class DoshkaNavigator {
    var navController: NavHostController? = null

    fun navigateTo(screen: Screen) {
        navController?.navigate(screen.route)
    }

    fun navigateToRegisterWithInvite(inviteToken: String) {
        navController?.navigate(Screen.Register.createRoute(inviteToken))
    }

    fun navigateToTask(taskId: String) {
        navController?.navigate(Screen.TaskDetails.createRoute(taskId))
    }

    fun navigateBack() {
        navController?.popBackStack()
    }
}
