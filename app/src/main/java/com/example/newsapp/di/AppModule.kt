package com.example.newsapp.di

import android.content.Context
import com.example.shared.NewsAppDatabase
import com.example.shared.data.api.NewsApi
import com.example.shared.data.repo.NewsDetailRepository
import com.example.shared.data.repo.NewsRepository
import com.example.shared.db.DatabaseDriverFactory
import com.example.shared.viewmodel.NewsDetailViewModel
import com.example.shared.viewmodel.NewsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

//Dependency- Injection (Return Singleton)
/**Creates and holds shared dependencies,
    Ensures dependencies are initialized once,Makes them easily accessible across your app
 **/
//Koin(KMP), Dagger(Common DI), Hilt(Android)

object AppModule {
    lateinit var newsViewModel : NewsViewModel
        private set // this means we can access from anywhere but only modified in here.

    lateinit var newsDetailsViewModel : NewsDetailViewModel
        private set

    fun init(context: Context){
        val db = NewsAppDatabase(
            DatabaseDriverFactory(context).createDriver()
        )
        val client = HttpClient(OkHttp){
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("Ktor Logger: $message")
                    }
                }
                level = LogLevel.ALL
            }
        }
        val api = NewsApi(client)
        val newsRepository = NewsRepository(api, db)
        val newsDetailrepo = NewsDetailRepository(db)

        newsViewModel = NewsViewModel(newsRepository)
        newsDetailsViewModel = NewsDetailViewModel(newsDetailrepo)

    }
}