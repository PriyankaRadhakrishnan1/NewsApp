package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.dto.ErrorResponse
import com.example.shared.data.repo.NewsRepository
import com.example.shared.domain.mapper.mapToNewsList
import com.example.shared.domain.model.News
import com.example.shared.domain.model.NewsApiReponse
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class NewsViewModel(private val newsRepository: NewsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _loadedNews = mutableListOf<News>()
    val loadedNews: List<News>
        get() = _loadedNews.toList()

    private var currentPage = 1
    private var pageSize = 20
    private var isLoading = false
    private var hasMoreToLoad = true
    var isLoadingNextPage = false
        private set

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || !hasMoreToLoad) {
            println("VM: Load next page called: isLoading=$isLoading, hasMoreToLoad=$hasMoreToLoad. Returning.")
            return
        }

        isLoading = true
        isLoadingNextPage = _loadedNews.isNotEmpty()

        println("VM: isLoadingNextPage set to $isLoadingNextPage. CurrentPage=$currentPage")

        if (!isLoadingNextPage && !_isRefreshing.value) {
            _uiState.value = NewsUiState.Loading
            println("VM: Setting UI state to Loading (full screen).")
        }

        viewModelScope.launch {
            delay(1000)

            println("VM: Attempting to fetch page $currentPage...")
            try {
                // CORRECTED LINE: Pass the search query to the repository
                val newsApiResponse = newsRepository.fetchNewsPage(currentPage, pageSize, _searchQuery.value)
                if (newsApiResponse is NewsApiReponse.Success) {
                    val newItems: List<News> = newsApiResponse.newsResponse.mapToNewsList()

                    if (newItems.isEmpty() && _loadedNews.isEmpty()) {
                        hasMoreToLoad = false
                        _uiState.value = NewsUiState.NoData
                        println("VM: No new items and no existing items. Setting state to NoData.")
                    } else if (newItems.isEmpty()) {
                        hasMoreToLoad = false
                        _uiState.value = NewsUiState.Success(_loadedNews.toList(), hasMoreToLoad)
                        println("VM: No new items on page $currentPage. Assuming no more data.")
                    } else {
                        val uniqueNewItems = newItems.filter { newItem ->
                            !_loadedNews.any { existingItem -> existingItem.id == newItem.id }
                        }

                        if (uniqueNewItems.isNotEmpty()) {
                            _loadedNews.addAll(uniqueNewItems)
                            currentPage++
                            hasMoreToLoad = newsRepository.hasMoreData(_loadedNews.size)
                            _uiState.value =
                                NewsUiState.Success(_loadedNews.toList(), hasMoreToLoad)
                            println("VM: Successfully loaded ${uniqueNewItems.size} unique new items. Total loaded: ${_loadedNews.size}. Has more: $hasMoreToLoad")
                        } else {
                            println("VM: All new items were duplicates. Assuming no more data.")
                            hasMoreToLoad = false
                            _uiState.value =
                                NewsUiState.Success(_loadedNews.toList(), hasMoreToLoad)
                        }
                    }
                }else{
                    val errorResponse = newsApiResponse as NewsApiReponse.Error
                    _uiState.value = NewsUiState.Error(
                        message =errorResponse.errorMessage.message,
                        isPagination = false,
                        isMaxResultsReached = true
                    )
                }
            } catch (e: Exception) {
                println("VM: Error loading news: ${e.message}")
                if (e is ClientRequestException) {
                    try {
                        val errorBody = e.response.bodyAsText()
                        val errorResponse = Json.decodeFromString<ErrorResponse>(errorBody)
                        if (errorResponse.code == "maximumResultsReached") {
                            hasMoreToLoad = false
                            _uiState.value = NewsUiState.Error(
                                message = "You've reached the maximum number of results (100) for your account.",
                                isPagination = true,
                                isMaxResultsReached = true
                            )
                            println("VM: Caught maximumResultsReached error.")
                            return@launch
                        }
                    } catch (parseException: Exception) {
                        println("VM: Failed to parse error body: ${parseException.message}")
                    }
                }

                if (_loadedNews.isEmpty()) {
                    val cached = newsRepository.getCachedNews()
                    if (cached.isNotEmpty()) {
                        _loadedNews.addAll(cached)
                        hasMoreToLoad = newsRepository.hasMoreData(_loadedNews.size)
                        _uiState.value = NewsUiState.Success(_loadedNews.toList(), hasMoreToLoad)
                        println("VM: Loaded ${cached.size} items from cache due to network error.")
                    } else {
                        println("e.message====="+e.localizedMessage)
                        _uiState.value = NewsUiState.Error(
                            message = "Failed to load news.\n${e.message}",
                            isPagination = false
                        )
                        println("VM: Failed to load news and no cached data.")
                    }
                } else {
                    _uiState.value = NewsUiState.Error(
                        message = "Failed to load more.\n${e.message}",
                        isPagination = true
                    )
                    println("VM: Failed to load more news. Existing items: ${_loadedNews.size}.")
                }
            } finally {
                isLoading = false
                isLoadingNextPage = false
                _isRefreshing.value = false
                println("VM: isLoading set to $isLoading, isLoadingNextPage set to $isLoadingNextPage, isRefreshing set to ${_isRefreshing.value}.")
            }
        }
    }

    fun refresh() {

            if (isLoading) {
                println("VM: Refresh called while loading. Returning.")
                return
            }
            println("VM: Refreshing news...")
            _isRefreshing.value = true
            currentPage = 1
            hasMoreToLoad = true
            _loadedNews.clear()
        viewModelScope.launch {
            newsRepository.clearCache() // it clears db on refresh
        }
            loadNextPage() // now it correctly picks the current _searchQuery.value
    }

    fun retry() {
        println("VM: Retrying load...")
        loadNextPage()
    }

    fun updateSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            println("VM: Search query updated to: $query. Triggering refresh.")
            refresh() // Trigger a full refresh when search query changes
        }
    }
}