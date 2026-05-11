package com.example.skillexchange.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object SkillBoard : Screen("skill_board", "Board", Icons.AutoMirrored.Filled.List)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object CreatePost : Screen("create_post", "Post", Icons.Default.Add)
    object Swaps : Screen("swaps", "Swaps", Icons.Default.SwapHoriz)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
}
