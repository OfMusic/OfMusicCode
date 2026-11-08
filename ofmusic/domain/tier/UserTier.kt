package com.haziel.ofmusic.domain.tier

/**
 * Tier del usuario. El rawValue es el string que vive en Supabase (profiles.tier).
 * Acepta "eco" como alias legacy de "standard" para no romper datos previos.
 */
enum class UserTier(val rawValue: String) {
    FREE("free"),
    STANDARD("standard"),
    PRO("pro");

    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            STANDARD -> "Standard"
            PRO -> "Pro"
        }

    companion object {
        fun fromRaw(value: String?): UserTier = when (value?.lowercase()?.trim()) {
            "standard", "eco" -> STANDARD
            "pro" -> PRO
            else -> FREE
        }
    }
}