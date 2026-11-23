package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CardDifficulty
import com.example.data.ExportFormat
import com.example.data.ImportExportManager
import com.example.data.QuizMode
import com.example.data.SmartCardsDatabase
import com.example.data.SmartCardsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: SmartCardsDatabase
    private lateinit var repository: SmartCardsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SmartCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SmartCardsRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SmartCards", appName)
    }

    @Test
    fun `deck card lifecycle and difficulty tracking`() = runTest {
        val deckId = repository.createDeck("Physics 101", "Physics", "Mechanics", 0)
        val card1 = repository.addCard(deckId, "Unit of force?", "Newton", "Dynamics")
        val card2 = repository.addCard(deckId, "Acceleration of gravity?", "9.8 m/s^2", "Kinematics")

        repository.markCardReviewed(card1, CardDifficulty.EASY)
        repository.markCardReviewed(card2, CardDifficulty.DIFFICULT)

        val decks = repository.observeDecksWithStats().first()
        assertEquals(1, decks.size)
        assertEquals(2, decks.first().totalCards)
        assertEquals(1, decks.first().easyCards)
        assertEquals(1, decks.first().difficultCards)

        val difficult = repository.getCardsForStudy(deckId, difficultOnly = true)
        assertEquals(1, difficult.size)
        assertEquals("Acceleration of gravity?", difficult.first().question)
    }

    @Test
    fun `json and csv export import roundtrip`() = runTest {
        val deckId = repository.createDeck("Spanish Verbs", "Languages", "Common verbs", 2)
        repository.addCard(deckId, "Hablar", "To speak, chat", "Regular -ar", CardDifficulty.EASY)
        repository.addCard(deckId, "Comer", "To eat", "Regular -er", CardDifficulty.DIFFICULT)

        val json = repository.exportAllDecks(ExportFormat.JSON)
        val parsedJson = ImportExportManager.parseImportContent(json)
        assertEquals(1, parsedJson.size)
        assertEquals("Spanish Verbs", parsedJson.first().title)
        assertEquals(2, parsedJson.first().cards.size)

        val csv = repository.exportAllDecks(ExportFormat.CSV)
        val parsedCsv = ImportExportManager.parseImportContent(csv)
        assertEquals(1, parsedCsv.size)
        assertEquals(2, parsedCsv.first().cards.size)
        assertTrue(parsedCsv.first().cards.any { it.answer == "To speak, chat" && it.difficulty == CardDifficulty.EASY })
    }

    @Test
    fun `quiz session persistence records score`() = runTest {
        val deckId = repository.createDeck("Math", "Mathematics", "", 0)
        repository.recordQuizSession(deckId, "Math", QuizMode.TRUE_FALSE, 8, 10, 42)
        val sessions = repository.observeAllQuizSessions().first()
        assertEquals(1, sessions.size)
        assertEquals(80, sessions.first().percentage)
        assertEquals(QuizMode.TRUE_FALSE, sessions.first().quizMode)
    }
}
