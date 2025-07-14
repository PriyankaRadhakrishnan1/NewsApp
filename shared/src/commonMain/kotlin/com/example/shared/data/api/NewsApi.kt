package com.example.shared.data.api

import com.example.shared.data.dto.ErrorResponse
import com.example.shared.data.dto.NewsResponse
import com.example.shared.domain.model.NewsApiReponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class NewsApi(private val client: HttpClient) {
    private val apiKey = "e2c3687548024c0488a65f36d74fe7cb"

    suspend fun fetchTopHeadlines(
        page: Int,
        pageSize: Int,
        country: String = "US",
    ): NewsApiReponse {
       return try {
            val response = client.get("https://newsapi.org/v2/top-headlines") {
                parameter("country", country)
                parameter("page", page)
                parameter("pageSize", pageSize)
                parameter("apiKey", apiKey)
            }
           if (response.status.isSuccess()) {
               val body = response.body<NewsResponse>()
               println("API Response (Top Headlines): $body")
               NewsApiReponse.Success(body)
           } else {
               val error =  response.body<ErrorResponse>()
               println("API Error Response: $error")
               NewsApiReponse.Error(error)
           }
        }catch (e: Exception){
           NewsApiReponse.Error(ErrorResponse("Unknown", "Unknown", message = e.localizedMessage ?: "Unknown exception"))
        }
    }

    suspend fun searchNews(
        query: String,
        page: Int,
        pageSize: Int
    ): NewsApiReponse {
        return try {
            val response = client.get("https://newsapi.org/v2/everything") {
                parameter("q", query)
                parameter("page", page)
                parameter("pageSize", pageSize)
                parameter("apiKey", apiKey)
            }
            if (response.status.isSuccess()) {
                val body = response.body<NewsResponse>()
                println("API Response (Top Headlines): $body")
                NewsApiReponse.Success(body)
            } else {
                val error =  response.body<ErrorResponse>()
                println("API Error Response: $error")
                NewsApiReponse.Error(error)
            }
        }catch (e: Exception){
            NewsApiReponse.Error(ErrorResponse("Unknown", "Unknown", message = e.localizedMessage ?: "Unknown exception"))
        }
    }
}