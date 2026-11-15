package com.example.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.FaceDao
import com.example.core.database.FaceGroupEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.SavedSearchDao
import com.example.core.database.SavedSearchEntity
import com.example.feature.search.engine.AdvancedSearchEngine
import com.example.feature.search.engine.ScoredMediaResult
import com.example.feature.search.engine.SearchExecutionReport
import com.example.feature.search.engine.SearchQuery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SearchUiState(
    val query: SearchQuery = SearchQuery(),
    val results: List<ScoredMediaResult> = emptyList(),
    val totalIndexedCount: Int = 0,
    val latencyMillis: Long = 0L,
    val expandedTerms: List<String> = emptyList(),
    val savedSearches: List<SavedSearchEntity> = emptyList(),
    val availableAlbums: List<String> = emptyList(),
    val availableFolders: List<String> = emptyList(),
    val availableFormats: List<String> = emptyList(),
    val availableFaceGroups: List<FaceGroupEntity> = emptyList(),
    val similarityAnchor: MediaIndexEntity? = null,
    val isSearching: Boolean = false,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchEngine: AdvancedSearchEngine,
    private val mediaIndexDao: MediaIndexDao,
    private val faceDao: FaceDao,
    private val savedSearchDao: SavedSearchDao,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val queryFlow = MutableStateFlow(SearchQuery())
    private val searchingFlow = MutableStateFlow(false)

    private val reportFlow = combine(
        queryFlow.debounce(110L),
        mediaIndexDao.observeTotalCount(),
    ) { q, _ -> q }.flatMapLatest { q ->
        flow {
            searchingFlow.value = true
            val report = searchEngine.search(q)
            val anchor = q.similarToMediaId?.let { mediaIndexDao.getById(it) }
            emit(report to anchor)
            searchingFlow.value = false
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        queryFlow,
        reportFlow,
        savedSearchDao.observeAllSavedSearches(),
        mediaIndexDao.observeDistinctAlbums(),
        mediaIndexDao.observeDistinctFolders(),
        mediaIndexDao.observeDistinctFormats(),
        faceDao.observeAllGroups(),
        searchingFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val q = values[0] as SearchQuery
        @Suppress("UNCHECKED_CAST")
        val reportPair = values[1] as Pair<SearchExecutionReport, MediaIndexEntity?>
        @Suppress("UNCHECKED_CAST")
        val saved = values[2] as List<SavedSearchEntity>
        @Suppress("UNCHECKED_CAST")
        val albums = values[3] as List<String>
        @Suppress("UNCHECKED_CAST")
        val folders = values[4] as List<String>
        @Suppress("UNCHECKED_CAST")
        val formats = values[5] as List<String>
        @Suppress("UNCHECKED_CAST")
        val faceGroups = values[6] as List<FaceGroupEntity>
        val searching = values[7] as Boolean

        val report = reportPair.first
        SearchUiState(
            query = q,
            results = report.results,
            totalIndexedCount = report.totalIndexedCount,
            latencyMillis = report.latencyMillis,
            expandedTerms = report.expandedTerms,
            savedSearches = saved,
            availableAlbums = albums,
            availableFolders = folders,
            availableFormats = formats,
            availableFaceGroups = faceGroups,
            similarityAnchor = reportPair.second,
            isSearching = searching,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(),
    )

    fun updateQuery(transform: (SearchQuery) -> SearchQuery) {
        queryFlow.value = transform(queryFlow.value)
    }

    fun resetFilters() {
        queryFlow.value = SearchQuery(useSemanticExpansion = queryFlow.value.useSemanticExpansion)
    }

    fun startVisualSearch(mediaId: Long) {
        queryFlow.value = SearchQuery(
            similarToMediaId = mediaId,
            similarityThreshold = 0.70f,
            useSemanticExpansion = queryFlow.value.useSemanticExpansion,
        )
    }

    fun filterByFaceGroup(groupId: String) {
        queryFlow.value = SearchQuery(
            faceGroupIds = listOf(groupId),
            useSemanticExpansion = queryFlow.value.useSemanticExpansion,
        )
    }

    fun applySmartRecommendationFilter(filterTag: String) {
        queryFlow.value = when (filterTag) {
            "documents" -> SearchQuery(documentsOnly = true)
            "screenshots" -> SearchQuery(screenshotsOnly = true)
            "blurry" -> SearchQuery(blurryOnly = true)
            "gps" -> SearchQuery(hasGpsOnly = true)
            "nature" -> SearchQuery(textQuery = "nature", useSemanticExpansion = true)
            else -> SearchQuery()
        }
    }

    fun saveCurrentSearch(name: String) {
        val current = queryFlow.value
        val trimmed = name.trim().ifEmpty { current.buildHumanSummary().take(32) }
        viewModelScope.launch {
            savedSearchDao.upsert(
                SavedSearchEntity(
                    name = trimmed,
                    summary = current.buildHumanSummary(),
                    queryJson = json.encodeToString(current),
                ),
            )
        }
    }

    fun applySavedSearch(entity: SavedSearchEntity) {
        try {
            val decoded = json.decodeFromString<SearchQuery>(entity.queryJson)
            queryFlow.value = decoded
            viewModelScope.launch { savedSearchDao.touchLastUsed(entity.id) }
        } catch (_: Throwable) {
            // Ignore malformed legacy entry
        }
    }

    fun togglePinSavedSearch(entity: SavedSearchEntity) {
        viewModelScope.launch {
            savedSearchDao.setPinned(entity.id, !entity.isPinned)
        }
    }

    fun deleteSavedSearch(id: Long) {
        viewModelScope.launch {
            savedSearchDao.deleteById(id)
        }
    }

    class Factory(
        private val searchEngine: AdvancedSearchEngine,
        private val mediaIndexDao: MediaIndexDao,
        private val faceDao: FaceDao,
        private val savedSearchDao: SavedSearchDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(
                searchEngine = searchEngine,
                mediaIndexDao = mediaIndexDao,
                faceDao = faceDao,
                savedSearchDao = savedSearchDao,
            ) as T
        }
    }
}
