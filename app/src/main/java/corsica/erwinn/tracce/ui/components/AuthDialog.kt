package corsica.erwinn.tracce.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import corsica.erwinn.tracce.ui.theme.Terracotta

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Boolean
) {
    val context = LocalContext.current
    var inputUser by rememberSaveable { mutableStateOf("") }
    var inputPass by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Terracotta) },
        title = { Text("Connexion Explorateur", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Saisissez votre pseudo pour enregistrer vos validations et vos points sur le terrain.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = inputUser,
                    onValueChange = { inputUser = it },
                    label = { Text("Pseudo") },
                    placeholder = { Text("Votre nom ou prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputPass,
                    onValueChange = { inputPass = it },
                    label = { Text("Mot de passe admin") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = inputUser.trim()
                    if (u.isBlank()) {
                        Toast.makeText(context, "Veuillez entrer un pseudo valide", Toast.LENGTH_SHORT).show()
                    } else {
                        val success = onLogin(u, inputPass.trim())
                        if (success) {
                            val welcomeText = if (u.equals("erwinn", ignoreCase = true) && inputPass.trim() == "pwaErwinn") {
                                "Session Administrateur activée. Bienvenue, $u !"
                            } else {
                                "Bienvenue, $u !"
                            }
                            Toast.makeText(context, welcomeText, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erreur de connexion", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Se connecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
