package com.example.newsapp.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.newsapp.di.AppModule
import com.example.shared.domain.model.News
import com.example.shared.viewmodel.NewsUiState
import com.example.shared.viewmodel.NewsViewModel
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    viewModel: NewsViewModel = AppModule.newsViewModel,
                   navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Coil ImageLoader setup (retained from your original)
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { OkHttpClient() }
                    )
                )
            }
            .build()
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val searchQuery by viewModel.searchQuery.collectAsState() // Observe search query from ViewModel

    // Effect to trigger loading more items when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { items ->
                val lastVisibleItem = items.lastOrNull()
                val totalItemsCount = listState.layoutInfo.totalItemsCount

                // Check if last visible item is near the end and not currently loading more
                if (lastVisibleItem != null && totalItemsCount > 1 &&
                    lastVisibleItem.index >= totalItemsCount - 2 && !viewModel.isLoadingNextPage
                ) {
                    println("UI: Condition met for loadNextPage. Calling viewModel.loadNextPage().")
                    viewModel.loadNextPage()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NewsApp") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Bar with filled style and rounded corners
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) }
            )

            // PullToRefreshBox wraps the main content
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(), // Fill remaining space in the Column
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                indicator = {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isRefreshing,
                        state = pullRefreshState,
                    )
                }
            ) {
                // Conditional UI based on NewsUiState
                when (uiState) {
                    is NewsUiState.Error -> {
                        val errorState = uiState as NewsUiState.Error
                        println("UI: Displaying UI State: Error. Is pagination error: ${errorState.isPagination}. Is max results reached: ${errorState.isMaxResultsReached}")

                        if (errorState.isPagination) {
                            // If it's a pagination error (after initial load)
                            NewsListContent(
                                news = viewModel.loadedNews, // Show already loaded news
                                listState = listState,
                                showLoading = false,
                                paginationError = errorState.message,
                                hasMoreToLoad = true,
                                onRetryPagination = viewModel::retry,
                                imageLoader = imageLoader,
                                isMaxResultsReached = errorState.isMaxResultsReached,
                                navController = navController
                            )
                        } else {
                            // If it's an initial load error (no news loaded yet)
                            CenteredMessage(errorState.message, onRetry = viewModel::retry)
                        }
                    }

                    NewsUiState.Loading -> {
                        // Show full screen loader only if no news is loaded and not just refreshing
                        if (!isRefreshing && viewModel.loadedNews.isEmpty()) {
                            println("UI: Displaying UI State: Loading (full screen).")
                            CenteredLoading()
                        } else {
                            // If refreshing or loading more, show existing news + appropriate indicators
                            NewsListContent(
                                news = viewModel.loadedNews,
                                listState = listState,
                                showLoading = viewModel.isLoadingNextPage,
                                paginationError = null,
                                hasMoreToLoad = (uiState as? NewsUiState.Success)?.hasMoreToLoad ?: true,
                                onRetryPagination = viewModel::retry,
                                imageLoader = imageLoader,
                                navController = navController
                            )
                        }
                    }

                    NewsUiState.NoData -> {
                        println("UI: Displaying UI State: NoData.")
                        val message = if (searchQuery.isNotBlank()) "No results found for \"$searchQuery\"" else "No news found"
                        CenteredMessage(message, onRetry = viewModel::retry)
                    }

                    is NewsUiState.Success -> {
                        val successState = uiState as NewsUiState.Success
                        println("UI: Displaying UI State: Success. Items count: ${successState.news.size}. Show loader: ${viewModel.isLoadingNextPage}. Has more to load: ${successState.hasMoreToLoad}")
                        NewsListContent(
                            news = successState.news,
                            listState = listState,
                            showLoading = viewModel.isLoadingNextPage,
                            paginationError = null,
                            hasMoreToLoad = successState.hasMoreToLoad,
                            onRetryPagination = viewModel::retry,
                            imageLoader = imageLoader,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

// Reusable Composable for the Search Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Search news...") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}

// Composable for displaying the list of news articles and pagination indicators/errors
@Composable
fun NewsListContent(
    news: List<News>,
    listState: LazyListState,
    showLoading: Boolean,
    paginationError: String?,
    hasMoreToLoad: Boolean,
    onRetryPagination: () -> Unit,
    imageLoader: ImageLoader,
    isMaxResultsReached: Boolean = false,
    navController: NavHostController
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        items(news, key = { it.id }) { item ->
            NewsItem(news = item, imageLoader = imageLoader, navController = navController)
        }

        item {
            // Footer content for pagination status
            when {
                showLoading -> {
                    println("NewsListContent: Loader condition TRUE. Displaying pagination loader.")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                isMaxResultsReached -> {
                    println("NewsListContent: Max results reached condition TRUE. Displaying message.")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = paginationError ?: "You've reached the maximum number of results for your plan (100 articles).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                paginationError != null -> {
                    println("NewsListContent: Pagination error condition TRUE. Displaying pagination error.")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = paginationError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onRetryPagination() }
                        )
                    }
                }
                !showLoading && news.isNotEmpty() && !hasMoreToLoad -> {
                    println("NewsListContent: 'No more news' condition TRUE. Displaying 'No more news' message.")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("You're up to date! No more news.")
                    }
                }
                // No else needed, as other conditions cover all states or result in no footer content
            }
        }
    }
}

// Composable for a single news item (no changes needed from your previous version)
@Composable
fun NewsItem(news: News, imageLoader: ImageLoader, navController: NavHostController) {
    Surface ( // Use Material3 Surface
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clip(shape = RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .clickable {
                val encodedUrl = URLEncoder.encode(news.url, StandardCharsets.UTF_8.toString())
                navController.navigate("news_detail_screen/${encodedUrl}")
            },
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (news.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(news.imageUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    onError = {
                        println("Image load failed for ${news.imageUrl}: ${it.result.throwable}")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = news.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = news.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Read more →",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.clickable {
                        // TODO: Implement read more click, e.g., open a browser with news.url
                    }
                )
            }
        }
    }
}


// Composable for displaying a centered message (e.g., error, no data)
@Composable
fun CenteredMessage(message: String, onRetry: () -> Unit) {
    println("Displaying CenteredMessage: $message")
    Box(modifier = Modifier.fillMaxSize()
        .padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// Composable for displaying a centered loading indicator
@Composable
fun CenteredLoading() {
    println("Displaying CenteredLoading")
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}