package com.haziel.ofmusic.ui.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haziel.ofmusic.domain.tier.UserTier
import com.haziel.ofmusic.ui.main.MusicUiState
import com.haziel.ofmusic.ui.main.MusicViewModel
import com.haziel.ofmusic.ui.main.components.ProfileAvatar
import com.haziel.ofmusic.ui.theme.liquidGlass

@Composable
fun AccountScreen(
    state: MusicUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPlans: () -> Unit
) {
    val tier = state.authProfile?.asUserTier() ?: UserTier.FREE
    val effectivePhotoUri = state.userProfile.photoUri?.ifBlank { null } ?: state.authProfile?.photoUrl
    val memberSince = state.authProfile?.getMemberSinceFormatted() ?: "Nov 2023"
    val birthDateText = state.userProfile.birthDate.ifBlank { "14 de Agosto, 1998" }
    val countryText = state.userProfile.country.ifBlank { "México (MX)" }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showResetStorageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(Brush.verticalGradient(listOf(Color(0xFF2E1065).copy(alpha = 0.2f), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ─── Top Bar ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.liquidGlass(CircleShape, alpha = 0.1f).size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text("Mi Cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.weight(1f))

                // Badge VIP
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("VIP HI-FI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ─── Header Perfil ───
            Column(
                modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), alpha = 0.08f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ProfileAvatar(photoUri = effectivePhotoUri, name = state.userProfile.name, size = 90.dp)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable { onEditProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.userProfile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(state.userProfile.email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.1f).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Miembro desde $memberSince", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Box(modifier = Modifier.liquidGlass(RoundedCornerShape(50), alpha = 0.1f).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Plan ${tier.displayName}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .liquidGlass(RoundedCornerShape(12.dp), alpha = 0.12f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar Perfil y Fotografía", fontWeight = FontWeight.Medium)
                }
            }

            // ─── Información Personal ───
            SectionTitle("INFORMACIÓN PERSONAL")
            InfoRow(title = "Nombre Completo", subtitle = state.userProfile.name, icon = Icons.Default.Badge, isEditable = true, onClick = onEditProfile)
            InfoRow(title = "Fecha de Nacimiento", subtitle = birthDateText, icon = Icons.Default.Cake, isEditable = true, onClick = onEditProfile)
            InfoRow(title = "País / Región", subtitle = countryText, icon = Icons.Default.Public, isEditable = true, onClick = onEditProfile)

            // ─── Suscripción y Facturación ───
            SectionTitle("SUSCRIPCIÓN & FACTURACIÓN")
            Box(
                modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp), alpha = 0.05f).padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("PLAN ACTIVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Hi-Fi Lossless Familiar", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("$129 MXN / mes • 6 cuentas", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Banner de Próximamente
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFA500).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFFA500).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚧 Gestión de suscripción próximamente", color = Color(0xFFFFA500), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).liquidGlass(RoundedCornerShape(12.dp), alpha = 0.05f).padding(12.dp)) {
                            Column {
                                Text("PRÓXIMA RENOVACIÓN", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                                Text("15 Marzo 2025", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).liquidGlass(RoundedCornerShape(12.dp), alpha = 0.05f).padding(12.dp)) {
                            Column {
                                Text("MÉTODO DE PAGO", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("•••• 4821", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ─── Seguridad ───
            SectionTitle("SEGURIDAD DE LA CUENTA")
            InfoRow(title = "Cambiar Contraseña", subtitle = "Actualizada hace 4 meses", icon = Icons.Default.Lock, isEditable = false)

            // 2FA
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Autenticación en dos pasos", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Próximamente", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                    Text("Protegido con aplicación autenticadora", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }

            // Dispositivos
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dispositivos Vinculados", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Próximamente", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                        }
                    }
                    Text("3 dispositivos autorizados", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }

            // ─── Privacidad ───
            SectionTitle("PRIVACIDAD DE PERFIL")
            Box(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Configuración de Privacidad", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Box(modifier = Modifier.background(Color(0xFFFFA500).copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("PRÓXIMAMENTE", color = Color(0xFFFFA500), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Perfil público, compartir actividad de escucha y permitir seguidores.", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Acciones Finales ───
            // Cerrar Sesión (Funcional)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).clickable { viewModel.signOut(); onBack() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF5252))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cerrar Sesión", color = Color(0xFFFF5252), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Desconectar cuenta en este teléfono", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }

            // Eliminar Cuenta (Próximamente)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Eliminar Cuenta Permanentemente", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Próximamente", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                    Text("Borrar biblioteca, favoritos y datos", color = Color.White.copy(alpha = 0.3f), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Footer (Año Dinámico)
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("OfMusic v3.4.1 (Build 2024)", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.labelSmall)
                Text("© $currentYear OfMusic • Android SDK Lossless Engine", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp))
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEditable: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(RoundedCornerShape(16.dp), alpha = 0.05f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Text(subtitle, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        if (isEditable) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}