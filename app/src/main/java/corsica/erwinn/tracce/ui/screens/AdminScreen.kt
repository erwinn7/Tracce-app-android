package corsica.erwinn.tracce.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import corsica.erwinn.tracce.utils.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onAddQuest: (Quest, () -> Unit) -> Unit,
    onSyncClick: () -> Unit,
    onBackClick: () -> Unit,
    customQuests: List<Quest> = emptyList(),
    onDeleteQuest: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity

    // Nettoyage de l'ancien cache SharedPreferences s'il existe pour repartir sur un cycle de vie standard
    LaunchedEffect(Unit) {
        context.getSharedPreferences("tracce_admin_draft", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    // Gestion standard du cycle de vie Android / Compose (Slides 5, 8, 9 & 14) :
    // - Survit à la rotation de l'écran via le Bundle (savedInstanceState)
    // - Survit au kill du process par le système en arrière-plan (am kill) via le Bundle
    // - Est RÉINITIALISÉ à neuf lorsque l'utilisateur quitte volontairement l'application (swipe-away des applications récentes)
    var name by rememberSaveable { mutableStateOf("") }
    var cityId by rememberSaveable { mutableStateOf("ajaccio") }
    var type by rememberSaveable { mutableStateOf("ville") }
    var description by rememberSaveable { mutableStateOf("") }
    var latStr by rememberSaveable { mutableStateOf("") }
    var lngStr by rememberSaveable { mutableStateOf("") }
    var altitudeStr by rememberSaveable { mutableStateOf("") }
    var radiusStr by rememberSaveable { mutableStateOf("100") }
    var pointsStr by rememberSaveable { mutableStateOf("10") }

    var isLocating by remember { mutableStateOf(false) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var successMsg by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val cities = listOf(
        "ajaccio" to "Ajaccio",
        "bastia" to "Bastia",
        "corte" to "Corte"
    )

    val types = listOf(
        "ville" to "Patrimoine & Ville",
        "site-naturel" to "Site Naturel",
        "sommet" to "Sommet & Crête",
        "colline" to "Colline"
    )

    val fetchLocation: () -> Unit = {
        if (!LocationHelper.isGpsEnabled(context)) {
            errorMsg = "Le GPS est désactivé. Veuillez l'activer dans les réglages Android."
        } else {
            isLocating = true
            errorMsg = null
            coroutineScope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                isLocating = false
                if (loc != null) {
                    latStr = String.format(java.util.Locale.US, "%.7f", loc.latitude)
                    lngStr = String.format(java.util.Locale.US, "%.7f", loc.longitude)
                    loc.altitude?.let { alt ->
                        altitudeStr = alt.toInt().toString()
                    }
                    Toast.makeText(context, "Position GPS capturée avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    errorMsg = "Impossible de capter le signal GPS. Assurez-vous d'être à ciel ouvert."
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            if (shouldShowRationale) {
                showRationaleDialog = true
            } else {
                showSettingsDialog = true
            }
        }
    }

    val requestLocationWithRationale: () -> Unit = {
        if (LocationHelper.hasLocationPermission(context)) {
            fetchLocation()
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            if (shouldShowRationale) {
                showRationaleDialog = true
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaquisGreen) },
            title = { Text("Permission Localisation requise") },
            text = {
                Text("Pour remplir automatiquement les coordonnées GPS de ce nouveau lieu avec votre position actuelle, Tracce a besoin d'accéder au capteur GPS.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaquisGreen)
                ) {
                    Text("Autoriser l'accès GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Terracotta) },
            title = { Text("Activer la Localisation") },
            text = {
                Text("La permission localisation a été refusée avec 'Ne plus demander'. Pour capturer votre position actuelle, veuillez activer la permission dans les paramètres de l'application.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaquisGreen)
                ) {
                    Text("Ouvrir Paramètres")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Annuler", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administration Tracce", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour Profil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaquisGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SandLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Carte explicative
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaquisGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajout d'un nouveau lieu au catalogue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaquisGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Les nouveaux lieux sont inscrits dans la base locale Room et synchronisés vers le Cloud Firestore (Pattern Outbox hors-ligne).",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }

            // Formulaire
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nom
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            successMsg = false
                        },
                        label = { Text("Nom du lieu") },
                        placeholder = { Text("Ex: Citadelle génoise de Calvi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Ville / Territoire
                    Text("Territoire corse", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cities.forEach { (id, label) ->
                            FilterChip(
                                selected = cityId == id,
                                onClick = {
                                    cityId = id
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Typologie
                    Text("Typologie du site", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        types.take(2).forEach { (id, label) ->
                            FilterChip(
                                selected = type == id,
                                onClick = {
                                    type = id
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        types.drop(2).forEach { (id, label) ->
                            FilterChip(
                                selected = type == id,
                                onClick = {
                                    type = id
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            successMsg = false
                        },
                        label = { Text("Description & Histoire") },
                        placeholder = { Text("Contexte historique, patrimoine...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Coordonnées GPS avec bouton de capture directe
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coordonnées GPS", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        TextButton(
                            onClick = requestLocationWithRationale,
                            enabled = !isLocating
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaquisGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recherche GPS...", fontSize = 11.sp, color = MaquisGreen)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaquisGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ma position GPS", fontSize = 11.sp, color = MaquisGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latStr,
                            onValueChange = {
                                latStr = it
                                successMsg = false
                            },
                            label = { Text("Latitude") },
                            placeholder = { Text("41.9180") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngStr,
                            onValueChange = {
                                lngStr = it
                                successMsg = false
                            },
                            label = { Text("Longitude") },
                            placeholder = { Text("8.7386") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Altitude & Rayon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = altitudeStr,
                            onValueChange = {
                                altitudeStr = it
                            },
                            label = { Text("Altitude (m)") },
                            placeholder = { Text("Ex: 15") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = radiusStr,
                            onValueChange = {
                                radiusStr = it
                            },
                            label = { Text("Rayon (m)") },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Points accordés
                    OutlinedTextField(
                        value = pointsStr,
                        onValueChange = {
                            pointsStr = it
                        },
                        label = { Text("Points d'expérience") },
                        placeholder = { Text("10, 25, 50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Message d'erreur éventuel
                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bouton Enregistrer avec protection anti-doublon et état de soumission
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            val latVal = latStr.toDoubleOrNull()
                            val lngVal = lngStr.toDoubleOrNull()
                            val radiusVal = radiusStr.toIntOrNull() ?: 100
                            val pointsVal = pointsStr.toIntOrNull() ?: 10
                            val altVal = altitudeStr.toIntOrNull()

                            if (name.isBlank()) {
                                errorMsg = "Veuillez saisir un nom pour le lieu."
                            } else if (latVal == null || lngVal == null) {
                                errorMsg = "Coordonnées GPS invalides."
                            } else {
                                errorMsg = null
                                isSubmitting = true

                                // Identifiant déterministe basé sur la ville et le nom pour éviter les doublons accidentels
                                val cleanSlug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                                val deterministicId = "custom_${cityId}_$cleanSlug"

                                val newQuest = Quest(
                                    id = deterministicId,
                                    name = name.trim(),
                                    cityId = cityId,
                                    lat = latVal,
                                    lng = lngVal,
                                    description = description.ifBlank { "Nouveau site ajouté par l'administrateur." },
                                    points = pointsVal,
                                    radiusM = radiusVal,
                                    altitudeM = altVal,
                                    type = type,
                                    source = "custom"
                                )

                                onAddQuest(newQuest) {
                                    isSubmitting = false
                                    successMsg = true
                                    name = ""
                                    description = ""
                                    latStr = ""
                                    lngStr = ""
                                    altitudeStr = ""
                                    Toast.makeText(context, "Lieu enregistré avec succès dans le catalogue Room !", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaquisGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrement en cours...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrer dans le Catalogue Room", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    // Message de succès animé
                    AnimatedVisibility(visible = successMsg, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Lieu ajouté avec succès dans le catalogue Room (synchronisation Outbox active) !",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Gestion des lieux personnalisés existants (permet de supprimer facilement les doublons)
            if (customQuests.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaquisGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lieux créés par l'Admin (${customQuests.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaquisGreen
                            )
                        }
                        Text(
                            text = "Supprimez un lieu ou un doublon directement d'un simple toucher.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        customQuests.forEach { quest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quest.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "${quest.cityId.uppercase()} • ${quest.type.uppercase()} • +${quest.points} pts",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteQuest(quest.id)
                                        Toast.makeText(context, "Lieu '${quest.name}' supprimé", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = Terracotta
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Bouton Outbox WorkManager
            OutlinedButton(
                onClick = {
                    onSyncClick()
                    Toast.makeText(context, "Tâche WorkManager déclenchée !", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaquisGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forcer Synchronisation Cloud", color = MaquisGreen)
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
