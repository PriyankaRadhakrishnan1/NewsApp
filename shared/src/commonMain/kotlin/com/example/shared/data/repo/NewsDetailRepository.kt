package com.example.shared.data.repo

import com.example.shared.NewsAppDatabase

class NewsDetailRepository( private val db: NewsAppDatabase) {
    fun getNewsUrlById(id: String): String? {
        return if (id.isNotBlank()) {
            db.newsQueries.transactionWithResult {
                db.newsQueries.getDetailedNews(id).executeAsOneOrNull()?.url
            }
        } else {
            null
        }
    }
}