package corsica.erwinn.tracce.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import corsica.erwinn.tracce.ui.components.AppBottomBar
import corsica.erwinn.tracce.ui.components.AuthDialog
import corsica.erwinn.tracce.ui.components.ModernAppTopBar
import corsica.erwinn.tracce.ui.screens.*
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import corsica.erwinn.tracce.ui.viewmodel.QuestsViewModel

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Profile : Screen("profile")     // Exploration & Grande Carte principale
    data object Quests : Screen("quests")       // Catalogue
    data object Settings : Screen("settings")   // Réglages & Capteurs
    data object Admin : Screen("admin")         // Espace Administrateur
    data object QuestDetail : Screen("quest_detail/{questId}") {
        fun createRoute(questId: String) = "quest_detail/$questId"
    }
}

@Composable
fun TracceNavGraph(
    initialQuestId: String? = null,
    navController: NavHostController = rememberNavController(),
    viewModel: QuestsViewModel = viewModel(factory = QuestsViewModel.Factory)
) {
    val context = LocalContext.current

    // Navigation automatique vers un lieu ciblé (Push FCM ou App Link - Slide 9 & 13)
    LaunchedEffect(initialQuestId) {
        if (!initialQuestId.isNullOrBlank()) {
            navController.navigate(Screen.QuestDetail.createRoute(initialQuestId)) {
                launchSingleTop = true
            }
        }
    }

    val quests by viewModel.filteredQuests.collectAsStateWithLifecycle()
    val allQuests by viewModel.allQuests.collectAsStateWithLifecycle()
    val customQuests by viewModel.customQuests.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val totalScore by viewModel.totalScore.collectAsStateWithLifecycle()

    val username by viewModel.username.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isRadarActive by viewModel.isRadarActive.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Profile.route

    var showAuthDialog by remember { mutableStateOf(false) }

    // Si première installation / première ouverture, affiche l'onboarding animé avec splash et les 3 grandes tuiles
    if (!hasSeenOnboarding) {
        OnboardingScreen(
            onComplete = {
                viewModel.completeOnboarding()
            }
        )
    } else {
        // Titre sobre de la barre supérieure selon la section
        val topBarTitle = when {
            currentRoute == Screen.Profile.route -> "Exploration"
            currentRoute == Screen.Quests.route -> "Catalogue des Quêtes"
            currentRoute == Screen.Settings.route -> "Réglages & Capteurs"
            currentRoute == Screen.Admin.route -> "Espace Administrateur"
            currentRoute.startsWith("quest_detail") -> "Détail de la Quête"
            else -> "Tracce"
        }

        val showBottomBar = currentRoute in listOf(
            Screen.Profile.route,
            Screen.Quests.route,
            Screen.Settings.route,
            Screen.Admin.route
        )

        Scaffold(
            containerColor = SandLight,
            topBar = {
                if (currentRoute != Screen.Onboarding.route) {
                    ModernAppTopBar(
                        title = topBarTitle,
                        username = username,
                        isAdmin = isAdmin,
                        score = totalScore,
                        onProfileClick = {
                            if (username == "Explorateur") {
                                showAuthDialog = true
                            } else {
                                navController.navigate(Screen.Settings.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        isAdmin = isAdmin,
                        onTabSelected = { targetRoute ->
                            if (targetRoute != currentRoute) {
                                navController.navigate(targetRoute) {
                                    popUpTo(Screen.Profile.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Profile.route, // La page principale est directement l'exploration avec la grande carte !
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { 600 }, animationSpec = tween(260)) +
                            fadeIn(animationSpec = tween(260))
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -600 }, animationSpec = tween(260)) +
                            fadeOut(animationSpec = tween(260))
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -600 }, animationSpec = tween(260)) +
                            fadeIn(animationSpec = tween(260))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { 600 }, animationSpec = tween(260)) +
                            fadeOut(animationSpec = tween(260))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. PAGE PRINCIPALE : EXPLORATION AVEC LA GRANDE CARTE DE CORSE & STATS
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        username = username,
                        isAdmin = isAdmin,
                        score = totalScore,
                        quests = allQuests,
                        completions = completions,
                        onQuestClick = { quest ->
                            navController.navigate(Screen.QuestDetail.createRoute(quest.id))
                        },
                        onOpenAuth = { showAuthDialog = true },
                        onLogout = { viewModel.logout() }
                    )
                }

                // 2. CATALOGUE DES QUÊTES
                composable(Screen.Quests.route) {
                    QuestsScreen(
                        quests = quests,
                        completions = completions,
                        selectedCity = selectedCity,
                        selectedType = selectedType,
                        onCitySelect = { viewModel.setCity(it) },
                        onTypeSelect = { viewModel.setType(it) },
                        onQuestClick = { quest ->
                            navController.navigate(Screen.QuestDetail.createRoute(quest.id))
                        }
                    )
                }

                // 3. RÉGLAGES & CAPTEURS (Radar GPS, Mode Démo, WorkManager, Replay Onboarding)
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        username = username,
                        isAdmin = isAdmin,
                        isRadarActive = isRadarActive,
                        isDemoMode = isDemoMode,
                        onToggleRadar = { viewModel.toggleRadar(context) },
                        onToggleDemoMode = { viewModel.toggleDemoMode() },
                        onSyncClick = { viewModel.triggerSync() },
                        onOpenAuth = { showAuthDialog = true },
                        onLogout = { viewModel.logout() },
                        onReplayTutorial = {
                            navController.navigate(Screen.Onboarding.route)
                        }
                    )
                }

                // Replay tutoriel
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onComplete = {
                            navController.popBackStack()
                        }
                    )
                }

                // 4. ESPACE ADMINISTRATEUR (Ajout de lieux avec géolocalisation directe)
                composable(Screen.Admin.route) {
                    AdminScreen(
                        onAddQuest = { quest, onComplete ->
                            viewModel.addCustomQuest(quest, onComplete)
                        },
                        onSyncClick = { viewModel.triggerSync() },
                        onBackClick = {
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        },
                        customQuests = customQuests,
                        onDeleteQuest = { id -> viewModel.deleteCustomQuest(id) }
                    )
                }

                // 5. FICHE DÉTAIL D'UNE QUÊTE (Photo + validation GPS stricte)
                composable(
                    route = Screen.QuestDetail.route,
                    arguments = listOf(navArgument("questId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val questId = backStackEntry.arguments?.getString("questId") ?: ""
                    val quest = allQuests.find { it.id == questId } ?: quests.find { it.id == questId }
                    val completion = completions.find { it.questId == questId }

                    if (quest != null) {
                        QuestDetailScreen(
                            quest = quest,
                            completion = completion,
                            isDemoMode = isDemoMode,
                            onToggleDemoMode = { viewModel.toggleDemoMode() },
                            onBackClick = { navController.popBackStack() },
                            onValidateClick = { userLat, userLng, photoPath, onResult ->
                                viewModel.validateQuest(
                                    quest = quest,
                                    lat = userLat,
                                    lng = userLng,
                                    photoPath = photoPath,
                                    isDemo = isDemoMode,
                                    onResult = onResult
                                )
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SandLight),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Terracotta)
                        }
                    }
                }
            }
        }

        // Modal d'authentification
        if (showAuthDialog) {
            AuthDialog(
                onDismiss = { showAuthDialog = false },
                onLogin = { u, p -> viewModel.login(u, p) }
            )
        }
    }
}
