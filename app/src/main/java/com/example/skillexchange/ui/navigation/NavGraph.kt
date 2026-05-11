package com.example.skillexchange.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skillexchange.ui.screens.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SkillExchangeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.SkillBoard.route) {
            SkillBoardScreen(
                onChatClick = { userId ->
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val threadId = if (currentUserId < userId) "${currentUserId}_$userId" else "${userId}_$currentUserId"
                    navController.navigate(Screen.Chat.createRoute(threadId))
                }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen()
        }
        composable(Screen.CreatePost.route) {
            CreatePostScreen(onPostCreated = {
                navController.navigate(Screen.SkillBoard.route) {
                    popUpTo(Screen.CreatePost.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Swaps.route) {
            SwapScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(chatId = chatId)
        }
    }
}
