package com.example.data

import kotlinx.coroutines.flow.Flow

class SmartCardsRepository(private val database: SmartCardsDatabase) {
    private val deckDao = database.deckDao()
    private val cardDao = database.cardDao()
    private val quizSessionDao = database.quizSessionDao()

    fun observeDecksWithStats(): Flow<List<DeckWithStats>> = deckDao.observeDecksWithStats()

    fun observeDeckById(deckId: Long): Flow<DeckEntity?> = deckDao.observeDeckById(deckId)

    suspend fun getDeckById(deckId: Long): DeckEntity? = deckDao.getDeckById(deckId)

    fun observeCardsByDeck(deckId: Long): Flow<List<CardEntity>> = cardDao.observeCardsByDeck(deckId)

    fun observeAllCards(): Flow<List<CardEntity>> = cardDao.observeAllCards()

    fun observeAllCardsWithDeck(): Flow<List<CardWithDeck>> = cardDao.observeAllCardsWithDeck()

    fun observeDifficultCards(): Flow<List<CardWithDeck>> = cardDao.observeDifficultCards()

    fun observeAllQuizSessions(): Flow<List<QuizSessionEntity>> = quizSessionDao.observeAllSessions()

    suspend fun getCardsForStudy(deckId: Long?, difficultOnly: Boolean): List<CardEntity> {
        return when {
            deckId != null && difficultOnly -> cardDao.getDifficultCardsByDeckOnce(deckId)
            deckId != null -> cardDao.getCardsByDeckOnce(deckId)
            difficultOnly -> cardDao.getAllDifficultCardsOnce()
            else -> cardDao.getAllCardsOnce()
        }
    }

    suspend fun getAllCardsOnce(): List<CardEntity> = cardDao.getAllCardsOnce()

    suspend fun createDeck(
        title: String,
        subject: String,
        description: String,
        accentIndex: Int
    ): Long {
        val now = System.currentTimeMillis()
        return deckDao.insertDeck(
            DeckEntity(
                title = title.trim(),
                subject = subject.trim().ifEmpty { "General" },
                description = description.trim(),
                accentIndex = accentIndex.coerceIn(0, 5),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateDeck(
        deckId: Long,
        title: String,
        subject: String,
        description: String,
        accentIndex: Int
    ) {
        val existing = deckDao.getDeckById(deckId) ?: return
        deckDao.updateDeck(
            existing.copy(
                title = title.trim(),
                subject = subject.trim().ifEmpty { "General" },
                description = description.trim(),
                accentIndex = accentIndex.coerceIn(0, 5),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteDeck(deckId: Long) {
        deckDao.deleteDeckById(deckId)
    }

    suspend fun addCard(
        deckId: Long,
        question: String,
        answer: String,
        topic: String,
        difficulty: CardDifficulty = CardDifficulty.UNMARKED
    ): Long {
        val id = cardDao.insertCard(
            CardEntity(
                deckId = deckId,
                question = question.trim(),
                answer = answer.trim(),
                topic = topic.trim(),
                difficulty = difficulty
            )
        )
        deckDao.touchDeck(deckId)
        return id
    }

    suspend fun updateCard(
        card: CardEntity,
        question: String,
        answer: String,
        topic: String,
        difficulty: CardDifficulty
    ) {
        cardDao.updateCard(
            card.copy(
                question = question.trim(),
                answer = answer.trim(),
                topic = topic.trim(),
                difficulty = difficulty
            )
        )
        deckDao.touchDeck(card.deckId)
    }

    suspend fun deleteCard(cardId: Long, deckId: Long) {
        cardDao.deleteCardById(cardId)
        deckDao.touchDeck(deckId)
    }

    suspend fun markCardReviewed(cardId: Long, difficulty: CardDifficulty) {
        cardDao.markCardReviewed(cardId, difficulty, System.currentTimeMillis())
    }

    suspend fun setCardDifficulty(cardId: Long, difficulty: CardDifficulty) {
        cardDao.setCardDifficulty(cardId, difficulty)
    }

    suspend fun recordQuizSession(
        deckId: Long?,
        deckTitle: String,
        quizMode: QuizMode,
        correctCount: Int,
        totalCount: Int,
        durationSeconds: Int
    ) {
        quizSessionDao.insertSession(
            QuizSessionEntity(
                deckId = deckId,
                deckTitle = deckTitle,
                quizMode = quizMode,
                correctCount = correctCount,
                totalCount = totalCount,
                durationSeconds = durationSeconds
            )
        )
    }

    suspend fun exportAllDecks(format: ExportFormat): String {
        val decks = deckDao.getAllDecksOnce()
        val pairs = decks.map { deck -> deck to cardDao.getCardsByDeckOnce(deck.id) }
        return when (format) {
            ExportFormat.JSON -> ImportExportManager.exportDecksToJson(pairs)
            ExportFormat.CSV -> ImportExportManager.exportDecksToCsv(pairs)
        }
    }

    suspend fun exportSingleDeck(deckId: Long, format: ExportFormat): String? {
        val deck = deckDao.getDeckById(deckId) ?: return null
        val cards = cardDao.getCardsByDeckOnce(deckId)
        val pairs = listOf(deck to cards)
        return when (format) {
            ExportFormat.JSON -> ImportExportManager.exportDecksToJson(pairs)
            ExportFormat.CSV -> ImportExportManager.exportDecksToCsv(pairs)
        }
    }

    suspend fun importPayloads(payloads: List<ImportedDeckPayload>, targetDeckId: Long? = null): ImportSummary {
        var decksCreated = 0
        var cardsImported = 0
        val now = System.currentTimeMillis()

        payloads.forEach { payload ->
            val resolvedDeckId = if (targetDeckId != null && payloads.size == 1) {
                deckDao.touchDeck(targetDeckId, now)
                targetDeckId
            } else {
                decksCreated++
                deckDao.insertDeck(
                    DeckEntity(
                        title = payload.title,
                        subject = payload.subject,
                        description = payload.description,
                        accentIndex = payload.accentIndex,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            val entities = payload.cards.mapIndexed { idx, c ->
                CardEntity(
                    deckId = resolvedDeckId,
                    question = c.question,
                    answer = c.answer,
                    topic = c.topic,
                    difficulty = c.difficulty,
                    createdAt = now + idx
                )
            }
            if (entities.isNotEmpty()) {
                cardDao.insertCards(entities)
                cardsImported += entities.size
            }
        }
        return ImportSummary(decksCreated = decksCreated, cardsImported = cardsImported)
    }

    suspend fun loadStarterDecks() {
        val now = System.currentTimeMillis()
        val starterPacks = listOf(
            Triple(
                DeckEntity(
                    title = "Human Anatomy & Cell Biology",
                    subject = "Biology",
                    description = "Core cellular organelles, physiology, and human body systems.",
                    accentIndex = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                listOf(
                    Triple("Which organelle generates most of the cell's ATP?", "Mitochondria", "Cell Biology"),
                    Triple("What protein in red blood cells carries oxygen?", "Hemoglobin", "Circulatory"),
                    Triple("Which part of the brain coordinates balance and fine motor control?", "Cerebellum", "Neurology"),
                    Triple("What is the functional filtration unit of the kidney?", "Nephron", "Renal System"),
                    Triple("Which blood vessels carry blood away from the heart?", "Arteries", "Circulatory"),
                    Triple("What process converts mRNA into a polypeptide chain?", "Translation", "Genetics")
                ),
                CardDifficulty.UNMARKED
            ),
            Triple(
                DeckEntity(
                    title = "World Capitals & Landmarks",
                    subject = "Geography",
                    description = "Capitals, straits, mountain ranges, and global geography trivia.",
                    accentIndex = 0,
                    createdAt = now - 1000,
                    updatedAt = now - 1000
                ),
                listOf(
                    Triple("What is the capital city of Australia?", "Canberra", "Oceania"),
                    Triple("Which river flows through Cairo?", "Nile River", "Africa"),
                    Triple("What is the capital city of Canada?", "Ottawa", "North America"),
                    Triple("Which mountain range separates Europe from Asia?", "Ural Mountains", "Eurasia"),
                    Triple("What is the smallest independent country in the world by area?", "Vatican City", "Europe"),
                    Triple("Which strait separates Spain from Morocco?", "Strait of Gibraltar", "Waterways")
                ),
                CardDifficulty.UNMARKED
            ),
            Triple(
                DeckEntity(
                    title = "Kotlin & Jetpack Compose",
                    subject = "Computer Science",
                    description = "Modern Kotlin coroutines, state management, and Compose essentials.",
                    accentIndex = 3,
                    createdAt = now - 2000,
                    updatedAt = now - 2000
                ),
                listOf(
                    Triple("Which coroutine builder returns a Deferred result?", "async", "Coroutines"),
                    Triple("Which Compose API preserves state across configuration changes?", "rememberSaveable", "Compose"),
                    Triple("What Kotlin keyword marks a function that can pause without blocking a thread?", "suspend", "Coroutines"),
                    Triple("Which Flow operator converts a cold Flow into a hot StateFlow?", "stateIn", "StateFlow"),
                    Triple("Which modifier makes a Composable scrollable only when needed?", "verticalScroll", "Layouts")
                ),
                CardDifficulty.UNMARKED
            )
        )

        starterPacks.forEach { (deck, cards, defaultDiff) ->
            val deckId = deckDao.insertDeck(deck)
            val cardEntities = cards.mapIndexed { idx, (q, a, topic) ->
                CardEntity(
                    deckId = deckId,
                    question = q,
                    answer = a,
                    topic = topic,
                    difficulty = if (idx == 0) CardDifficulty.EASY else if (idx == 2) CardDifficulty.DIFFICULT else defaultDiff,
                    createdAt = now + idx
                )
            }
            cardDao.insertCards(cardEntities)
        }
    }
}
