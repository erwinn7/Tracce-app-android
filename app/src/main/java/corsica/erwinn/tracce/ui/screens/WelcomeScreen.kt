package corsica.erwinn.tracce.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.R
import corsica.erwinn.tracce.ui.theme.GoldScore
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta

data class OnboardingStep(
    val icon: String,
    val stepNumber: String,
    val title: String,
    val description: String,
    val accentColor: Color
)

@Composable
fun WelcomeScreen(
    username: String,
    isAdmin: Boolean,
    score: Int,
    completedCount: Int,
    totalQuests: Int,
    isRadarActive: Boolean,
    isDemoMode: Boolean,
    onToggleRadar: () -> Unit,
    onToggleDemoMode: () -> Unit,
    onStartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAdminClick: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val steps = remember(totalQuests) {
        listOf(
            OnboardingStep(
                icon = "🗺️",
                stepNumber = "ÉTAPE 1",
                title = "Choisissez votre lieu",
                description = "Parcourez $totalQuests sites corses : citadelles génoises, places ombragées, cascades et crêtes rocheuses.",
                accentColor = Color(0xFF3B82F6)
            ),
            OnboardingStep(
                icon = "📸",
                stepNumber = "ÉTAPE 2",
                title = "Prenez votre photo",
                description = "Rejoignez le site sur le terrain et capturez une photo avec votre smartphone pour attester de votre passage.",
                accentColor = Terracotta
            ),
            OnboardingStep(
                icon = "🛰️",
                stepNumber = "ÉTAPE 3",
                title = "Validez par GPS",
                description = "Le récepteur satellite contrôle votre position géographique en direct dans le rayon requis pour créditer vos points.",
                accentColor = MaquisGreen
            ),
            OnboardingStep(
                icon = "🏆",
                stepNumber = "ÉTAPE 4",
                title = "Épinglez vos exploits",
                description = "Plantez vos punaises rouges sur la carte de Corse, gravissez les échelons et devenez Maître des Traces.",
                accentColor = GoldScore
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SandLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HERO BANNER STYLE APPLICATION MOBILE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_home),
                    contentDescription = "Explorateur en Corse",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dégradé sombre pour lisibilité
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )

                // Badge thématique
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaquisGreen,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🌿 Exploration insulaire",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Badge score fluide
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clickable { onProfileClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⭐ $score pts", color = GoldScore, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$completedCount / $totalQuests", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // 2. CORPS DE LA PAGE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titre moderne
                Column {
                    Text(
                        text = "Explorez la Corse à travers l’objectif",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaquisGreen,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rally photo géolocalisé au cœur des trésors d’Ajaccio, Bastia, Corte et des sommets environnants.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }

                // 3. CAROUSEL SWIPEABLE MODERNE (HorizontalPager avec indicateurs fluides)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comment jouer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaquisGreen
                        )
                        Text(
                            text = "Glisser pour découvrir →",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        pageSpacing = 12.dp
                    ) { page ->
                        val step = steps[page]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(step.accentColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = step.icon, fontSize = 28.sp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.stepNumber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = step.accentColor,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = step.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaquisGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = step.description,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Indicateurs de pages (Dots animés)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(steps.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 8.dp,
                                label = "dotWidth"
                            )
                            val color = if (isSelected) MaquisGreen else Color.LightGray

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(7.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }

                // 4. BOUTON D'ACTION PRINCIPAL
                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Découvrir les quêtes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "→", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                // Grille 2 cartes d'accès rapide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProfileClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Mon Profil", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaquisGreen)
                                Text("Carte & badges", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAdminClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👑", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Espace Admin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Terracotta)
                                    Text("Créer un lieu", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // 5. CONTRÔLES MOBILES (SANS AUCUNE PARENTHÈSE)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Fonctionnalités & Capteurs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaquisGreen
                        )

                        // Radar GPS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📡 Radar GPS Proximité", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Surveillance active à moins de 250 mètres", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isRadarActive,
                                onCheckedChange = { onToggleRadar() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2E7D32),
                                    checkedTrackColor = Color(0xFFA5D6A7)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // Mode Démo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚡ Mode Démo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFE65100))
                                Text("Validation immédiate sans contrainte de distance", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isDemoMode,
                                onCheckedChange = { onToggleDemoMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFE65100),
                                    checkedTrackColor = Color(0xFFFFCC80)
                                )
                            )
                        }
                    }
                }

                // Indication hors-ligne sobre
                Text(
                    text = "⚡ Fonctionne à 100 % hors ligne avec Room SQLite",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
