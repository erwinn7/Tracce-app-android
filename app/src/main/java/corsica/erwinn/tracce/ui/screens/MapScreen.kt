package corsica.erwinn.tracce.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.ui.components.LeafletMapView
import corsica.erwinn.tracce.ui.theme.MaquisGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    quests: List<Quest>,
    onQuestSelect: (Quest) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carte de Corse — Tracce", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaquisGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LeafletMapView(
            quests = quests,
            completedIds = emptySet(),
            selectedQuestId = null,
            onSelectQuest = onQuestSelect,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
