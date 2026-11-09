package corsica.erwinn.tracce.ui.screens

import android.graphics.BitmapFactory
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.ui.components.LeafletMapView
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    username: String,
    isAdmin: Boolean,
    score: Int,
    quests: List<Quest>,
    completions: List<Completion>,
    onQuestClick: (Quest) -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val completedIds = remember(completions) { completions.map { it.questId }.toSet() }

    // Titre d'enquêteur inspiré de la PWA
    val rank = remember(completions.size) {
        val count = completions.size
        when {
            count == 0 -> "Novice du Maquis"
            count < 3 -> "Éclaireur des Sentiers"
            count < 6 -> "Arpenteur Insulaire"
            count < 10 -> "Enquêteur Chevronné"
            else -> "Maître des Traces de Corse 🏆"
        }
    }

    // Villes visitées
    val visitedCitiesCount = remember(completions, quests) {
        val cities = mutableSetOf<String>()
        completions.forEach { comp ->
            quests.find { it.id == comp.questId }?.let { cities.add(it.cityId.lowercase()) }
        }
        cities.size
    }

    // Quête sélectionnée sur la carte
    var selectedQuestId by remember { mutableStateOf<String?>(null) }
    val selectedQuest = remember(selectedQuestId, quests) {
        quests.find { it.id == selectedQuestId }
    }
    val selectedCompletion = remember(selectedQuestId, completions) {
        completions.find { it.questId == selectedQuestId }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SandLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CARTE PRINCIPALE D'EXPLORATION (GRANDE & IMMERSIVE)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Carte de Corse",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaquisGreen
                        )
                        Text(
                            text = "Épingles d'enquêteur sur les lieux explorés",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Terracotta.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "📌 ${completions.size} lieu${if (completions.size > 1) "x" else ""} exploré${if (completions.size > 1) "s" else ""}",
                            color = Terracotta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Boutons de recentrage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.recenterCorsica();", null)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎯 Recentrer Corse", fontSize = 12.sp)
                    }

                    if (completions.isNotEmpty()) {
                        Button(
                            onClick = {
                                webViewRef?.evaluateJavascript("window.fitPins();", null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📌 Mes Épingles", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grande carte Leaflet interactive qui remplit parfaitement son conteneur
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp)
                ) {
                    LeafletMapView(
                        quests = quests,
                        completedIds = completedIds,
                        selectedQuestId = selectedQuestId,
                        onSelectQuest = { q ->
                            selectedQuestId = q.id
                        },
                        onWebViewReady = { webViewRef = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // FICHE POLAROID DE L'ÉPINGLE SÉLECTIONNÉE
                AnimatedVisibility(
                    visible = selectedQuest != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    selectedQuest?.let { q ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8E5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (q.id in completedIds) "📍 Épingle d'enquête active" else "🗺️ Quête à découvrir",
                                        color = Terracotta,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = { selectedQuestId = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaquisGreen
                                )
                                Text(
                                    text = q.description,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    maxLines = 2
                                )

                                selectedCompletion?.photoPath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                        if (bmp != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Photo prise",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onQuestClick(q) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaquisGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Consulter la quête →", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. CARTE IDENTITÉ ENQUÊTEUR & STATISTIQUES
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaquisGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAdmin) "👑" else "🕵️‍♂️",
                                fontSize = 26.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (username != "Explorateur") username else "Explorateur Anonyme",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaquisGreen
                                )
                                if (isAdmin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Terracotta
                                    ) {
                                        Text(
                                            text = "Admin",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = rank,
                                fontSize = 13.sp,
                                color = Terracotta,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (username != "Explorateur") {
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Déconnexion", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onOpenAuth,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Connexion", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grille de statistiques
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        value = "$score",
                        label = "Points d'Enquête",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "${completions.size} / ${quests.size.coerceAtLeast(12)}",
                        label = "Lieux Épinglés",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "$visitedCitiesCount / 3",
                        label = "Villes Explorées",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. CARNET DE BORD DES EXPLORATIONS (GALERIE PHOTOS)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📸 Carnet de bord des explorations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaquisGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vos souvenirs photographiques immortalisés sur le terrain en Corse.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (completions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SandLight, RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🕵️‍♂️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Votre carnet de bord est encore vierge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaquisGreen
                            )
                            Text(
                                text = "Parcourez la Corse et validez une quête pour enregistrer votre première photo !",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(completions) { comp ->
                            val quest = quests.find { it.id == comp.questId }
                            CarnetPhotoCard(
                                questName = quest?.name ?: comp.questId,
                                points = quest?.points ?: 10,
                                completedAt = comp.completedAt,
                                photoPath = comp.photoPath,
                                onClick = {
                                    quest?.let { onQuestClick(it) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Marge inférieure pour éviter le chevauchement avec la barre flottante du bas
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SandLight,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaquisGreen
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun CarnetPhotoCard(
    questName: String,
    points: Int,
    completedAt: Long,
    photoPath: String?,
    onClick: () -> Unit
) {
    val dateStr = remember(completedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)
        sdf.format(Date(completedAt))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SandLight),
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.LightGray)
            ) {
                if (photoPath != null && File(photoPath).exists()) {
                    val bmp = BitmapFactory.decodeFile(photoPath)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = questName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("📷 Photo validée", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                    color = Terracotta,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "+$points pts",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = questName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = MaquisGreen
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
