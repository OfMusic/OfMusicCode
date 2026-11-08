package com.haziel.ofmusic.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.haziel.ofmusic.ui.theme.liquidGlass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.haziel.ofmusic.ui.main.components.OfMusicLogoIcon
import com.haziel.ofmusic.ui.main.components.ProfileAvatar
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun AuthScreen(
    errorMessage: String?,
    isLoading: Boolean,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (name: String, email: String, pass: String, photoUri: String?) -> Unit,
    onClearError: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    isRegisteringA: Boolean = false
) {
    var isRegistering by remember { mutableStateOf(isRegisteringA) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) photoUri = uri.toString() }

    LaunchedEffect(isRegisteringA) {
        isRegistering = isRegisteringA
    }

    // Fondo oscuro con un resplandor púrpura sutil en el centro
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent),
                    radius = 1000f
                )
            )
    ) {



        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {

            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .liquidGlass(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
            }
            // Logo superior
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .liquidGlass(CircleShape, alpha = 0.1f),
                contentAlignment = Alignment.Center
            ) {
                OfMusicLogoIcon(size = 120.dp)
            }

            Spacer(Modifier.height(16.dp))
            Text("OfMusic", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                text = if (isRegistering) "Crea tu cuenta" else "Bienvenido de vuelta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Contenedor Glass del Formulario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(32.dp), alpha = 0.05f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRegistering) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfileAvatar(photoUri = photoUri, name = name.ifBlank { "U" }, size = 88.dp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Elegir foto", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    AuthTextField(value = name, onValueChange = { name = it; onClearError() }, label = "Nombre", leadingIcon = Icons.Default.Person, enabled = !isLoading)
                    Spacer(Modifier.height(16.dp))
                }

                AuthTextField(value = email, onValueChange = { email = it; onClearError() }, label = "Correo electrónico", leadingIcon = Icons.Default.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), enabled = !isLoading)
                Spacer(Modifier.height(16.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; onClearError() },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                        }
                    }
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Botón Principal (LiquidGlass)
                Button(
                    onClick = { if (isRegistering) onRegister(name, email, password, photoUri) else onLogin(email, password) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .liquidGlass(RoundedCornerShape(50), alpha = 0.25f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegistering) "Crear Cuenta" else "Iniciar Sesión", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.clickable(enabled = !isLoading) { isRegistering = !isRegistering; onClearError() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isRegistering) "¿Ya tienes cuenta? " else "¿No tienes cuenta? ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isRegistering) "Inicia Sesión" else "Regístrate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }


            Spacer(Modifier.height(24.dp))

            // Botón Invitado (Glass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .liquidGlass(RoundedCornerShape(50), alpha = 0.08f)
                    .clickable(enabled = !isLoading, onClick = onContinueAsGuest),
                contentAlignment = Alignment.Center
            ) {
                Text("Continuar sin cuenta", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        singleLine = true,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.03f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
        )
    )
}