package com.example.newsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.newsapp.di.AppModule
import com.example.newsapp.ui.navigation.AppNavigation
import com.example.newsapp.ui.theme.NewsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.init(this) //check is this required.
        enableEdgeToEdge()
        setContent {
            NewsAppTheme {
                AppNavigation()
            }
        }
    }
}