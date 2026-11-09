package corsica.erwinn.tracce.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.ui.theme.*

@Composable
fun QuestsScreen(
    quests: List<Quest>,
    completions: List<Completion>,
    selectedCity: String?,
    selectedType: String?,
    onCitySelect: (String?) -> Unit,
    onTypeSelect: (String?) -> Unit,
    onQuestClick: (Quest) -> Unit
) {
    val completedMap = completions.associateBy { it.questId }
    val cities = listOf(
        "all" to "Toute la Corse",
        "ajaccio" to "Ajaccio",
        "bastia" to "Bastia",
        "corte" to "Corte"
    )

    val types = listOf(
        "all" to "Tous types",
        "ville" to "Ville",
        "colline" to "Colline",
        "site-naturel" to "Site Naturel",
        "sommet" to "Sommet"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SandLight)
    ) {
        // 1. SÉLECTEUR DE TERRITOIRE (GRILLE / LIGNE ÉLÉGANTE COMME LA PWA)
        Column(modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)) {
            Text(
                text = "Choisissez un territoire",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaquisGreen
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cities) { (cityId, label) ->
                    val isSelected = (selectedCity == cityId) || (selectedCity == null && cityId == "all")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaquisGreen else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaquisGreen else BorderColor
                        ),
                        modifier = Modifier
                            .clickable { onCitySelect(if (cityId == "all") null else cityId) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. FILTRES PAR TYPE AVEC COULEURS CHARTE PWA
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(types) { (typeId, label) ->
                val isSelected = (selectedType == typeId) || (selectedType == null && typeId == "all")
                val chipColor = when (typeId) {
                    "ville" -> TypeVille
                    "colline" -> TypeColline
                    "site-naturel" -> TypeNaturel
                    "sommet" -> TypeSommet
                    else -> MaquisGreen
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelect(if (typeId == "all") null else typeId) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 3. EN-TÊTE DE LA LISTE : COMPTEUR & STATUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${quests.size} quêtes au catalogue",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaquisGreen
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaquisGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${completions.size} validée${if (completions.size > 1) "s" else ""}",
                    color = MaquisGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 4. LISTE DES QUÊTES
        if (quests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aucune quête ne correspond à vos filtres.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaquisGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        onCitySelect(null)
                        onTypeSelect(null)
                    }) {
                        Text("Réinitialiser les filtres", color = Terracotta, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quests, key = { it.id }) { quest ->
                    val completion = completedMap[quest.id]
                    PwaQuestCard(
                        quest = quest,
                        completion = completion,
                        onClick = { onQuestClick(quest) }
                    )
                }
            }
        }
    }
}

@Composable
fun PwaQuestCard(
    quest: Quest,
    completion: Completion?,
    onClick: () -> Unit
) {
    val isDone = completion != null
    val typeColor = when (quest.type.lowercase()) {
        "ville" -> TypeVille
        "colline" -> TypeColline
        "site-naturel" -> TypeNaturel
        "sommet" -> TypeSommet
        else -> Terracotta
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFF9FDF9) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDone) Color(0xFFA5D6A7) else BorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ligne supérieure : Badge Type + Statut Pilule + Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de Type coloré
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor
                ) {
                    Text(
                        text = quest.type.replace("-", " ").uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pilule de statut (Style PWA : À faire ou Validée)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDone) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = if (isDone) "✅ Validée" else "⏳ À faire",
                            color = if (isDone) Color(0xFF2E7D32) else Color(0xFFF57F17),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Badge Points
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Terracotta.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "+${quest.points} pts",
                            color = Terracotta,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Titre de la quête
            Text(
                text = quest.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaquisGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Métadonnées : Ville & Altitude
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📍 ${quest.cityId.replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
                quest.altitudeM?.let {
                    Text(
                        text = "⛰️ $it m",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Aperçu de la description
            Text(
                text = quest.description,
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp,
                maxLines = 2
            )
        }
    }
}
