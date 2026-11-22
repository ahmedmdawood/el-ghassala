package dev.ahmeddawood.elghassala

import android.app.Application
import dev.ahmeddawood.elghassala.data.db.AppDatabase
import dev.ahmeddawood.elghassala.data.repo.GhassalaRepository
import dev.ahmeddawood.elghassala.settings.SettingsRepository

class ElGhassalaApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: GhassalaRepository by lazy { GhassalaRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val avatarStore: dev.ahmeddawood.elghassala.settings.AvatarStore by lazy { dev.ahmeddawood.elghassala.settings.AvatarStore(this, settings) }
    val jokes: dev.ahmeddawood.elghassala.playful.JokeRepository by lazy { dev.ahmeddawood.elghassala.playful.JokeRepository(this) }
}
