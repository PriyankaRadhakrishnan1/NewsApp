package com.example.shared.domain.mapper

import com.example.shared.data.dto.Article
import com.example.shared.data.dto.NewsResponse
import com.example.shared.domain.model.News
//Use to map the api response into an object
fun Article.mapToNews(): News = News(
    id = url,
    title = title,
    description = description.orEmpty(),
    content = content.orEmpty(),
    url = url,
    imageUrl = urlToImage?:"",
    publishedAt = publishedAt,
    source = source?.name ?: "Unknown"
)

fun NewsResponse.mapToNewsList(): List<News> = articles.map { it.mapToNews() }