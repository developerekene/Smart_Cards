package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CardWithDeck
import com.example.data.DeckEntity
import com.example.data.DeckWithStats
import com.example.data.ExportFormat
import com.example.data.QuizMode
import com.example.data.ThemeMode
import com.example.ui.components.DeckEditorDialog
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.QuizSetupDialog
import com.example.ui.components.SubjectPill
import com.example.ui.theme.DeckAccentPalette
import com.example.ui.theme.LocalSmartCardsColors
import com.example.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectSubject: (String?) -> Unit,
    onCreateDeck: (String, String, String, Int) -> Unit,
    onUpdateDeck: (Long, String, String, String, Int) -> Unit,
    onDeleteDeck: (Long, String) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onStartStudy: (deckId: Long?, difficultOnly: Boolean, shuffled: Boolean) -> Unit,
    onStartQuiz: (deckId: Long?, mode: QuizMode, difficultOnly: Boolean, limit: Int) -> Unit,
    onLoadStarterDecks: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingDeck by remember { mutableStateOf<DeckWithStats?>(null) }
    var deletingDeck by remember { mutableStateOf<DeckWithStats?>(null) }
    var quizTargetDeck by remember { mutableStateOf<DeckWithStats?>(null) }
    var showImportExportDialog by remember { mutableStateOf(false) }

    val ext = LocalSmartCardsColors.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "SmartCards",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showImportExportDialog = true },
                        modifier = Modifier.testTag("library_import_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImportExport,
                            contentDescription = "Import and Export Flashcards"
                        )
                    }
                    IconButton(
                        onClick = onCycleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            },
                            contentDescription = "Switch Theme (${themeMode.name})"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_deck_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Deck")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner
                item(key = "hero_banner") {
                    StudyHeroCard(
                        totalDecks = uiState.allDecks.size,
                        totalCards = uiState.totalCards,
                        easyCards = uiState.easyCards,
                        difficultCards = uiState.difficultCards,
                        onRandomStudy = { onStartStudy(null, false, true) },
                        onReviewDifficult = { onStartStudy(null, true, true) }
                    )
                }

                // Search Bar
                if (uiState.allDecks.isNotEmpty()) {
                    item(key = "search_bar") {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search decks, subjects, questions & answers…") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input")
                        )
                    }
                }

                // Subject Filter Chips
                if (uiState.subjects.isNotEmpty()) {
                    item(key = "subject_filters") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedSubject == null,
                                    onClick = { onSelectSubject(null) },
                                    label = { Text("All (${uiState.allDecks.size})") },
                                    modifier = Modifier.testTag("subject_chip_all")
                                )
                            }
                            items(uiState.subjects) { subject ->
                                FilterChip(
                                    selected = uiState.selectedSubject.equals(subject, ignoreCase = true),
                                    onClick = { onSelectSubject(subject) },
                                    label = { Text(subject) }
                                )
                            }
                        }
                    }
                }

                // Empty State when no decks exist
                if (uiState.allDecks.isEmpty()) {
                    item(key = "empty_library") {
                        EmptyLibraryCard(
                            onCreateDeck = { showCreateDialog = true },
                            onImport = onImportClick,
                            onLoadStarter = onLoadStarterDecks
                        )
                    }
                } else {
                    // Section header for Decks
                    item(key = "decks_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    "Matching Decks (${uiState.filteredDecks.size})"
                                } else {
                                    "Your Decks (${uiState.filteredDecks.size})"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            TextButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Deck")
                            }
                        }
                    }

                    if (uiState.filteredDecks.isEmpty()) {
                        item(key = "no_matching_decks") {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No decks match your search or subject filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.filteredDecks, key = { "deck_${it.id}" }) { deck ->
                            DeckLibraryCard(
                                deck = deck,
                                onOpen = { onOpenDeck(deck.id) },
                                onStudy = { onStartStudy(deck.id, false, false) },
                                onReviewDifficult = { onStartStudy(deck.id, true, true) },
                                onQuiz = { quizTargetDeck = deck },
                                onEdit = { editingDeck = deck },
                                onDelete = { deletingDeck = deck }
                            )
                        }
                    }

                    // Matching Cards section when searching
                    if (uiState.searchQuery.isNotBlank() && uiState.matchingCards.isNotEmpty()) {
                        item(key = "matching_cards_header") {
                            Text(
                                text = "Matching Cards (${uiState.matchingCards.size})",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.matchingCards.take(25), key = { "card_${it.id}" }) { card ->
                            SearchCardResultItem(
                                card = card,
                                onClick = { onOpenDeck(card.deckId) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        DeckEditorDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, subject, description, accent ->
                onCreateDeck(title, subject, description, accent)
                showCreateDialog = false
            }
        )
    }

    editingDeck?.let { deck ->
        DeckEditorDialog(
            initialDeck = DeckEntity(
                id = deck.id,
                title = deck.title,
                subject = deck.subject,
                description = deck.description,
                accentIndex = deck.accentIndex
            ),
            onDismiss = { editingDeck = null },
            onSave = { title, subject, description, accent ->
                onUpdateDeck(deck.id, title, subject, description, accent)
                editingDeck = null
            }
        )
    }

    deletingDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { deletingDeck = null },
            title = { Text("Delete Deck?") },
            text = { Text("Delete \"${deck.title}\" and all ${deck.totalCards} card(s) inside it? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDeck(deck.id, deck.title)
                        deletingDeck = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDeck = null }) { Text("Cancel") }
            }
        )
    }

    quizTargetDeck?.let { deck ->
        QuizSetupDialog(
            deckTitle = deck.title,
            totalCards = deck.totalCards,
            difficultCards = deck.difficultCards,
            onDismiss = { quizTargetDeck = null },
            onStartQuiz = { mode, diffOnly, limit ->
                quizTargetDeck = null
                onStartQuiz(deck.id, mode, diffOnly, limit)
            }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            hasCardsToExport = uiState.totalCards > 0,
            onDismiss = { showImportExportDialog = false },
            onImportClick = onImportClick,
            onExportClick = onExportClick
        )
    }
}

@Composable
private fun StudyHeroCard(
    totalDecks: Int,
    totalCards: Int,
    easyCards: Int,
    difficultCards: Int,
    onRandomStudy: () -> Unit,
    onReviewDifficult: () -> Unit
) {
    val ext = LocalSmartCardsColors.current
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_study_hero),
                contentDescription = "Study flashcard library illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ext.heroScrimStart, ext.heroScrimEnd)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$totalDecks Decks · $totalCards Cards",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (totalCards > 0) {
                        val pct = ((easyCards * 100f) / totalCards).toInt()
                        Text(
                            text = "$pct% Mastered",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }

                Text(
                    text = if (totalCards == 0) {
                        "Build Your Study Library"
                    } else {
                        "Ready to sharpen your recall?"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Text(
                    text = if (totalCards == 0) {
                        "Create custom flashcard decks, flip to test recall, and track mastery over time."
                    } else {
                        "Jump into a shuffled mix across all decks or focus on cards marked difficult."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )

                if (totalCards > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRandomStudy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF1E1B18)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_random_study_button")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Random Mix", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (difficultCards > 0) {
                            Button(
                                onClick = onReviewDifficult,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_review_difficult_button")
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Difficult ($difficultCards)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    onCreateDeck: () -> Unit,
    onImport: () -> Unit,
    onLoadStarter: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_library),
                contentDescription = "Empty flashcard deck stack",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Text(
                text = "No Flashcard Decks Yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Create your first custom deck, import an existing JSON/CSV set, or load starter decks to explore study & quiz modes right away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateDeck,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_create_deck_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Deck")
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_import_button")
                ) {
                    Text("Import File")
                }
            }

            TextButton(
                onClick = onLoadStarter,
                modifier = Modifier.testTag("load_starter_decks_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load 3 Starter Decks")
            }
        }
    }
}

@Composable
private fun DeckLibraryCard(
    deck: DeckWithStats,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    onReviewDifficult: () -> Unit,
    onQuiz: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = DeckAccentPalette[deck.accentIndex.coerceIn(0, DeckAccentPalette.lastIndex)]
    val ext = LocalSmartCardsColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("deck_card_${deck.id}")
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Spine Accent
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(172.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubjectPill(subject = deck.subject, accentColor = accent)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${deck.totalCards} cards",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Deck Options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Deck") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Deck") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deck.description.isNotBlank()) {
                        Text(
                            text = deck.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Mastery Bar
                if (deck.totalCards > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { deck.masteryFraction },
                            color = ext.easy,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${deck.easyCards} Easy · ${deck.unmarkedCards} New",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (deck.difficultCards > 0) {
                                Text(
                                    text = "${deck.difficultCards} Difficult",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.difficult
                                )
                            }
                        }
                    }
                }

                // Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStudy,
                        enabled = deck.totalCards > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Study", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onQuiz,
                        enabled = deck.totalCards > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quiz", style = MaterialTheme.typography.labelMedium)
                    }

                    if (deck.difficultCards > 0) {
                        OutlinedButton(
                            onClick = onReviewDifficult,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.difficult)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Review Difficult", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCardResultItem(
    card: CardWithDeck,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${card.deckTitle} · ${card.deckSubject}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                DifficultyBadge(difficulty = card.difficulty)
            }
            Text(
                text = card.question,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "A: ${card.answer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
