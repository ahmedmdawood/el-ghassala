package dev.ahmeddawood.elghassala.playful

import android.content.Context
import org.json.JSONArray

/**
 * Data-driven jokes: each bucket is a JSON array of strings in assets/jokes/<bucket>.json.
 * Picks are random but remember the last few so the same joke does not come back quickly.
 */
class JokeRepository(private val context: Context) {

    private val cache = HashMap<String, List<String>>()
    private val recent = HashMap<String, ArrayDeque<String>>()

    fun count(bucket: String): Int = list(bucket).size

    fun pick(bucket: String): String? {
        val all = list(bucket)
        if (all.isEmpty()) return null
        val hist = recent.getOrPut(bucket) { ArrayDeque() }
        val candidates = all.filter { it !in hist }.ifEmpty { all }
        val joke = candidates.random()
        hist.addLast(joke)
        val keep = minOf(15, all.size / 2)
        while (hist.size > keep) hist.removeFirst()
        return joke
    }

    fun allBuckets(): List<String> =
        runCatching { context.assets.list("jokes")?.map { it.removeSuffix(".json") } }.getOrNull().orEmpty()

    @Synchronized
    private fun list(bucket: String): List<String> = cache.getOrPut(bucket) {
        runCatching {
            val text = context.assets.open("jokes/$bucket.json").use { it.readBytes().toString(Charsets.UTF_8) }
            val arr = JSONArray(text)
            List(arr.length()) { arr.getString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
