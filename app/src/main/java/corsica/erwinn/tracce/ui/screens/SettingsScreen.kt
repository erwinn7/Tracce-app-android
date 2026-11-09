package corsica.erwinn.tracce.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import corsica.erwinn.tracce.R
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import corsica.erwinn.tracce.utils.LocationHelper

@Composable
fun SettingsScreen(
    username: String,
    isAdmin: Boolean,
    isRadarActive: Boolean,
    isDemoMode: Boolean,
    onToggleRadar: () -> Unit,
    onToggleDemoMode: () -> Unit,
    onSyncClick: () -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit,
    onReplayTutorial: () -> Unit
) {
    val context = LocalContext.current
    var showRadarPermissionRationale by rememberSaveable { mutableStateOf(false) }

    val radarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            onToggleRadar()
        } else {
            showRadarPermissionRationale = true
        }
    }

    if (showRadarPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showRadarPermissionRationale = false },
            title = { Text("Permissions requises pour le Radar", fontWeight = FontWeight.Bold) },
            text = {
                Text("Le Radar GPS de proximité surveille les sentiers en arrière-plan et vous alerte par notification à l'approche des monuments (<250m). Veuillez autoriser la localisation.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRadarPermissionRationale = false
                        val neededPerms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            neededPerms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        radarPermissionLauncher.launch(neededPerms.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                ) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRadarPermissionRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SandLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CARTE COMPTE & SESSION
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Compte Explorateur",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaquisGreen
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (username != "Explorateur") username else "Explorateur Anonyme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A201C)
                        )
                        Text(
                            text = if (isAdmin) "👑 Session Administrateur" else "Explorateur standard",
                            fontSize = 12.sp,
                            color = if (isAdmin) Terracotta else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (username != "Explorateur") {
                        OutlinedButton(
                            onClick = {
                                onLogout()
                                Toast.makeText(context, "Session fermée", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Déconnexion", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onOpenAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Se connecter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. FONCTIONNALITÉS NATIVES & CAPTEURS (SANS AUCUNE PARENTHÈSE)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Capteurs & Fonctionnalités Natives",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaquisGreen
                )

                // Radar GPS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📡 Radar GPS Proximité", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Vibre en tâche de fond à moins de 250 mètres d'un site", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isRadarActive,
                        onCheckedChange = {
                            if (isRadarActive) {
                                onToggleRadar()
                            } else {
                                val hasLocation = LocationHelper.hasLocationPermission(context)
                                val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                } else true

                                if (hasLocation && hasNotification) {
                                    onToggleRadar()
                                } else {
                                    val neededPerms = mutableListOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        neededPerms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    radarPermissionLauncher.launch(neededPerms.toTypedArray())
                                }
                            }
                        },
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
                        Text("⚡ Mode Démo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFE65100))
                        Text("Validation instantanée sans contrainte de distance", fontSize = 12.sp, color = Color.Gray)
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

        // 3. SYNCHRONISATION & OUTBOX WORKMANAGER
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Synchronisation & Données",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaquisGreen
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SandLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSyncClick()
                            Toast.makeText(context, "Tâche WorkManager Outbox déclenchée !", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaquisGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Forcer Synchronisation Cloud", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Enclenche le SyncWorker en arrière-plan", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SandLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReplayTutorial() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Terracotta)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Revoir les tuiles explicatives", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Rejouer l'animation de présentation et les 3 étapes", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 4. À PROPOS
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tracce_logo),
                        contentDescription = "Logo Tracce",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Text("Tracce Corse", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaquisGreen)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Tracce est un rallye photo géolocalisé invitant à explorer les trésors patrimoniaux, citadelles génoises et crêtes rocheuses de Corse. Rendez-vous sur chaque site, capturez votre cliché et validez votre position par GPS pour inscrire vos exploits dans votre carnet d'enquêteur.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Version 1.0 • Université de Corse Pasquale Paoli", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
