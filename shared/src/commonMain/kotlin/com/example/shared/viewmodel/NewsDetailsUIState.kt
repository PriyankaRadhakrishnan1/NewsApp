package com.example.shared.viewmodel

sealed class NewsDetailUiState {
    object Loading : NewsDetailUiState()
    data class Success(val url: String) : NewsDetailUiState()
    data class Error(val message: String) : NewsDetailUiState()
}
