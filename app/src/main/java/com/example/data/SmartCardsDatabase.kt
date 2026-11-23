package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query(
        """
        SELECT 
            d.id,
            d.title,
            d.subject,
            d.description,
            d.accentIndex,
            d.createdAt,
            d.updatedAt,
            COUNT(c.id) AS totalCards,
            COALESCE(SUM(CASE WHEN c.difficulty = 'EASY' THEN 1 ELSE 0 END), 0) AS easyCards,
            COALESCE(SUM(CASE WHEN c.difficulty = 'DIFFICULT' THEN 1 ELSE 0 END), 0) AS difficultCards,
            COALESCE(SUM(CASE WHEN c.difficulty = 'UNMARKED' THEN 1 ELSE 0 END), 0) AS unmarkedCards
        FROM decks d
        LEFT JOIN cards c ON d.id = c.deckId
        GROUP BY d.id
        ORDER BY d.updatedAt DESC
        """
    )
    fun observeDecksWithStats(): Flow<List<DeckWithStats>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun observeDeckById(deckId: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks ORDER BY updatedAt DESC")
    suspend fun getAllDecksOnce(): List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("UPDATE decks SET updatedAt = :updatedAt WHERE id = :deckId")
    suspend fun touchDeck(deckId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun observeCardsByDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt ASC")
    suspend fun getCardsByDeckOnce(deckId: Long): List<CardEntity>

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY createdAt ASC")
    suspend fun getAllCardsOnce(): List<CardEntity>

    @Query(
        """
        SELECT 
            c.id,
            c.deckId,
            d.title AS deckTitle,
            d.subject AS deckSubject,
            c.question,
            c.answer,
            c.topic,
            c.difficulty,
            c.reviewCount,
            c.lastReviewedAt,
            c.createdAt
        FROM cards c
        INNER JOIN decks d ON c.deckId = d.id
        ORDER BY c.createdAt DESC
        """
    )
    fun observeAllCardsWithDeck(): Flow<List<CardWithDeck>>

    @Query(
        """
        SELECT 
            c.id,
            c.deckId,
            d.title AS deckTitle,
            d.subject AS deckSubject,
            c.question,
            c.answer,
            c.topic,
            c.difficulty,
            c.reviewCount,
            c.lastReviewedAt,
            c.createdAt
        FROM cards c
        INNER JOIN decks d ON c.deckId = d.id
        WHERE c.difficulty = 'DIFFICULT'
        ORDER BY c.lastReviewedAt ASC, c.createdAt DESC
        """
    )
    fun observeDifficultCards(): Flow<List<CardWithDeck>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND difficulty = 'DIFFICULT' ORDER BY createdAt ASC")
    suspend fun getDifficultCardsByDeckOnce(deckId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE difficulty = 'DIFFICULT' ORDER BY createdAt ASC")
    suspend fun getAllDifficultCardsOnce(): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query(
        """
        UPDATE cards 
        SET difficulty = :difficulty, 
            reviewCount = reviewCount + 1, 
            lastReviewedAt = :reviewedAt 
        WHERE id = :cardId
        """
    )
    suspend fun markCardReviewed(
        cardId: Long,
        difficulty: CardDifficulty,
        reviewedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE cards SET difficulty = :difficulty WHERE id = :cardId")
    suspend fun setCardDifficulty(cardId: Long, difficulty: CardDifficulty)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)
}

@Dao
interface QuizSessionDao {
    @Query("SELECT * FROM quiz_sessions ORDER BY completedAt DESC")
    fun observeAllSessions(): Flow<List<QuizSessionEntity>>

    @Query("SELECT * FROM quiz_sessions WHERE deckId = :deckId ORDER BY completedAt DESC")
    fun observeSessionsForDeck(deckId: Long): Flow<List<QuizSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QuizSessionEntity): Long

    @Query("DELETE FROM quiz_sessions")
    suspend fun clearAllSessions()
}

@Database(
    entities = [DeckEntity::class, CardEntity::class, QuizSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SmartCardsConverters::class)
abstract class SmartCardsDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun quizSessionDao(): QuizSessionDao

    companion object {
        @Volatile
        private var INSTANCE: SmartCardsDatabase? = null

        fun getInstance(context: Context): SmartCardsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartCardsDatabase::class.java,
                    "smartcards.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
