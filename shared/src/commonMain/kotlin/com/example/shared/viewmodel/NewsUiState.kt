package com.example.shared.viewmodel

import com.example.shared.domain.model.News


sealed class NewsUiState{
    object Loading: NewsUiState()
    data class Success(val news: List<News>, val hasMoreToLoad: Boolean) : NewsUiState()
    data class Error(
        val message: String,
        val isPagination: Boolean = false,
        val isMaxResultsReached: Boolean = false
    ) : NewsUiState()
    object NoData: NewsUiState()
}