package com.haziel.ofmusic.domain.model

data class UserProfile(
    val name: String = "Usuario OfMusic",
    val email: String = "usuario@ofmusic.com",
    val birthDate: String = "14 de Agosto, 1998",
    val country: String = "México (MX)",
    val avatarUrl: String = "",
    val photoUri: String? = null,
    val isAdmin: Boolean = false
)