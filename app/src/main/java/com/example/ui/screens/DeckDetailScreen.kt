package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CardDifficulty
import com.example.data.CardEntity
import com.example.data.ExportFormat
import com.example.data.QuizMode
import com.example.ui.components.CardEditorDialog
import com.example.ui.components.DeckEditorDialog
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.QuizSetupDialog
import com.example.ui.components.SubjectPill
import com.example.ui.theme.DeckAccentPalette
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.DeckDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    uiState: DeckDetailUiState,
    onBack: () -> Unit,
    onUpdateDeck: (String, String, String, Int) -> Unit,
    onAddCard: (String, String, String, CardDifficulty) -> Unit,
    onUpdateCard: (CardEntity, String, String, String, CardDifficulty) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onToggleDifficulty: (CardEntity, CardDifficulty) -> Unit,
    onSetFilter: (CardDifficulty?) -> Unit,
    onSearchChange: (String) -> Unit,
    onStartStudy: (difficultOnly: Boolean, shuffled: Boolean) -> Unit,
    onStartQuiz: (mode: QuizMode, difficultOnly: Boolean, limit: Int) -> Unit,
    onImportIntoDeck: () -> Unit,
    onExportDeck: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCard by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardEntity?>(null) }
    var deletingCard by remember { mutableStateOf<CardEntity?>(null) }
    var showEditDeck by remember { mutableStateOf(false) }
    var showQuizSetup by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val deck = uiState.deck
    val ext = LocalSmartCardsColors.current
    val accent = DeckAccentPalette[(deck?.accentIndex ?: 0).coerceIn(0, DeckAccentPalette.lastIndex)]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = deck?.title ?: "Deck",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deck_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportExport = true }) {
                        Icon(Icons.Default.ImportExport, contentDescription = "Import or Export Deck")
                    }
                    if (deck != null) {
                        IconButton(onClick = { showEditDeck = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Deck")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCard = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Flashcard")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (deck != null) {
                    item(key = "deck_summary") {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SubjectPill(subject = deck.subject, accentColor = accent)
                                    Text(
                                        text = "${uiState.allCards.size} total cards",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (deck.description.isNotBlank()) {
                                    Text(
                                        text = deck.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val mastery = if (uiState.allCards.isNotEmpty()) {
                                    uiState.easyCount.toFloat() / uiState.allCards.size.toFloat()
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { mastery },
                                    color = ext.easy,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${uiState.easyCount} Easy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.easy
                                    )
                                    Text(
                                        text = "${uiState.difficultCount} Difficult",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.difficult
                                    )
                                    Text(
                                        text = "${uiState.unmarkedCount} New",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onStartStudy(false, false) },
                                        enabled = uiState.allCards.isNotEmpty(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("deck_study_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Study")
                                    }

                                    OutlinedButton(
                                        onClick = { onStartStudy(false, true) },
                                        enabled = uiState.allCards.isNotEmpty(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("deck_shuffle_study_button")
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Shuffle")
                                    }

                                    Button(
                                        onClick = { showQuizSetup = true },
                                        enabled = uiState.allCards.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("deck_quiz_button")
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Quiz")
                                    }
                                }

                                if (uiState.difficultCount > 0) {
                                    OutlinedButton(
                                        onClick = { onStartStudy(true, true) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.difficult),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("deck_review_difficult_button")
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Review ${uiState.difficultCount} Difficult Card(s)")
                                    }
                                }
                            }
                        }
                    }
                }

                // Search + Difficulty Filter
                if (uiState.allCards.isNotEmpty()) {
                    item(key = "card_search") {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search cards in this deck…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deck_card_search_input")
                        )
                    }

                    item(key = "card_filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == null,
                                    onClick = { onSetFilter(null) },
                                    label = { Text("All (${uiState.allCards.size})") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == CardDifficulty.DIFFICULT,
                                    onClick = { onSetFilter(CardDifficulty.DIFFICULT) },
                                    label = { Text("Difficult (${uiState.difficultCount})") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == CardDifficulty.EASY,
                                    onClick = { onSetFilter(CardDifficulty.EASY) },
                                    label = { Text("Easy (${uiState.easyCount})") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == CardDifficulty.UNMARKED,
                                    onClick = { onSetFilter(CardDifficulty.UNMARKED) },
                                    label = { Text("New (${uiState.unmarkedCount})") }
                                )
                            }
                        }
                    }
                }

                if (uiState.allCards.isEmpty()) {
                    item(key = "empty_cards") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("No flashcards in this deck yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Tap + to add custom questions and answers, or import a JSON/CSV file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { showAddCard = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add First Card")
                                }
                            }
                        }
                    }
                } else if (uiState.filteredCards.isEmpty()) {
                    item(key = "no_filtered_cards") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No cards match your filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.filteredCards, key = { it.id }) { card ->
                        CardListItem(
                            card = card,
                            onMarkEasy = { onToggleDifficulty(card, CardDifficulty.EASY) },
                            onMarkDifficult = { onToggleDifficulty(card, CardDifficulty.DIFFICULT) },
                            onEdit = { editingCard = card },
                            onDelete = { deletingCard = card }
                        )
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddCard) {
        CardEditorDialog(
            onDismiss = { showAddCard = false },
            onSave = { q, a, topic, diff ->
                onAddCard(q, a, topic, diff)
                showAddCard = false
            }
        )
    }

    editingCard?.let { card ->
        CardEditorDialog(
            initialCard = card,
            onDismiss = { editingCard = null },
            onSave = { q, a, topic, diff ->
                onUpdateCard(card, q, a, topic, diff)
                editingCard = null
            }
        )
    }

    deletingCard?.let { card ->
        AlertDialog(
            onDismissRequest = { deletingCard = null },
            title = { Text("Delete Card?") },
            text = { Text("Remove this flashcard from the deck?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(card.id)
                        deletingCard = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCard = null }) { Text("Cancel") }
            }
        )
    }

    if (showEditDeck && deck != null) {
        DeckEditorDialog(
            initialDeck = deck,
            onDismiss = { showEditDeck = false },
            onSave = { title, subject, desc, accentIdx ->
                onUpdateDeck(title, subject, desc, accentIdx)
                showEditDeck = false
            }
        )
    }

    if (showQuizSetup && deck != null) {
        QuizSetupDialog(
            deckTitle = deck.title,
            totalCards = uiState.allCards.size,
            difficultCards = uiState.difficultCount,
            onDismiss = { showQuizSetup = false },
            onStartQuiz = { mode, diffOnly, limit ->
                showQuizSetup = false
                onStartQuiz(mode, diffOnly, limit)
            }
        )
    }

    if (showImportExport) {
        ImportExportDialog(
            title = "Deck Import & Export",
            hasCardsToExport = uiState.allCards.isNotEmpty(),
            onDismiss = { showImportExport = false },
            onImportClick = onImportIntoDeck,
            onExportClick = onExportDeck
        )
    }
}

@Composable
private fun CardListItem(
    card: CardEntity,
    onMarkEasy: () -> Unit,
    onMarkDifficult: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalSmartCardsColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashcard_item_${card.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(difficulty = card.difficulty)
                    if (card.topic.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = card.topic,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Card", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = card.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider(color = ext.cardRuledLine)

            Text(
                text = card.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = card.difficulty == CardDifficulty.EASY,
                    onClick = onMarkEasy,
                    label = { Text("Easy") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (card.difficulty == CardDifficulty.EASY) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                FilterChip(
                    selected = card.difficulty == CardDifficulty.DIFFICULT,
                    onClick = onMarkDifficult,
                    label = { Text("Difficult") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (card.difficulty == CardDifficulty.DIFFICULT) {
                                Icons.Default.Warning
                            } else {
                                Icons.Outlined.Warning
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Spacer(Modifier.weight(1f))
                if (card.reviewCount > 0) {
                    Text(
                        text = "${card.reviewCount}x studied",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
