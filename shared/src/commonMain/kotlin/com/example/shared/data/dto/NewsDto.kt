package com.example.shared.data.dto

import kotlinx.serialization.Serializable
//DTO stands for Data Transfer Object
@Serializable
data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

@Serializable
data class Article(
    val source: Source?=null,
    val author: String?= null,
    val title: String,
    val description: String?=null,
    val url: String,
    val urlToImage: String?= null,
    val publishedAt: String,
    val content: String?= null
)

@Serializable
data class Source(
    val id: String?= null,
    val name: String? = null
)

@Serializable
data class ErrorResponse(
    val status: String,
    val code: String,
    val message: String
)
