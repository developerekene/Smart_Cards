package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardDifficulty
import com.example.data.CardEntity
import com.example.data.DeckEntity
import com.example.data.ExportFormat
import com.example.data.ImportExportManager
import com.example.data.SmartCardsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckDetailUiState(
    val deck: DeckEntity? = null,
    val allCards: List<CardEntity> = emptyList(),
    val filteredCards: List<CardEntity> = emptyList(),
    val filterDifficulty: CardDifficulty? = null,
    val searchQuery: String = "",
    val easyCount: Int = 0,
    val difficultCount: Int = 0,
    val unmarkedCount: Int = 0
)

class DeckDetailViewModel(
    private val deckId: Long,
    private val repository: SmartCardsRepository
) : ViewModel() {

    private val _filterDifficulty = MutableStateFlow<CardDifficulty?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<DeckDetailUiState> = combine(
        repository.observeDeckById(deckId),
        repository.observeCardsByDeck(deckId),
        _filterDifficulty,
        _searchQuery
    ) { deck, cards, filter, query ->
        val trimmed = query.trim().lowercase()
        val filtered = cards.filter { card ->
            val matchesDiff = filter == null || card.difficulty == filter
            val matchesQuery = trimmed.isEmpty() ||
                card.question.lowercase().contains(trimmed) ||
                card.answer.lowercase().contains(trimmed) ||
                card.topic.lowercase().contains(trimmed)
            matchesDiff && matchesQuery
        }

        DeckDetailUiState(
            deck = deck,
            allCards = cards,
            filteredCards = filtered,
            filterDifficulty = filter,
            searchQuery = query,
            easyCount = cards.count { it.difficulty == CardDifficulty.EASY },
            difficultCount = cards.count { it.difficulty == CardDifficulty.DIFFICULT },
            unmarkedCount = cards.count { it.difficulty == CardDifficulty.UNMARKED }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeckDetailUiState())

    fun setFilter(difficulty: CardDifficulty?) {
        _filterDifficulty.value = if (_filterDifficulty.value == difficulty) null else difficulty
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCard(question: String, answer: String, topic: String, difficulty: CardDifficulty) {
        if (question.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.addCard(deckId, question, answer, topic, difficulty)
            _snackbarEvents.tryEmit("Card added")
        }
    }

    fun updateCard(
        card: CardEntity,
        question: String,
        answer: String,
        topic: String,
        difficulty: CardDifficulty
    ) {
        if (question.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.updateCard(card, question, answer, topic, difficulty)
            _snackbarEvents.tryEmit("Card updated")
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId, deckId)
            _snackbarEvents.tryEmit("Card deleted")
        }
    }

    fun toggleDifficulty(card: CardEntity, target: CardDifficulty) {
        val next = if (card.difficulty == target) CardDifficulty.UNMARKED else target
        viewModelScope.launch {
            repository.setCardDifficulty(card.id, next)
        }
    }

    fun updateDeck(title: String, subject: String, description: String, accentIndex: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateDeck(deckId, title, subject, description, accentIndex)
            _snackbarEvents.tryEmit("Deck updated")
        }
    }

    suspend fun exportDeck(format: ExportFormat): String? {
        return repository.exportSingleDeck(deckId, format)
    }

    fun importCardsIntoDeck(rawContent: String) {
        viewModelScope.launch {
            try {
                val deckTitle = uiState.value.deck?.title ?: "Deck"
                val payloads = ImportExportManager.parseImportContent(rawContent, deckTitle)
                if (payloads.isEmpty()) {
                    _snackbarEvents.tryEmit("No valid cards found in file")
                    return@launch
                }
                val mergedPayload = if (payloads.size > 1) {
                    listOf(
                        payloads.first().copy(
                            title = deckTitle,
                            cards = payloads.flatMap { it.cards }
                        )
                    )
                } else {
                    payloads
                }
                val summary = repository.importPayloads(mergedPayload, targetDeckId = deckId)
                _snackbarEvents.tryEmit("Imported ${summary.cardsImported} cards into deck")
            } catch (e: Exception) {
                _snackbarEvents.tryEmit("Import failed: ${e.localizedMessage ?: "Invalid file"}")
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    class Factory(
        private val deckId: Long,
        private val repository: SmartCardsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeckDetailViewModel(deckId, repository) as T
        }
    }
}
