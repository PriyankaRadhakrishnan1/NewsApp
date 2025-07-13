package com.example.shared.data.repo

import com.example.shared.NewsAppDatabase
import com.example.shared.data.api.NewsApi
import com.example.shared.domain.mapper.mapToNewsList
import com.example.shared.domain.model.News
import com.example.shared.domain.model.NewsApiReponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsRepository(
    private val api: NewsApi,
    private val db: NewsAppDatabase
) {
    private var _totalAvailableResults: Int? = null

    suspend fun fetchNewsPage(page: Int, pageSize: Int, searchQuery: String): NewsApiReponse = withContext(Dispatchers.IO) {
        try {
            println("Repository: Fetching news: page=$page, pageSize=$pageSize, query='$searchQuery'")

            val response = if (searchQuery.isNotBlank()) {
                api.searchNews(query = searchQuery, page = page, pageSize = pageSize)
            } else {
                api.fetchTopHeadlines(page = page, pageSize = pageSize)
            }
            if (response is NewsApiReponse.Success) {
                val newsList = response.newsResponse.mapToNewsList()

                println("Repository: Fetched ${newsList.size} articles from API (total available: ${response.newsResponse.totalResults})")

                _totalAvailableResults = response.newsResponse.totalResults

                db.newsQueries.transaction {
                    newsList.forEach { news ->
                        db.newsQueries.insertNews(
                            id = news.id,
                            title = news.title,
                            description = news.description,
                            content = news.content,
                            url = news.url,
                            imageUrl = news.imageUrl,
                            publishedAt = news.publishedAt,
                            source = news.source
                        )
                    }
                }
            }
            return@withContext response
        } catch (e: Exception) {
            println("Repository: Error fetching news from API: ${e.localizedMessage}")
            throw e
        }
    }

    suspend fun getCachedNews(): List<News> = withContext(Dispatchers.IO) {
        val result = db.newsQueries.selectAll()
            .executeAsList()
            .map {
                News(
                    id = it.id,
                    title = it.title.orEmpty(),
                    description = it.description.orEmpty(),
                    content = it.content.orEmpty(),
                    url = it.url.orEmpty(),
                    imageUrl = it.imageUrl.orEmpty(),
                    publishedAt = it.publishedAt.orEmpty(),
                    source = it.source.orEmpty()
                )
            }
        println("Repository: Loaded ${result.size} items from cache.")
        result
    }

    fun hasMoreData(loadedCount: Int): Boolean {
        val result = _totalAvailableResults == null || loadedCount < _totalAvailableResults!!
        println("Repository: hasMoreData(loadedCount=$loadedCount, totalAvailableResults=$_totalAvailableResults) = $result")
        return result
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        db.newsQueries.clearAllNews()
        _totalAvailableResults = null
        println("Repository: Cache cleared.")
    }
}