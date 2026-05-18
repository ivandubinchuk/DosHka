package com.example.doshka

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.ui.navigation.DoshkaNavGraph
import com.example.doshka.ui.navigation.Screen
import com.example.doshka.ui.theme.DoshkaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private var pendingDeepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Встановлюємо splash screen до super.onCreate
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Показуємо splash поки йде ініціалізація
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        // Обробляємо deep link
        handleDeepLink(intent)

        enableEdgeToEdge()

        setContent {
            // Читаємо налаштування темної теми з DataStore
            val isDarkTheme by dataStoreManager.darkTheme.collectAsStateWithLifecycle(initialValue = false)

            DoshkaTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Визначаємо стартовий екран залежно від авторизації
                    val isLoggedIn = remember {
                        runBlocking { dataStoreManager.accessToken.first()?.isNotBlank() == true }
                    }
                    val startDestination = if (isLoggedIn) Screen.Board.route else Screen.Login.route

                    // Вимикаємо splash screen після композиції
                    LaunchedEffect(Unit) {
                        keepSplashScreen = false
                    }

                    // Обробляємо pending deep link після навігації
                    LaunchedEffect(navController) {
                        pendingDeepLink?.let { deepLink ->
                            if (isLoggedIn) {
                                handleDeepLinkNavigation(navController, deepLink)
                            }
                            pendingDeepLink = null
                        }
                    }

                    DoshkaNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            pendingDeepLink = uri.toString()
        }
    }

    private fun handleDeepLinkNavigation(navController: NavHostController, deepLink: String) {
        when {
            // doshka://task/create - створення задачі
            deepLink.contains("task/create") -> {
                // Навігуємо на дошку (там є функція створення)
                navController.navigate(Screen.Board.route) {
                    popUpTo(Screen.Board.route) { inclusive = true }
                }
            }
            // doshka://dashboard - дашборд
            deepLink.contains("dashboard") -> {
                navController.navigate(Screen.Dashboard.route)
            }
            // doshka://tasks/mine - мої задачі
            deepLink.contains("tasks/mine") -> {
                // Поки що навігуємо на дошку
                navController.navigate(Screen.Board.route) {
                    popUpTo(Screen.Board.route) { inclusive = true }
                }
            }
            // doshka://invite?token=... - QR-запрошення
            deepLink.contains("invite") -> {
                val token = android.net.Uri.parse(deepLink).getQueryParameter("token")
                if (token != null) {
                    navController.navigate(Screen.Register.createRoute(token))
                }
            }
        }
    }
}
