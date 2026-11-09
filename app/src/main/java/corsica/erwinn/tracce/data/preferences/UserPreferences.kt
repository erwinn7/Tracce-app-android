package corsica.erwinn.tracce.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tracce_user_prefs")

class UserPreferences(private val context: Context) {

    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_IS_ADMIN = booleanPreferencesKey("is_admin")
    private val KEY_RADAR_ACTIVE = booleanPreferencesKey("radar_active")
    private val KEY_DEMO_MODE = booleanPreferencesKey("demo_mode")
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: "Explorateur"
    }

    val isAdmin: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_ADMIN] ?: false
    }

    val isRadarActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RADAR_ACTIVE] ?: false
    }

    val isDemoMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEMO_MODE] ?: false
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun saveUser(username: String, isAdmin: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
            prefs[KEY_IS_ADMIN] = isAdmin
        }
    }

    suspend fun setRadarActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RADAR_ACTIVE] = active
        }
    }

    suspend fun setDemoMode(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEMO_MODE] = active
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
