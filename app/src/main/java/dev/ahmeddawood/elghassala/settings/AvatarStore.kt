package dev.ahmeddawood.elghassala.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** A person's picture: a photo kept in app storage, or an emoji. No picture means colored initials. */
data class Avatar(val kind: String, val value: String) {
    val isPhoto get() = kind == "photo"
    val isEmoji get() = kind == "emoji"
}

/**
 * Avatars live outside the Room database on purpose: no schema change, so updating over
 * v1.0.1/v1.1 can never risk existing data. Map of personId -> Avatar in DataStore.
 */
class AvatarStore(private val context: Context, private val settings: SettingsRepository) {

    private val key = stringPreferencesKey("avatars")
    private val dir get() = File(context.filesDir, "avatars").apply { mkdirs() }

    val avatars: Flow<Map<Long, Avatar>> = settings.rawFlow().map { prefs -> parse(prefs[key]) }

    private fun parse(json: String?): Map<Long, Avatar> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(json)
            o.keys().asSequence().mapNotNull { k ->
                val id = k.toLongOrNull() ?: return@mapNotNull null
                val a = o.optJSONObject(k) ?: return@mapNotNull null
                id to Avatar(a.optString("k"), a.optString("v"))
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun encode(map: Map<Long, Avatar>): String {
        val o = JSONObject()
        map.forEach { (id, a) -> o.put(id.toString(), JSONObject().put("k", a.kind).put("v", a.value)) }
        return o.toString()
    }

    private suspend fun update(block: (MutableMap<Long, Avatar>) -> Unit) {
        settings.editRaw { prefs ->
            val m = parse(prefs[key]).toMutableMap()
            block(m)
            prefs[key] = encode(m)
        }
    }

    suspend fun setEmoji(personId: Long, emoji: String) {
        deleteFile(personId)
        update { it[personId] = Avatar("emoji", emoji) }
    }

    /** Center-crops to a square and stores a 320px JPEG. Returns false when the image cannot be read. */
    suspend fun setPhoto(personId: Long, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 640 && bounds.outHeight / (sample * 2) >= 640) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching false
            val side = minOf(src.width, src.height)
            val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
            val out = Bitmap.createScaledBitmap(square, 320, 320, true)
            val file = File(dir, "p$personId-${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            deleteFile(personId)
            update { it[personId] = Avatar("photo", file.name) }
            true
        }.getOrDefault(false)
    }

    suspend fun clear(personId: Long) {
        deleteFile(personId)
        update { it.remove(personId) }
    }

    fun photoFile(a: Avatar): File = File(dir, a.value)

    private suspend fun deleteFile(personId: Long) {
        // Remove any previous photo for this person.
        dir.listFiles()?.filter { it.name.startsWith("p$personId-") }?.forEach { it.delete() }
    }
}
