package corsica.erwinn.tracce.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.R
import corsica.erwinn.tracce.ui.theme.GoldScore
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.MaquisGreenDark
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta

@Composable
fun AppDrawerContent(
    currentRoute: String,
    username: String,
    isAdmin: Boolean,
    score: Int,
    isRadarActive: Boolean,
    isDemoMode: Boolean,
    onNavigate: (String) -> Unit,
    onToggleRadar: () -> Unit,
    onToggleDemoMode: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = SandLight,
        drawerContentColor = Color(0xFF1A201C),
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête vert avec logo officiel et identité voyageur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaquisGreen)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.tracce_logo),
                            contentDescription = "Logo Tracce",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Tracce",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Rally photo en Corse",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Carte utilisateur intégrée dans le header
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (username != "Explorateur") username else "Mode Visiteur",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isAdmin) "👑 Administrateur" else "Palmarès : $score pts",
                                    fontSize = 12.sp,
                                    color = if (isAdmin) Terracotta else GoldScore,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (username != "Explorateur") {
                                TextButton(
                                    onClick = onLogout,
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Quitter", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onClose()
                                        onOpenAuth()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Connexion", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation principale
            Text(
                text = "NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Accueil", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "welcome",
                onClick = {
                    onClose()
                    onNavigate("welcome")
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                label = { Text("Catalogue des Quêtes", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "quests",
                onClick = {
                    onClose()
                    onNavigate("quests")
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("📍 Profil & Carte d'Enquêteur", fontWeight = FontWeight.Medium) },
                selected = currentRoute == "profile",
                onClick = {
                    onClose()
                    onNavigate("profile")
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            if (isAdmin) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Terracotta) },
                    label = { Text("👑 Espace Administrateur", fontWeight = FontWeight.Bold, color = Terracotta) },
                    selected = currentRoute == "admin",
                    onClick = {
                        onClose()
                        onNavigate("admin")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))

            // Réglages & Outils
            Text(
                text = "FONCTIONNALITÉS NATIVES & TESTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Switch Radar GPS
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📡 Radar GPS Proximité", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Surveillance active à moins de 250 mètres", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isRadarActive,
                        onCheckedChange = { onToggleRadar() }
                    )
                }
            }

            // Switch Mode Démo
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ Mode Démo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text("Validation sans contrainte GPS", fontSize = 11.sp, color = Color.Gray)
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

            // Forcer Synchronisation WorkManager
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clickable {
                        onSyncClick()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaquisGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("🔄 Synchronisation Cloud", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("WorkManager Outbox en tâche de fond", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
