package corsica.erwinn.tracce

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import corsica.erwinn.tracce.ui.navigation.TracceNavGraph
import corsica.erwinn.tracce.ui.theme.TracceTheme

class MainActivity : ComponentActivity() {

    private var targetQuestId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetQuestId = extractQuestId(intent)

        setContent {
            TracceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TracceNavGraph(initialQuestId = targetQuestId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractQuestId(intent)?.let { qId ->
            targetQuestId = qId
        }
    }

    private fun extractQuestId(intent: Intent?): String? {
        if (intent == null) return null
        val directExtra = intent.getStringExtra("questId")
        if (!directExtra.isNullOrBlank()) return directExtra

        val data = intent.data
        if (data != null) {
            val queryQuest = data.getQueryParameter("quest") ?: data.getQueryParameter("id")
            if (!queryQuest.isNullOrBlank()) return queryQuest

            val pathSegments = data.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == "quest") {
                return pathSegments[1]
            }
        }
        return null
    }
}