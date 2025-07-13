package com.example.shared.domain.model

import com.example.shared.data.dto.ErrorResponse
import com.example.shared.data.dto.NewsResponse

data class News(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    val publishedAt: String
)

sealed class NewsApiReponse{
    data class Success(val newsResponse: NewsResponse): NewsApiReponse()
    data class Error(val errorMessage: ErrorResponse): NewsApiReponse()
}