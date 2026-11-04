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

    private var entries: List<AutoEqProfile>? = null
    private var summaries: List<AutoEqProfileSummary>? = null

    val isLoaded: Boolean
        get() = entries != null

    val totalCount: Int
        get() = summaries?.size ?: 0

    @Synchronized
    fun ensureLoaded() {
        if (entries != null) return

        val stream: InputStream? = AutoEqDatabase::class.java.getResourceAsStream("/autoeq/autoeq_database.json.gz")
            ?: AutoEqDatabase::class.java.classLoader?.getResourceAsStream("autoeq/autoeq_database.json.gz")

        if (stream == null) {
            entries = emptyList()
            summaries = emptyList()
            return
        }

        try {
            val decompressed = GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rawList = json.decodeFromString<List<RawEntry>>(decompressed)

            val parsedEntries = ArrayList<AutoEqProfile>(rawList.size)
            val parsedSummaries = ArrayList<AutoEqProfileSummary>(rawList.size)

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
                parsedEntries.add(profile)
                parsedSummaries.add(
                    AutoEqProfileSummary(
                        id = index,
                        model = raw.model,
                        source = raw.source,
                        category = raw.category,
                    ),
                )
            }

            entries = parsedEntries
            summaries = parsedSummaries
        } catch (_: Throwable) {
            entries = emptyList()
            summaries = emptyList()
        }
    }

    /**
     * Searches profiles by headphone model name, brand, or measurement source.
     */
    fun search(query: String, limit: Int = 60): List<AutoEqProfileSummary> {
        ensureLoaded()
        val all = summaries ?: return emptyList()
        val q = query.trim()
        if (q.isEmpty()) {
            return all.take(limit)
        }

        val terms = q.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val matches = ArrayList<AutoEqProfileSummary>()

        for (item in all) {
            var allMatch = true
            for (term in terms) {
                if (!item.model.contains(term, ignoreCase = true) &&
                    !item.source.contains(term, ignoreCase = true)
                ) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) {
                matches.add(item)
                if (matches.size >= limit) break
            }
        }
        return matches
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
        )

        val result = LinkedHashSet<AutoEqProfileSummary>()
        for (kw in popularKeywords) {
            all.firstOrNull { it.model.contains(kw, ignoreCase = true) }?.let { result.add(it) }
        }
        return result.toList()
    }
}

