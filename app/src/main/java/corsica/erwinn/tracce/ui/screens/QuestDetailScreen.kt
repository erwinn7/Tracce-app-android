package corsica.erwinn.tracce.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.ui.theme.GoldScore
import corsica.erwinn.tracce.ui.theme.MaquisGreen
import corsica.erwinn.tracce.ui.theme.SandLight
import corsica.erwinn.tracce.ui.theme.Terracotta
import corsica.erwinn.tracce.utils.DeviceLocation
import corsica.erwinn.tracce.utils.GeoUtils
import corsica.erwinn.tracce.utils.LocationHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreen(
    quest: Quest,
    completion: Completion?,
    isDemoMode: Boolean,
    onToggleDemoMode: () -> Unit,
    onBackClick: () -> Unit,
    onValidateClick: (userLat: Double, userLng: Double, photoPath: String?, onResult: (Result<Completion>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isCompleted = completion != null

    // Fichier temporaire pour la photo capturée
    val draftFile = remember(quest.id) {
        File(context.filesDir, "photos/quest_${quest.id}_draft.jpg")
    }

    // États de capture photo avec rememberSaveable (Slides 5 & 9) :
    // - Survit à la rotation
    // - Survit à la mort du process en arrière-plan (am kill) via le Bundle
    // - Est réinitialisé lors d'une fermeture par l'utilisateur (swipe-away des récentes)
    var capturedPhotoPath by rememberSaveable {
        mutableStateOf(completion?.photoPath)
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(quest.id) {
        // En cas de démarrage à froid sans complétion (swipe-away des récentes), on nettoie l'ancien brouillon
        if (completion == null && capturedPhotoPath == null && draftFile.exists()) {
            draftFile.delete()
        }
    }

    // Chargement de la photo existante ou du brouillon
    LaunchedEffect(capturedPhotoPath) {
        val path = capturedPhotoPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                capturedBitmap = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    // États de géolocalisation et messages
    var isLocating by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<DeviceLocation?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }

    // Sauvegarde d'un bitmap dans le stockage interne (fichier brouillon pour résister à la mort du process)
    fun savePhoto(bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "quest_${quest.id}_draft.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Launcher Appareil Photo
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val path = savePhoto(bitmap)
            capturedPhotoPath = path
            Toast.makeText(context, "📸 Photo enregistrée !", Toast.LENGTH_SHORT).show()
        }
    }

    // Fonction de déclenchement de la géolocalisation et validation
    val performValidationWithGps: () -> Unit = {
        if (!LocationHelper.isGpsEnabled(context)) {
            errorMessage = "Le capteur GPS de votre appareil est désactivé. Veuillez l'activer dans vos paramètres Android."
        } else {
            isLocating = true
            errorMessage = null
            coroutineScope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                isLocating = false
                if (loc == null) {
                    errorMessage = "Impossible de récupérer votre signal GPS. Assurez-vous d'être à ciel ouvert, ou activez le Mode Démo pour vos tests."
                } else {
                    userLocation = loc
                    val dist = GeoUtils.haversineDistanceMeters(loc.latitude, loc.longitude, quest.lat, quest.lng).toInt()

                    onValidateClick(loc.latitude, loc.longitude, capturedPhotoPath) { result ->
                        result.onSuccess {
                            val permanentFile = File(context.filesDir, "photos/quest_${quest.id}_completed.jpg")
                            if (draftFile.exists()) {
                                draftFile.renameTo(permanentFile)
                                capturedPhotoPath = permanentFile.absolutePath
                            }
                            val distText = if (dist >= 1000) String.format(java.util.Locale.FRANCE, "%.1f km", dist / 1000.0) else "$dist m"
                            successMessage = if (isDemoMode) {
                                "🎉 Quête validée avec succès en Mode Démo • Distance réelle : $distText"
                            } else {
                                "🎉 Quête validée sur place ! Vous étiez à $distText de la cible."
                            }
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Validation impossible."
                        }
                    }
                }
            }
        }
    }

    // Launcher Permission GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            performValidationWithGps()
        } else {
            showPermissionRationale = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quest.name, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SandLight)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bandeau Mode Démo
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDemoMode) Color(0xFFFFF3E0) else Color(0xFFECEFF1)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDemoMode) "⚡ Mode Démo ACTIF" else "🛰️ Mode Réel GPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDemoMode) Color(0xFFE65100) else Color(0xFF37474F)
                            )
                        }
                        Text(
                            text = if (isDemoMode)
                                "Validation autorisée sans contrainte de distance pour vos démonstrations."
                            else
                                "Contrôle strict : présence requise dans le rayon de ${quest.radiusM} mètres.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
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

            // Badge Territoire & Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Territoire : ${quest.cityId.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    color = Terracotta,
                    fontSize = 14.sp
                )

                Badge(containerColor = Terracotta) {
                    Text(
                        text = "+${quest.points} POINTS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Fiche descriptive
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Description du site",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaquisGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = quest.description,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.DarkGray
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Données géographiques de la cible",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaquisGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Coordonnées cibles : ${quest.lat}, ${quest.lng}", fontSize = 13.sp)
                    quest.altitudeM?.let {
                        Text("• Altitude cible : $it mètres", fontSize = 13.sp)
                    }
                    Text("• Rayon de validation requis : ${quest.radiusM} mètres", fontSize = 13.sp)

                    userLocation?.let { loc ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        val dist = GeoUtils.haversineDistanceMeters(loc.latitude, loc.longitude, quest.lat, quest.lng).toInt()
                        val distStr = if (dist >= 1000) String.format(java.util.Locale.FRANCE, "%.1f km", dist / 1000.0) else "$dist m"
                        Text(
                            text = "🛰️ Votre position détectée :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Terracotta
                        )
                        Text("• Coordonnées : ${loc.latitude}, ${loc.longitude}", fontSize = 12.sp)
                        Text("• Précision : ±${loc.accuracy.toInt()} m", fontSize = 12.sp)
                        Text("• Distance au site : $distStr", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section Photo (Feature native appareil photo)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📷 Preuve photo sur le terrain",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaquisGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (capturedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Photo prise",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { takePictureLauncher.launch(null) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (capturedBitmap != null) "Reprendre la photo" else "Prendre une photo du site")
                    }
                }
            }

            // Message d'erreur
            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Message de succès
            if (successMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = successMessage!!,
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Statut ou Bouton de validation
            if (isCompleted) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaquisGreen.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaquisGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quête validée avec succès !",
                                fontWeight = FontWeight.Bold,
                                color = MaquisGreen
                            )
                            Text(
                                text = "Points crédités à votre profil et enregistrés dans la base locale.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            performValidationWithGps()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isLocating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Acquisition du signal GPS...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Valider ma présence par GPS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // Modal expliquant le besoin de permission GPS
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Terracotta) },
            title = { Text("Permission GPS requise") },
            text = {
                Text(
                    "L'application Tracce a besoin d'accéder à votre position GPS afin de vérifier que vous êtes bien sur place pour valider la quête.\n\nVous pouvez autoriser la permission, ou activer le 'Mode Démo' pour tester l'application sans vous déplacer.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Autoriser le GPS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Plus tard")
                }
            }
        )
    }
}
