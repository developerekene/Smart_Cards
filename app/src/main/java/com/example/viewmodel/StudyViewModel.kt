package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CardDifficulty
import com.example.data.CardEntity
import com.example.data.SmartCardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUiState(
    val title: String = "Study Session",
    val isLoading: Boolean = true,
    val cards: List<CardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isShuffled: Boolean = false,
    val difficultOnly: Boolean = false,
    val sessionEasyCount: Int = 0,
    val sessionDifficultCount: Int = 0,
    val isCompleted: Boolean = false
) {
    val currentCard: CardEntity?
        get() = cards.getOrNull(currentIndex)
}

class StudyViewModel(
    private val deckId: Long?,
    initialDifficultOnly: Boolean,
    initialShuffled: Boolean,
    private val repository: SmartCardsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StudyUiState(
            isShuffled = initialShuffled,
            difficultOnly = initialDifficultOnly
        )
    )
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        loadSession(difficultOnly = initialDifficultOnly, shuffled = initialShuffled)
    }

    fun loadSession(difficultOnly: Boolean, shuffled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isCompleted = false) }
            val deckTitle = if (deckId != null) {
                repository.getDeckById(deckId)?.title ?: "Deck Study"
            } else if (difficultOnly) {
                "Difficult Cards Review"
            } else {
                "All Decks Mix"
            }

            val rawCards = repository.getCardsForStudy(deckId, difficultOnly)
            val ordered = if (shuffled) rawCards.shuffled() else rawCards

            _uiState.update {
                it.copy(
                    title = deckTitle,
                    isLoading = false,
                    cards = ordered,
                    currentIndex = 0,
                    isFlipped = false,
                    isShuffled = shuffled,
                    difficultOnly = difficultOnly,
                    sessionEasyCount = 0,
                    sessionDifficultCount = 0,
                    isCompleted = false
                )
            }
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun toggleShuffle() {
        val state = _uiState.value
        val nextShuffled = !state.isShuffled
        val remaining = if (nextShuffled) state.cards.shuffled() else state.cards.sortedBy { it.createdAt }
        _uiState.update {
            it.copy(
                cards = remaining,
                currentIndex = 0,
                isFlipped = false,
                isShuffled = nextShuffled,
                isCompleted = false
            )
        }
    }

    fun markAndAdvance(difficulty: CardDifficulty) {
        val state = _uiState.value
        val card = state.currentCard ?: return

        viewModelScope.launch {
            repository.markCardReviewed(card.id, difficulty)
        }

        val updatedCards = state.cards.toMutableList()
        updatedCards[state.currentIndex] = card.copy(
            difficulty = difficulty,
            reviewCount = card.reviewCount + 1
        )

        val nextIndex = state.currentIndex + 1
        val completed = nextIndex >= updatedCards.size

        _uiState.update {
            it.copy(
                cards = updatedCards,
                currentIndex = if (completed) state.currentIndex else nextIndex,
                isFlipped = false,
                sessionEasyCount = it.sessionEasyCount + if (difficulty == CardDifficulty.EASY) 1 else 0,
                sessionDifficultCount = it.sessionDifficultCount + if (difficulty == CardDifficulty.DIFFICULT) 1 else 0,
                isCompleted = completed
            )
        }
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.cards.isEmpty()) return
        if (state.currentIndex + 1 >= state.cards.size) {
            _uiState.update { it.copy(isCompleted = true) }
        } else {
            _uiState.update {
                it.copy(currentIndex = it.currentIndex + 1, isFlipped = false)
            }
        }
    }

    fun previousCard() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.update {
                it.copy(currentIndex = it.currentIndex - 1, isFlipped = false)
            }
        }
    }

    class Factory(
        private val deckId: Long?,
        private val difficultOnly: Boolean,
        private val shuffled: Boolean,
        private val repository: SmartCardsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyViewModel(deckId, difficultOnly, shuffled, repository) as T
        }
    }
}
