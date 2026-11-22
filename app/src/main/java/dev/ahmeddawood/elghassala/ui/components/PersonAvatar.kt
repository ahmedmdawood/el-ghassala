package dev.ahmeddawood.elghassala.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ahmeddawood.elghassala.settings.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Small shared cache prevents decoding the same photo again each time its row re-enters view. */
private object AvatarBitmaps {
    private val cache = object : android.util.LruCache<String, android.graphics.Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
    }
    fun load(file: File): ImageBitmap? {
        val key = file.absolutePath
        val bitmap = cache.get(key) ?: android.graphics.BitmapFactory.decodeFile(key)?.also { cache.put(key, it) }
        return bitmap?.asImageBitmap()
    }
}

private val avatarColors = listOf(
    Color(0xFF0E7C66), Color(0xFFB07E1E), Color(0xFF3E7A6A), Color(0xFFC0563E),
    Color(0xFF5B6FB5), Color(0xFF8E5BA8), Color(0xFF2F8FA3), Color(0xFF7C8B2E),
)

/** Stable color for a name, so the same person always looks the same. */
fun avatarColor(name: String): Color = avatarColors[(name.trim().lowercase().hashCode() and 0x7fffffff) % avatarColors.size]

fun initialOf(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

@Composable
fun PersonAvatar(
    name: String,
    avatar: Avatar?,
    photoFile: ((Avatar) -> File)?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val base = modifier.size(size).clip(CircleShape)
    when {
        avatar != null && avatar.isPhoto && photoFile != null -> {
            val bmp by produceState<ImageBitmap?>(null, avatar.value) {
                value = withContext(Dispatchers.IO) {
                    runCatching { AvatarBitmaps.load(photoFile(avatar)) }.getOrNull()
                }
            }
            val b = bmp
            if (b != null) {
                Image(b, null, base, contentScale = ContentScale.Crop)
            } else {
                Box(base.background(avatarColor(name)), contentAlignment = Alignment.Center) {
                    Text(initialOf(name), color = Color.White, fontSize = (size.value * 0.42f).sp)
                }
            }
        }
        avatar != null && avatar.isEmoji -> Box(
            base.background(avatarColor(name).copy(alpha = 0.22f)), contentAlignment = Alignment.Center,
        ) { Text(avatar.value, fontSize = (size.value * 0.52f).sp) }
        else -> Box(base.background(avatarColor(name)), contentAlignment = Alignment.Center) {
            Text(initialOf(name), color = Color.White, fontSize = (size.value * 0.42f).sp)
        }
    }
}
