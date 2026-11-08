package com.haziel.ofmusic.data.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Maneja autenticación y perfil del usuario en Supabase.
 *
 * Uso típico:
 *   val result = authRepo.signUp("user@mail.com", "password", "Nombre")
 *   result.fold(
 *     onSuccess = { profile -> /* usuario registrado */ },
 *     onFailure = { error -> /* mostrar error */ }
 *   )
 */
class AuthRepository {

    private val tag = "AuthRepository"
    private val client = SupabaseClientProvider.getInstance()
    private val auth = client.auth
    private val postgrest = client.postgrest

    // ═══════════════════════════════════════════════════════════════
    //  Sesión
    // ═══════════════════════════════════════════════════════════════

    /**
     * True si hay un usuario logueado actualmente.
     */
    fun isLoggedIn(): Boolean = auth.currentSessionOrNull() != null

    /**
     * Devuelve el ID del usuario actual, o null si no hay sesión.
     */
    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    /**
     * Cierra la sesión actual.
     */
    suspend fun signOut() {
        try {
            auth.signOut()
            Log.d(tag, "✓ Sesión cerrada")
        } catch (e: Exception) {
            Log.e(tag, "Error cerrando sesión: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Registro
    // ═══════════════════════════════════════════════════════════════

    /**
     * Registra un nuevo usuario con email y contraseña.
     * Si Supabase tiene "Confirm email" activado, el usuario deberá
     * confirmar su correo antes de poder loguearse.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<ProfileDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Registrando: $email")
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("display_name", displayName)
                }
            }
            // Tras el registro, intentamos obtener el perfil
            val profile = fetchProfile()
            if (profile != null) {
                Log.d(tag, "✓ Registro exitoso: ${profile.email}")
                Result.success(profile)
            } else {
                Result.failure(Exception("No se pudo crear el perfil"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error en registro: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Login
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicia sesión con email y contraseña.
     */
    suspend fun signIn(
        email: String,
        password: String
    ): Result<ProfileDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Iniciando sesión: $email")
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val profile = fetchProfile()
            if (profile != null) {
                Log.d(tag, "✓ Login exitoso: ${profile.email}")
                Result.success(profile)
            } else {
                Result.failure(Exception("No se pudo cargar el perfil"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error en login: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Obtener perfil
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene el perfil del usuario actual desde la tabla `profiles`.
     * Devuelve null si no hay sesión o si la fila no existe.
     */
    suspend fun fetchProfile(): ProfileDto? = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId() ?: return@withContext null
            val profile = postgrest
                .from("profiles")
                .select(Columns.list("id", "email", "display_name", "photo_url", "birth_date", "country", "tier", "premium_since", "stripe_customer_id", "created_at", "updated_at")) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileDto>()
            Log.d(tag, "✓ Perfil cargado: ${profile?.email} (tier=${profile?.tier})")
            profile
        } catch (e: Exception) {
            Log.e(tag, "Error obteniendo perfil: ${e.message}", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Actualizar perfil
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza el nombre, foto, fecha de nacimiento y país del usuario actual.
     */
    suspend fun updateProfile(
        displayName: String?,
        photoUrl: String?,
        birthDate: String? = null,
        country: String? = null
    ): Result<ProfileDto> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId()
                ?: return@withContext Result.failure(Exception("No hay sesión activa"))

            val update = buildJsonObject {
                displayName?.let { put("display_name", it) }
                photoUrl?.let { put("photo_url", it) }
                birthDate?.let { put("birth_date", it) }
                country?.let { put("country", it) }
            }

            postgrest.from("profiles").update(update) {
                filter { eq("id", userId) }
            }

            val profile = fetchProfile()
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("No se pudo actualizar el perfil"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error actualizando perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.updateUser {
                password = newPassword
            }
            Log.d(tag, "✓ Contraseña actualizada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error actualizando contraseña: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountPermanent(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId()
                ?: return@withContext Result.failure(Exception("No hay sesión activa"))

            runCatching { postgrest.from("favorites").delete { filter { eq("user_id", userId) } } }
            runCatching { postgrest.from("user_playlists").delete { filter { eq("user_id", userId) } } }
            runCatching { postgrest.from("profiles").delete { filter { eq("id", userId) } } }

            signOut()
            Log.d(tag, "✓ Cuenta eliminada permanentemente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando cuenta: ${e.message}", e)
            Result.failure(e)
        }
    }
}