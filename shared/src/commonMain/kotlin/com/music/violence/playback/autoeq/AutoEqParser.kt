package com.music.violence.playback.autoeq

/**
 * Parser for Equalizer APO / AutoEQ ParametricEQ.txt file formats.
 * Allows users to paste or import custom parametric equalization profiles.
 */
object AutoEqParser {

    private val PREAMP_REGEX = Regex("""Preamp:\s*([-\d.]+)\s*dB""", RegexOption.IGNORE_CASE)
    private val FILTER_REGEX = Regex(
        """Filter\s+\d+:\s+(ON|OFF)\s+([A-Za-z]+)\s+Fc\s+([\d.]+)\s*Hz\s+Gain\s+([-\d.]+)\s*dB\s+Q\s+([\d.]+)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Parses a ParametricEQ.txt content string into an [AutoEqProfile].
     */
    fun parse(rawText: String, name: String = "Custom Profile", source: String = "User Import"): AutoEqProfile {
        var preamp = 0f
        val preampMatch = PREAMP_REGEX.find(rawText)
        if (preampMatch != null) {
            preamp = preampMatch.groupValues[1].toFloatOrNull() ?: 0f
        }

        val filters = mutableListOf<AutoEqFilter>()
        for (match in FILTER_REGEX.findAll(rawText)) {
            val state = match.groupValues[1].uppercase()
            if (state == "ON") {
                val typeStr = match.groupValues[2]
                val fc = match.groupValues[3].toFloatOrNull() ?: continue
                val gain = match.groupValues[4].toFloatOrNull() ?: continue
                val q = match.groupValues[5].toFloatOrNull() ?: 0.707f

                filters.add(
                    AutoEqFilter(
                        type = AutoEqFilterType.fromString(typeStr),
                        frequencyHz = fc.coerceIn(10f, 22_000f),
                        gainDb = gain.coerceIn(-24f, 24f),
                        q = q.coerceIn(0.1f, 15f),
                    ),
                )
            }
        }

        return AutoEqProfile(
            model = name,
            source = source,
            category = "custom",
            preampDb = preamp.coerceIn(-30f, 12f),
            filters = filters,
        )
    }
}

