package com.music.violence.playback.autoeq

import kotlinx.serialization.Serializable

/**
 * Filter types supported by AutoEQ parametric profiles.
 */
@Serializable
enum class AutoEqFilterType {
    PEAKING,
    LOW_SHELF,
    HIGH_SHELF;

    companion object {
        fun fromString(str: String): AutoEqFilterType {
            val upper = str.uppercase().trim()
            return when (upper) {
                "LSC", "LS", "LOW_SHELF", "LOWSHELF" -> LOW_SHELF
                "HSC", "HS", "HIGH_SHELF", "HIGHSHELF" -> HIGH_SHELF
                else -> PEAKING
            }
        }
    }
}

/**
 * A single biquad / state-variable filter band within an AutoEQ profile.
 */
@Serializable
data class AutoEqFilter(
    val type: AutoEqFilterType,
    val frequencyHz: Float,
    val gainDb: Float,
    val q: Float,
)

/**
 * Full parametric calibration profile for a specific headphone measurement.
 */
@Serializable
data class AutoEqProfile(
    val model: String,
    val source: String,
    val category: String,
    val preampDb: Float,
    val filters: List<AutoEqFilter>,
) {
    companion object {
        val FLAT = AutoEqProfile(
            model = "Flat / Bypass",
            source = "None",
            category = "none",
            preampDb = 0f,
            filters = emptyList(),
        )
    }
}

/**
 * Lightweight summary used for rapid search and selection without loading all filter details.
 */
@Serializable
data class AutoEqProfileSummary(
    val id: Int,
    val model: String,
    val source: String,
    val category: String,
)

