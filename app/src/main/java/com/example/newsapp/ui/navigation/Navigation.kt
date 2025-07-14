package com.example.newsapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.newsapp.di.AppModule
import com.example.newsapp.ui.detailedNews.NewsDetailScreen
import com.example.newsapp.ui.news.NewsListScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home"){
        composable("home"){
            NewsListScreen(navController= navController)
        }
        composable(
            route = "news_detail_screen/{newsId}",
            arguments = listOf(navArgument("newsId") { type = NavType.StringType })
        ) { backStackEntry ->
            val newsId = backStackEntry.arguments?.getString("newsId") ?: ""
            val viewModel = remember { AppModule.newsDetailsViewModel }
            NewsDetailScreen(newsId = newsId, navController = navController, viewModel = viewModel)
        }
    }

}

