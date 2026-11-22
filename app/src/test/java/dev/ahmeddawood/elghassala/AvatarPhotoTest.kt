package dev.ahmeddawood.elghassala

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dev.ahmeddawood.elghassala.settings.AvatarStore
import dev.ahmeddawood.elghassala.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AvatarPhotoTest {
    @Test fun storedPhotoSurvivesSourceRemovalAndStoreRecreation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val settings = SettingsRepository(context)
        val store = AvatarStore(context, settings)
        val source = File(context.cacheDir, "avatar-source.png")
        val bitmap = Bitmap.createBitmap(100, 160, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(store.setPhoto(971L, Uri.fromFile(source)))
        val first = store.avatars.first().getValue(971L)
        assertTrue(store.photoFile(first).exists())
        assertNotNull(BitmapFactory.decodeFile(store.photoFile(first).path))
        assertTrue(store.setPhoto(971L, Uri.fromFile(source)))
        val second = store.avatars.first().getValue(971L)
        assertTrue(store.photoFile(second).exists())
        source.delete()
        val reopened = AvatarStore(context, settings)
        assertTrue(reopened.photoFile(reopened.avatars.first().getValue(971L)).exists())
        store.clear(971L)
    }
}
