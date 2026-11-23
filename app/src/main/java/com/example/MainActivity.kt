package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.CardDifficulty
import com.example.data.ExportFormat
import com.example.data.QuizMode
import com.example.data.SmartCardsDatabase
import com.example.data.SmartCardsRepository
import com.example.data.ThemePreferences
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DifficultReviewScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuizHubScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.SmartCardsTheme
import com.example.viewmodel.DeckDetailViewModel
import com.example.viewmodel.LibraryViewModel
import com.example.viewmodel.ProgressViewModel
import com.example.viewmodel.QuizViewModel
import com.example.viewmodel.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object Routes {
    const val LIBRARY = "library"
    const val REVIEW = "review"
    const val QUIZ_HUB = "quiz_hub"
    const val PROGRESS = "progress"
    const val DECK_DETAIL = "deck_detail/{deckId}"
    const val STUDY = "study?deckId={deckId}&difficultOnly={difficultOnly}&shuffled={shuffled}"
    const val QUIZ = "quiz?deckId={deckId}&mode={mode}&difficultOnly={difficultOnly}&limit={limit}"

    fun deckDetail(deckId: Long) = "deck_detail/$deckId"

    fun study(deckId: Long?, difficultOnly: Boolean, shuffled: Boolean): String =
        "study?deckId=${deckId ?: -1L}&difficultOnly=$difficultOnly&shuffled=$shuffled"

    fun quiz(deckId: Long?, mode: QuizMode, difficultOnly: Boolean, limit: Int): String =
        "quiz?deckId=${deckId ?: -1L}&mode=${mode.name}&difficultOnly=$difficultOnly&limit=$limit"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = SmartCardsDatabase.getInstance(applicationContext)
        val repository = SmartCardsRepository(database)
        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            SmartCardsApp(
                repository = repository,
                themePreferences = themePreferences
            )
        }
    }
}

@Composable
fun SmartCardsApp(
    repository: SmartCardsRepository,
    themePreferences: ThemePreferences
) {
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(repository, themePreferences)
    )
    val progressViewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.Factory(repository)
    )

    val themeMode by libraryViewModel.themeMode.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val difficultCards by libraryViewModel.difficultCards.collectAsStateWithLifecycle()
    val progressState by progressViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingExportFormat by remember { mutableStateOf(ExportFormat.JSON) }
    var pendingExportDeckId by remember { mutableStateOf<Long?>(null) }
    var pendingImportDeckId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        libraryViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }
            if (content.isNullOrBlank()) {
                libraryViewModel.emitMessage("Selected file was empty or unreadable")
                return@launch
            }
            val targetDeckId = pendingImportDeckId
            pendingImportDeckId = null
            if (targetDeckId != null) {
                val payloads = com.example.data.ImportExportManager.parseImportContent(content, "Imported Cards")
                if (payloads.isEmpty()) {
                    libraryViewModel.emitMessage("No valid flashcards found in file")
                } else {
                    val summary = repository.importPayloads(payloads, targetDeckId = targetDeckId)
                    libraryViewModel.emitMessage("Imported ${summary.cardsImported} card(s) into deck")
                }
            } else {
                libraryViewModel.importFromText(content)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val deckId = pendingExportDeckId
            val format = pendingExportFormat
            pendingExportDeckId = null
            val payload = if (deckId != null) {
                repository.exportSingleDeck(deckId, format)
            } else {
                libraryViewModel.generateExportText(format)
            }
            if (payload == null) {
                libraryViewModel.emitMessage("Could not find deck to export")
                return@launch
            }
            val wrote = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    }
                }.isSuccess
            }
            libraryViewModel.emitMessage(
                if (wrote) "Flashcards exported as ${format.label}" else "Export failed"
            )
        }
    }

    val launchImport: (Long?) -> Unit = { deckId ->
        pendingImportDeckId = deckId
        importLauncher.launch(
            arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*")
        )
    }

    val launchExport: (Long?, ExportFormat) -> Unit = { deckId, format ->
        pendingExportDeckId = deckId
        pendingExportFormat = format
        val prefix = if (deckId != null) "smartcards_deck" else "smartcards_library"
        exportLauncher.launch("$prefix.${format.extension}")
    }

    val topTabs = remember {
        listOf(
            TopLevelTab(
                route = Routes.LIBRARY,
                label = "Library",
                selectedIcon = Icons.Filled.Style,
                unselectedIcon = Icons.Outlined.Style,
                testTag = "tab_library"
            ),
            TopLevelTab(
                route = Routes.REVIEW,
                label = "Review",
                selectedIcon = Icons.Filled.Warning,
                unselectedIcon = Icons.Outlined.WarningAmber,
                testTag = "tab_review"
            ),
            TopLevelTab(
                route = Routes.QUIZ_HUB,
                label = "Quiz",
                selectedIcon = Icons.Filled.Quiz,
                unselectedIcon = Icons.Outlined.Quiz,
                testTag = "tab_quiz"
            ),
            TopLevelTab(
                route = Routes.PROGRESS,
                label = "Progress",
                selectedIcon = Icons.Filled.BarChart,
                unselectedIcon = Icons.Outlined.BarChart,
                testTag = "tab_progress"
            )
        )
    }

    val showTopLevelNav = currentRoute in setOf(
        Routes.LIBRARY,
        Routes.REVIEW,
        Routes.QUIZ_HUB,
        Routes.PROGRESS
    )

    SmartCardsTheme(themeMode = themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showTopLevelNav && !useNavigationRail) {
                        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                            topTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                        if (tab.route == Routes.REVIEW && difficultCards.isNotEmpty()) {
                                            BadgedBox(
                                                badge = { Badge { Text("${difficultCards.size}") } }
                                            ) {
                                                Icon(iconVector, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail {
                            topTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                        if (tab.route == Routes.REVIEW && difficultCards.isNotEmpty()) {
                                            BadgedBox(
                                                badge = { Badge { Text("${difficultCards.size}") } }
                                            ) {
                                                Icon(iconVector, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.LIBRARY
                        ) {
                            composable(Routes.LIBRARY) {
                                LibraryScreen(
                                    uiState = libraryState,
                                    themeMode = themeMode,
                                    onCycleTheme = libraryViewModel::cycleThemeMode,
                                    onSearchChange = libraryViewModel::updateSearchQuery,
                                    onSelectSubject = libraryViewModel::selectSubject,
                                    onCreateDeck = libraryViewModel::createDeck,
                                    onUpdateDeck = libraryViewModel::updateDeck,
                                    onDeleteDeck = libraryViewModel::deleteDeck,
                                    onOpenDeck = { deckId ->
                                        navController.navigate(Routes.deckDetail(deckId))
                                    },
                                    onStartStudy = { deckId, diffOnly, shuffled ->
                                        navController.navigate(Routes.study(deckId, diffOnly, shuffled))
                                    },
                                    onStartQuiz = { deckId, mode, diffOnly, limit ->
                                        navController.navigate(Routes.quiz(deckId, mode, diffOnly, limit))
                                    },
                                    onLoadStarterDecks = libraryViewModel::loadStarterDecks,
                                    onImportClick = { launchImport(null) },
                                    onExportClick = { format -> launchExport(null, format) }
                                )
                            }

                            composable(Routes.REVIEW) {
                                DifficultReviewScreen(
                                    difficultCards = difficultCards,
                                    onStartDifficultStudy = {
                                        navController.navigate(Routes.study(null, difficultOnly = true, shuffled = true))
                                    },
                                    onStartDifficultQuiz = { mode, limit ->
                                        navController.navigate(Routes.quiz(null, mode, difficultOnly = true, limit = limit))
                                    },
                                    onMarkEasy = { cardId ->
                                        libraryViewModel.setCardDifficulty(cardId, CardDifficulty.EASY)
                                    },
                                    onOpenDeck = { deckId ->
                                        navController.navigate(Routes.deckDetail(deckId))
                                    }
                                )
                            }

                            composable(Routes.QUIZ_HUB) {
                                QuizHubScreen(
                                    libraryState = libraryState,
                                    recentSessions = progressState.recentSessions,
                                    onStartQuiz = { deckId, mode, diffOnly, limit ->
                                        navController.navigate(Routes.quiz(deckId, mode, diffOnly, limit))
                                    }
                                )
                            }

                            composable(Routes.PROGRESS) {
                                ProgressScreen(
                                    uiState = progressState,
                                    themeMode = themeMode,
                                    onSetThemeMode = libraryViewModel::setThemeMode,
                                    onReviewDifficult = {
                                        navController.navigate(Routes.study(null, difficultOnly = true, shuffled = true))
                                    },
                                    onImportClick = { launchImport(null) },
                                    onExportClick = { format -> launchExport(null, format) }
                                )
                            }

                            composable(
                                route = Routes.DECK_DETAIL,
                                arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                                val deckViewModel: DeckDetailViewModel = viewModel(
                                    key = "deck_$deckId",
                                    factory = DeckDetailViewModel.Factory(deckId, repository)
                                )
                                val detailState by deckViewModel.uiState.collectAsStateWithLifecycle()

                                LaunchedEffect(deckViewModel) {
                                    deckViewModel.snackbarEvents.collect { msg ->
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }

                                DeckDetailScreen(
                                    uiState = detailState,
                                    onBack = { navController.popBackStack() },
                                    onUpdateDeck = deckViewModel::updateDeck,
                                    onAddCard = deckViewModel::addCard,
                                    onUpdateCard = deckViewModel::updateCard,
                                    onDeleteCard = deckViewModel::deleteCard,
                                    onToggleDifficulty = deckViewModel::toggleDifficulty,
                                    onSetFilter = deckViewModel::setFilter,
                                    onSearchChange = deckViewModel::updateSearchQuery,
                                    onStartStudy = { diffOnly, shuffled ->
                                        navController.navigate(Routes.study(deckId, diffOnly, shuffled))
                                    },
                                    onStartQuiz = { mode, diffOnly, limit ->
                                        navController.navigate(Routes.quiz(deckId, mode, diffOnly, limit))
                                    },
                                    onImportIntoDeck = { launchImport(deckId) },
                                    onExportDeck = { format -> launchExport(deckId, format) }
                                )
                            }

                            composable(
                                route = Routes.STUDY,
                                arguments = listOf(
                                    navArgument("deckId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("difficultOnly") { type = NavType.BoolType; defaultValue = false },
                                    navArgument("shuffled") { type = NavType.BoolType; defaultValue = false }
                                )
                            ) { backStackEntry ->
                                val rawDeckId = backStackEntry.arguments?.getLong("deckId") ?: -1L
                                val deckId = if (rawDeckId > 0L) rawDeckId else null
                                val difficultOnly = backStackEntry.arguments?.getBoolean("difficultOnly") ?: false
                                val shuffled = backStackEntry.arguments?.getBoolean("shuffled") ?: false

                                val studyViewModel: StudyViewModel = viewModel(
                                    factory = StudyViewModel.Factory(deckId, difficultOnly, shuffled, repository)
                                )
                                val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

                                StudyScreen(
                                    uiState = studyState,
                                    onBack = { navController.popBackStack() },
                                    onFlip = studyViewModel::flipCard,
                                    onMarkDifficulty = studyViewModel::markAndAdvance,
                                    onNext = studyViewModel::nextCard,
                                    onPrevious = studyViewModel::previousCard,
                                    onToggleShuffle = studyViewModel::toggleShuffle,
                                    onRestart = { restartDiff, restartShuf ->
                                        studyViewModel.loadSession(restartDiff, restartShuf)
                                    }
                                )
                            }

                            composable(
                                route = Routes.QUIZ,
                                arguments = listOf(
                                    navArgument("deckId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("mode") { type = NavType.StringType; defaultValue = QuizMode.MULTIPLE_CHOICE.name },
                                    navArgument("difficultOnly") { type = NavType.BoolType; defaultValue = false },
                                    navArgument("limit") { type = NavType.IntType; defaultValue = 10 }
                                )
                            ) { backStackEntry ->
                                val rawDeckId = backStackEntry.arguments?.getLong("deckId") ?: -1L
                                val deckId = if (rawDeckId > 0L) rawDeckId else null
                                val modeRaw = backStackEntry.arguments?.getString("mode") ?: QuizMode.MULTIPLE_CHOICE.name
                                val mode = enumValues<QuizMode>().firstOrNull { it.name == modeRaw } ?: QuizMode.MULTIPLE_CHOICE
                                val difficultOnly = backStackEntry.arguments?.getBoolean("difficultOnly") ?: false
                                val limit = backStackEntry.arguments?.getInt("limit") ?: 10

                                val quizViewModel: QuizViewModel = viewModel(
                                    factory = QuizViewModel.Factory(deckId, mode, difficultOnly, limit, repository)
                                )
                                val quizState by quizViewModel.uiState.collectAsStateWithLifecycle()

                                QuizScreen(
                                    uiState = quizState,
                                    onBack = { navController.popBackStack() },
                                    onSelectChoice = quizViewModel::selectMultipleChoice,
                                    onSelectTrueFalse = quizViewModel::selectTrueFalse,
                                    onNext = quizViewModel::nextQuestion,
                                    onRetake = quizViewModel::startQuiz,
                                    onMarkDifficult = quizViewModel::markMissedCardDifficult
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
