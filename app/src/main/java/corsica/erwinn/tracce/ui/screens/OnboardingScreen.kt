package corsica.erwinn.tracce.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.R
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.MaquisGreenDark
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingTile(
    val number: String,
    val icon: String,
    val title: String,
    val description: String,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    // Étape 1 : Splash d'introduction animé, puis passage aux 3 grandes tuiles swipeables
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2200) // 2.2s de belle animation d'intro
        showSplash = false
    }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
        },
        label = "onboarding_transition"
    ) { isSplash ->
        if (isSplash) {
            SplashIntroView(onSkip = { showSplash = false })
        } else {
            TilesCarouselView(onComplete = onComplete)
        }
    }
}

@Composable
private fun SplashIntroView(onSkip: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaquisGreen, MaquisGreenDark)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tracce_logo),
                    contentDescription = "Logo Tracce",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tracce",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Rallye photo géolocalisé en Corse",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Terracotta,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TilesCarouselView(
    onComplete: () -> Unit
) {
    val tiles = remember {
        listOf(
            OnboardingTile(
                number = "01",
                icon = "🗺️",
                title = "Choisissez votre lieu",
                description = "Parcourez 12 quêtes réelles à travers la Corse : citadelles génoises, cascades sauvages, places ombragées et sommets rocheux.",
                accentColor = Color(0xFF3B82F6)
            ),
            OnboardingTile(
                number = "02",
                icon = "📸",
                title = "Prenez votre photo",
                description = "Rendez-vous sur le terrain et capturez le monument avec votre appareil photo pour prouver votre présence.",
                accentColor = Terracotta
            ),
            OnboardingTile(
                number = "03",
                icon = "📍",
                title = "Validez par GPS",
                description = "Le positionnement satellite vérifie votre proximité dans le rayon requis pour débloquer vos points et enrichir votre carnet de voyage.",
                accentColor = MaquisGreen
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { tiles.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SandLight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête sobre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DÉCOUVERTE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Terracotta,
                    letterSpacing = 1.5.sp
                )
                TextButton(onClick = onComplete) {
                    Text("Passer", color = Color.Gray, fontSize = 13.sp)
                }
            }

            // 3 GRANDES TUILES SWIPEABLES
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                pageSpacing = 16.dp
            ) { page ->
                val tile = tiles[page]
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tile.accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "ÉTAPE ${tile.number}",
                                color = tile.accentColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(tile.accentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = tile.icon, fontSize = 54.sp)
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = tile.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaquisGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = tile.description,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }

            // Pied de page : Dots indicateurs + Bouton Suivant / Commencer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicateurs de pagination
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(tiles.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            label = "dotWidth"
                        )
                        val color = if (isSelected) MaquisGreen else Color.LightGray

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < tiles.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == tiles.size - 1) "Commencer l'exploration" else "Continuer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "→", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
