package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.repo.NewsDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsDetailViewModel(
    private val newsDetailRepository: NewsDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsDetailUiState>(NewsDetailUiState.Loading)
    val uiState: StateFlow<NewsDetailUiState> = _uiState

    fun loadNewsUrl(id: String) {
        viewModelScope.launch {
            _uiState.value = NewsDetailUiState.Loading
            try {
                val url = newsDetailRepository.getNewsUrlById(id)
                if (!url.isNullOrEmpty()) {
                    _uiState.value = NewsDetailUiState.Success(url)
                } else {
                    _uiState.value = NewsDetailUiState.Error("No news found for this ID.")
                }
            } catch (e: Exception) {
                _uiState.value = NewsDetailUiState.Error("Something went wrong: ${e.localizedMessage}")
            }
        }
    }
}
