package corsica.erwinn.tracce.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import corsica.erwinn.tracce.TracceContainer
import corsica.erwinn.tracce.data.firebase.FirebaseSyncManager
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.data.repository.TracceRepository
import corsica.erwinn.tracce.service.ProximityRadarService
import corsica.erwinn.tracce.utils.LocationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuestsViewModel(
    private val repository: TracceRepository
) : ViewModel() {

    // Profil & Préférences (DataStore - Slide 14 & 26)
    val username: StateFlow<String> = repository.userPreferences.username.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "Explorateur"
    )

    val isAdmin: StateFlow<Boolean> = repository.userPreferences.isAdmin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val isRadarActive: StateFlow<Boolean> = repository.userPreferences.isRadarActive.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val isDemoMode: StateFlow<Boolean> = repository.userPreferences.isDemoMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val hasSeenOnboarding: StateFlow<Boolean> = repository.userPreferences.hasSeenOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    // Filtres sélectionnés par l'utilisateur
    val selectedCity = MutableStateFlow<String?>("all") // "all", "ajaccio", "bastia", "corte"
    val selectedType = MutableStateFlow<String?>("all") // "all", "ville", "sommet", etc.

    // Quêtes brutes depuis Room
    val allQuests: StateFlow<List<Quest>> = repository.allQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Complétions réactives partitionnées par utilisateur courant (Room + Firestore)
    @OptIn(ExperimentalCoroutinesApi::class)
    val completions: StateFlow<List<Completion>> = username.flatMapLatest { user ->
        repository.getCompletionsForUser(user)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Quêtes filtrées combinant ville et type
    val filteredQuests: StateFlow<List<Quest>> = combine(
        allQuests,
        selectedCity,
        selectedType
    ) { quests, city, type ->
        quests.filter { q ->
            val matchCity = city == null || city == "all" || q.cityId.equals(city, ignoreCase = true)
            val matchType = type == null || type == "all" || q.type.equals(type, ignoreCase = true)
            matchCity && matchType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Score total dynamique calculé uniquement sur les complétions de l'utilisateur actif
    val totalScore: StateFlow<Int> = combine(allQuests, completions) { quests, comps ->
        val completedIds = comps.map { it.questId }.toSet()
        quests.filter { it.id in completedIds }.sumOf { it.points }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    init {
        // Synchronisation des complétions distantes dès qu'un utilisateur identifié est actif
        viewModelScope.launch {
            username.collect { user ->
                if (user.isNotBlank() && !user.equals("explorateur", ignoreCase = true) && !user.equals("explorateur anonyme", ignoreCase = true)) {
                    repository.syncRemoteCompletionsForUser(user)
                }
            }
        }
    }

    fun setCity(cityId: String?) {
        selectedCity.value = cityId
    }

    fun setType(typeId: String?) {
        selectedType.value = typeId
    }

    fun validateQuest(
        quest: Quest,
        lat: Double,
        lng: Double,
        photoPath: String?,
        isDemo: Boolean = isDemoMode.value,
        onResult: (Result<Completion>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.validateQuest(
                quest = quest,
                userLat = lat,
                userLng = lng,
                photoPath = photoPath,
                isDemoMode = isDemo,
                username = username.value,
                isAdmin = isAdmin.value
            )
            onResult(res)
        }
    }

    fun toggleDemoMode() {
        viewModelScope.launch {
            val current = isDemoMode.value
            repository.userPreferences.setDemoMode(!current)
        }
    }

    fun toggleRadar(context: Context): Boolean {
        val current = isRadarActive.value
        val nextState = !current
        if (nextState) {
            if (!LocationHelper.hasLocationPermission(context)) {
                return false
            }
            val started = ProximityRadarService.startService(context)
            if (started) {
                viewModelScope.launch {
                    repository.userPreferences.setRadarActive(true)
                }
                return true
            }
            return false
        } else {
            ProximityRadarService.stopService(context)
            viewModelScope.launch {
                repository.userPreferences.setRadarActive(false)
            }
            return true
        }
    }

    fun login(user: String, pass: String): Boolean {
        val u = user.trim()
        val p = pass.trim()
        if (u.isBlank()) return false

        val isAdmin = (u.equals("erwinn", ignoreCase = true) && p == "pwaErwinn")
        viewModelScope.launch {
            repository.userPreferences.saveUser(u, isAdmin = isAdmin)
            // 1. Récupère d'abord les complétions sauvegardées de cet utilisateur dans Firestore
            repository.syncRemoteCompletionsForUser(u)
            // 2. Synchronise le profil dans users/{uid} sur Firestore avec les vraies complétions de cet utilisateur
            val completedIds = repository.getCompletedQuestIdsForUser(u)
            FirebaseSyncManager.syncUserProfile(u, isAdmin, completedIds)
        }
        return true
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.userPreferences.setOnboardingCompleted(true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.userPreferences.saveUser("Explorateur", isAdmin = false)
            FirebaseSyncManager.syncUserProfile("Explorateur anonyme", isAdmin = false, emptyList())
        }
    }

    val customQuests: StateFlow<List<Quest>> = repository.customQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun triggerSync() {
        viewModelScope.launch {
            repository.triggerSync()
        }
    }

    fun addCustomQuest(quest: Quest, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addCustomQuest(quest)
            } finally {
                onComplete()
            }
        }
    }

    fun deleteCustomQuest(id: String) {
        viewModelScope.launch {
            repository.deleteCustomQuest(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                QuestsViewModel(TracceContainer.repository)
            }
        }
    }
}
