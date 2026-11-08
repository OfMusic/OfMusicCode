package com.haziel.ofmusic.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("country") val country: String? = null,
    val tier: String = "free",
    @SerialName("premium_since") val premiumSince: String? = null,
    @SerialName("stripe_customer_id") val stripeCustomerId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun isPremium(): Boolean = tier == "standard" || tier == "eco" || tier == "pro"
    fun isPro(): Boolean = tier == "pro"
    fun asUserTier(): com.haziel.ofmusic.domain.tier.UserTier =
        com.haziel.ofmusic.domain.tier.UserTier.fromRaw(tier)

    fun getMemberSinceFormatted(): String {
        val raw = createdAt ?: return "Nov 2023"
        return runCatching {
            val dateStr = raw.substringBefore("T")
            val parts = dateStr.split("-")
            val year = parts.getOrNull(0) ?: "2024"
            val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
            val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            val monthName = months.getOrNull(monthNum - 1) ?: "Ene"
            "$monthName $year"
        }.getOrDefault("Nov 2023")
    }
}