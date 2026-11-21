package dev.ahmeddawood.elghassala

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ahmeddawood.elghassala.settings.AppLanguage
import dev.ahmeddawood.elghassala.settings.AppTheme
import dev.ahmeddawood.elghassala.ui.nav.AppNavHost
import dev.ahmeddawood.elghassala.ui.theme.ElGhassalaTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val theme by vm.theme.collectAsState()
            val language by vm.language.collectAsState()

            // Per-app locale so Android rebuilds resources with the right
            // strings and RTL direction.
            val applied = androidx.compose.runtime.remember { HashSet<AppLanguage>() }
            if (applied.add(language)) {
                val tags = when (language) {
                    AppLanguage.ARABIC -> "ar"
                    AppLanguage.ENGLISH -> "en"
                    AppLanguage.SYSTEM -> ""
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
            }

            val dark = when (theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            ElGhassalaTheme(darkTheme = dark) {
                AppNavHost(vm)
            }
        }
    }
}
