package com.music.violence.playback.autoeq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Access layer for the complete bundled AutoEQ database containing 8,850+ headphone profiles.
 *
 * Lazily decompresses and indexes on first query, providing sub-millisecond search
 * across all measured models and sources.
 */
object AutoEqDatabase {

    @Volatile
    var streamProvider: (() -> InputStream?)? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class RawEntry(
        @SerialName("m") val model: String,
        @SerialName("s") val source: String,
        @SerialName("c") val category: String,
        @SerialName("p") val preamp: Float,
        @SerialName("f") val filters: List<RawFilter>,
    )

    @Serializable
    private data class RawFilter(
        @SerialName("t") val type: String,
        @SerialName("f") val freq: Float,
        @SerialName("g") val gain: Float,
        @SerialName("q") val q: Float,
    )

    private class NormalizedSummary(
        val summary: AutoEqProfileSummary,
        val cleanModel: String,
        val cleanSource: String,
        val compactModel: String,
    )

    private var entries: List<AutoEqProfile>? = null
    private var summaries: List<AutoEqProfileSummary>? = null
    private var normalizedSummaries: List<NormalizedSummary>? = null

    val isLoaded: Boolean
        get() {
            val list = entries
            return list != null && list.isNotEmpty()
        }

    val totalCount: Int
        get() = summaries?.size ?: 0

    @Synchronized
    fun ensureLoaded(customStream: InputStream? = null) {
        if (entries != null && entries!!.isNotEmpty()) return

        val stream: InputStream? = customStream
            ?: streamProvider?.invoke()
            ?: AutoEqDatabase::class.java.getResourceAsStream("/autoeq/autoeq_database.json.gz")
            ?: AutoEqDatabase::class.java.classLoader?.getResourceAsStream("autoeq/autoeq_database.json.gz")
            ?: Thread.currentThread().contextClassLoader?.getResourceAsStream("autoeq/autoeq_database.json.gz")
            ?: AutoEqDatabase::class.java.getResourceAsStream("/assets/autoeq/autoeq_database.json.gz")

        if (stream == null) {
            entries = emptyList()
            summaries = emptyList()
            normalizedSummaries = emptyList()
            return
        }

        try {
            val decompressed = GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rawList = json.decodeFromString<List<RawEntry>>(decompressed)

            val parsedEntries = ArrayList<AutoEqProfile>(rawList.size)
            val parsedSummaries = ArrayList<AutoEqProfileSummary>(rawList.size)
            val parsedNorm = ArrayList<NormalizedSummary>(rawList.size)

            rawList.forEachIndexed { index, raw ->
                val profile = AutoEqProfile(
                    model = raw.model,
                    source = raw.source,
                    category = raw.category,
                    preampDb = raw.preamp,
                    filters = raw.filters.map { rf ->
                        AutoEqFilter(
                            type = AutoEqFilterType.fromString(rf.type),
                            frequencyHz = rf.freq,
                            gainDb = rf.gain,
                            q = rf.q,
                        )
                    },
                )
                val summary = AutoEqProfileSummary(
                    id = index,
                    model = raw.model,
                    source = raw.source,
                    category = raw.category,
                )
                parsedEntries.add(profile)
                parsedSummaries.add(summary)
                parsedNorm.add(
                    NormalizedSummary(
                        summary = summary,
                        cleanModel = cleanForSearch(raw.model),
                        cleanSource = cleanForSearch(raw.source),
                        compactModel = raw.model.lowercase().replace("[^a-z0-9]".toRegex(), ""),
                    ),
                )
            }

            entries = parsedEntries
            summaries = parsedSummaries
            normalizedSummaries = parsedNorm
        } catch (_: Throwable) {
            entries = emptyList()
            summaries = emptyList()
            normalizedSummaries = emptyList()
        }
    }

    private fun cleanForSearch(s: String): String {
        return s.lowercase().replace("['’`-]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * Searches profiles by headphone model name, brand, or measurement source.
     * Uses fuzzy token and punctuation-insensitive matching.
     */
    fun search(query: String, limit: Int = 80): List<AutoEqProfileSummary> {
        ensureLoaded()
        val allNorm = normalizedSummaries ?: return emptyList()
        val q = query.trim()
        if (q.isEmpty()) {
            return allNorm.take(limit).map { it.summary }
        }

        val qLower = q.lowercase()
        val qClean = cleanForSearch(q)
        val qCompact = qLower.replace("[^a-z0-9]".toRegex(), "")
        val terms = qClean.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val exactMatches = ArrayList<AutoEqProfileSummary>()
        val fuzzyMatches = ArrayList<AutoEqProfileSummary>()

        for (item in allNorm) {
            val model = item.summary.model
            val source = item.summary.source

            // 1. Exact substring match on original text
            if (model.contains(q, ignoreCase = true) || source.contains(q, ignoreCase = true)) {
                exactMatches.add(item.summary)
                if (exactMatches.size >= limit) return exactMatches
                continue
            }

            // 2. Multi-term punctuation-insensitive matching
            // e.g. "Tangzu Waner" matches "TANGZU Wan'er S.G"
            var allTermsMatch = true
            for (term in terms) {
                val termCompact = term.replace("[^a-z0-9]".toRegex(), "")
                val matchesTerm = item.cleanModel.contains(term) ||
                    item.cleanSource.contains(term) ||
                    (termCompact.isNotEmpty() && item.compactModel.contains(termCompact))
                if (!matchesTerm) {
                    allTermsMatch = false
                    break
                }
            }

            if (allTermsMatch) {
                fuzzyMatches.add(item.summary)
                if (exactMatches.size + fuzzyMatches.size >= limit) break
                continue
            }

            // 3. Compact matching (e.g. "hd600" matches "Sennheiser HD 600", "xm4" matches "WH-1000XM4")
            if (qCompact.length >= 3 && item.compactModel.contains(qCompact)) {
                fuzzyMatches.add(item.summary)
                if (exactMatches.size + fuzzyMatches.size >= limit) break
            }
        }

        val combined = ArrayList<AutoEqProfileSummary>(exactMatches.size + fuzzyMatches.size)
        combined.addAll(exactMatches)
        combined.addAll(fuzzyMatches)
        return combined.take(limit)
    }

    /**
     * Retrieves the complete profile with filters by index ID.
     */
    fun getProfile(id: Int): AutoEqProfile? {
        ensureLoaded()
        val list = entries ?: return null
        return if (id in list.indices) list[id] else null
    }

    /**
     * Finds a profile by model name and optional measurement source.
     */
    fun findProfile(model: String, source: String? = null): AutoEqProfile? {
        ensureLoaded()
        val list = entries ?: return null
        return list.firstOrNull {
            it.model.equals(model, ignoreCase = true) &&
                (source == null || it.source.equals(source, ignoreCase = true))
        }
    }

    /**
     * Curated list of popular headphone and IEM models for quick discovery.
     */
    fun popularSummaries(): List<AutoEqProfileSummary> {
        ensureLoaded()
        val all = summaries ?: return emptyList()
        val popularKeywords = listOf(
            "AirPods Pro",
            "AirPods Max",
            "WH-1000XM5",
            "WH-1000XM4",
            "WF-1000XM5",
            "HD 600",
            "HD 650",
            "HD 560S",
            "Blessing 2",
            "Blessing 3",
            "Moondrop Chu",
            "Moondrop Aria",
            "QuietComfort 45",
            "QuietComfort Ultra",
            "Galaxy Buds2 Pro",
            "Galaxy Buds Pro",
            "DT 770 Pro",
            "DT 990 Pro",
            "DT 1990 Pro",
            "ATH-M50x",
            "Sundara",
            "Wan'er",
        )

        val result = LinkedHashSet<AutoEqProfileSummary>()
        for (kw in popularKeywords) {
            all.firstOrNull { it.model.contains(kw, ignoreCase = true) }?.let { result.add(it) }
        }
        return result.toList()
    }
}
