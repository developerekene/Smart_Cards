package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class ImportedDeckPayload(
    val title: String,
    val subject: String,
    val description: String,
    val accentIndex: Int,
    val cards: List<ImportedCardPayload>
)

data class ImportedCardPayload(
    val question: String,
    val answer: String,
    val topic: String,
    val difficulty: CardDifficulty
)

data class ImportSummary(
    val decksCreated: Int,
    val cardsImported: Int
)

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    JSON("json", "application/json", "JSON (.json)"),
    CSV("csv", "text/csv", "CSV (.csv)")
}

object ImportExportManager {

    fun exportDecksToJson(decks: List<Pair<DeckEntity, List<CardEntity>>>): String {
        val root = JSONObject()
        root.put("app", "SmartCards")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val decksArray = JSONArray()
        decks.forEach { (deck, cards) ->
            val deckObj = JSONObject().apply {
                put("title", deck.title)
                put("subject", deck.subject)
                put("description", deck.description)
                put("accentIndex", deck.accentIndex)
                val cardsArray = JSONArray()
                cards.forEach { card ->
                    cardsArray.put(
                        JSONObject().apply {
                            put("question", card.question)
                            put("answer", card.answer)
                            put("topic", card.topic)
                            put("difficulty", card.difficulty.name)
                        }
                    )
                }
                put("cards", cardsArray)
            }
            decksArray.put(deckObj)
        }
        root.put("decks", decksArray)
        return root.toString(2)
    }

    fun exportDecksToCsv(decks: List<Pair<DeckEntity, List<CardEntity>>>): String {
        val sb = StringBuilder()
        sb.appendLine("Deck,Subject,Question,Answer,Topic,Difficulty")
        decks.forEach { (deck, cards) ->
            cards.forEach { card ->
                sb.append(escapeCsv(deck.title)).append(',')
                sb.append(escapeCsv(deck.subject)).append(',')
                sb.append(escapeCsv(card.question)).append(',')
                sb.append(escapeCsv(card.answer)).append(',')
                sb.append(escapeCsv(card.topic)).append(',')
                sb.appendLine(escapeCsv(card.difficulty.name))
            }
        }
        return sb.toString()
    }

    fun parseImportContent(rawContent: String, fallbackDeckTitle: String = "Imported Deck"): List<ImportedDeckPayload> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            parseJson(trimmed, fallbackDeckTitle)
        } else {
            parseCsv(trimmed, fallbackDeckTitle)
        }
    }

    private fun parseJson(content: String, fallbackDeckTitle: String): List<ImportedDeckPayload> {
        val trimmed = content.trim()
        val result = mutableListOf<ImportedDeckPayload>()

        if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            val cards = mutableListOf<ImportedCardPayload>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.has("cards")) {
                    parseDeckObject(obj)?.let { result.add(it) }
                } else {
                    parseCardObject(obj)?.let { cards.add(it) }
                }
            }
            if (cards.isNotEmpty()) {
                result.add(
                    ImportedDeckPayload(
                        title = fallbackDeckTitle,
                        subject = "Imported",
                        description = "Imported flashcards",
                        accentIndex = 0,
                        cards = cards
                    )
                )
            }
            return result
        }

        val root = JSONObject(trimmed)
        when {
            root.has("decks") -> {
                val decksArray = root.getJSONArray("decks")
                for (i in 0 until decksArray.length()) {
                    val deckObj = decksArray.optJSONObject(i) ?: continue
                    parseDeckObject(deckObj)?.let { result.add(it) }
                }
            }
            root.has("cards") -> {
                parseDeckObject(root, fallbackDeckTitle)?.let { result.add(it) }
            }
            root.has("question") && root.has("answer") -> {
                parseCardObject(root)?.let { card ->
                    result.add(
                        ImportedDeckPayload(
                            title = fallbackDeckTitle,
                            subject = "Imported",
                            description = "",
                            accentIndex = 0,
                            cards = listOf(card)
                        )
                    )
                }
            }
        }
        return result
    }

    private fun parseDeckObject(obj: JSONObject, fallbackTitle: String = "Imported Deck"): ImportedDeckPayload? {
        val title = obj.optString("title").trim().ifEmpty { fallbackTitle }
        val subject = obj.optString("subject").trim().ifEmpty { "General" }
        val description = obj.optString("description").trim()
        val accentIndex = obj.optInt("accentIndex", 0).coerceIn(0, 5)
        val cardsArray = obj.optJSONArray("cards") ?: JSONArray()
        val cards = mutableListOf<ImportedCardPayload>()
        for (i in 0 until cardsArray.length()) {
            val cardObj = cardsArray.optJSONObject(i) ?: continue
            parseCardObject(cardObj)?.let { cards.add(it) }
        }
        if (cards.isEmpty() && title == fallbackTitle) return null
        return ImportedDeckPayload(
            title = title,
            subject = subject,
            description = description,
            accentIndex = accentIndex,
            cards = cards
        )
    }

    private fun parseCardObject(obj: JSONObject): ImportedCardPayload? {
        val question = obj.optString("question").trim()
        val answer = obj.optString("answer").trim()
        if (question.isEmpty() || answer.isEmpty()) return null
        val topic = obj.optString("topic").trim()
        val diffRaw = obj.optString("difficulty").trim().uppercase()
        val difficulty = enumValues<CardDifficulty>().firstOrNull { it.name == diffRaw }
            ?: CardDifficulty.UNMARKED
        return ImportedCardPayload(
            question = question,
            answer = answer,
            topic = topic,
            difficulty = difficulty
        )
    }

    private fun parseCsv(content: String, fallbackDeckTitle: String): List<ImportedDeckPayload> {
        val rows = parseCsvRows(content).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()

        val firstRowLower = rows.first().map { it.trim().lowercase() }
        val hasHeader = firstRowLower.contains("question") || firstRowLower.contains("answer")
        val dataRows = if (hasHeader) rows.drop(1) else rows
        if (dataRows.isEmpty()) return emptyList()

        // Detect column mapping from header if present, otherwise infer from column count
        val grouped = linkedMapOf<Pair<String, String>, MutableList<ImportedCardPayload>>()

        if (hasHeader) {
            val deckIdx = firstRowLower.indexOfFirst { it == "deck" || it == "deck_title" }
            val subjectIdx = firstRowLower.indexOfFirst { it == "subject" }
            val qIdx = firstRowLower.indexOfFirst { it == "question" || it == "front" || it == "term" }
            val aIdx = firstRowLower.indexOfFirst { it == "answer" || it == "back" || it == "definition" }
            val topicIdx = firstRowLower.indexOfFirst { it == "topic" || it == "tag" }
            val diffIdx = firstRowLower.indexOfFirst { it == "difficulty" }

            if (qIdx < 0 || aIdx < 0) return emptyList()

            dataRows.forEach { row ->
                val q = row.getOrNull(qIdx)?.trim().orEmpty()
                val a = row.getOrNull(aIdx)?.trim().orEmpty()
                if (q.isEmpty() || a.isEmpty()) return@forEach
                val deckTitle = row.getOrNull(deckIdx)?.trim()?.ifEmpty { fallbackDeckTitle } ?: fallbackDeckTitle
                val subject = row.getOrNull(subjectIdx)?.trim()?.ifEmpty { "Imported" } ?: "Imported"
                val topic = row.getOrNull(topicIdx)?.trim().orEmpty()
                val diffRaw = row.getOrNull(diffIdx)?.trim()?.uppercase().orEmpty()
                val diff = enumValues<CardDifficulty>().firstOrNull { it.name == diffRaw }
                    ?: CardDifficulty.UNMARKED
                grouped.getOrPut(deckTitle to subject) { mutableListOf() }.add(
                    ImportedCardPayload(q, a, topic, diff)
                )
            }
        } else {
            dataRows.forEach { row ->
                when {
                    row.size >= 4 -> {
                        val deckTitle = row[0].trim().ifEmpty { fallbackDeckTitle }
                        val subject = row[1].trim().ifEmpty { "Imported" }
                        val q = row[2].trim()
                        val a = row[3].trim()
                        val topic = row.getOrNull(4)?.trim().orEmpty()
                        val diffRaw = row.getOrNull(5)?.trim()?.uppercase().orEmpty()
                        val diff = enumValues<CardDifficulty>().firstOrNull { it.name == diffRaw }
                            ?: CardDifficulty.UNMARKED
                        if (q.isNotEmpty() && a.isNotEmpty()) {
                            grouped.getOrPut(deckTitle to subject) { mutableListOf() }.add(
                                ImportedCardPayload(q, a, topic, diff)
                            )
                        }
                    }
                    row.size >= 2 -> {
                        val q = row[0].trim()
                        val a = row[1].trim()
                        val topic = row.getOrNull(2)?.trim().orEmpty()
                        if (q.isNotEmpty() && a.isNotEmpty()) {
                            grouped.getOrPut(fallbackDeckTitle to "Imported") { mutableListOf() }.add(
                                ImportedCardPayload(q, a, topic, CardDifficulty.UNMARKED)
                            )
                        }
                    }
                }
            }
        }

        return grouped.entries.mapIndexed { idx, (key, cards) ->
            ImportedDeckPayload(
                title = key.first,
                subject = key.second,
                description = "Imported from CSV",
                accentIndex = idx % 6,
                cards = cards
            )
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvRows(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < input.length) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < input.length && input[i + 1] == '\n') i++
                        currentRow.add(currentField.toString())
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                        currentField.setLength(0)
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                        currentField.setLength(0)
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }
        return rows
    }
}
