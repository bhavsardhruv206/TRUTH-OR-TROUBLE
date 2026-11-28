package com.afterhours.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.ui.AfterHoursApp
import com.afterhours.app.ui.AfterHoursTheme
import com.afterhours.app.ui.isDark
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    private val ads by lazy { AdManager(this) }
    private val consent by lazy { ConsentManager(this) }
    private val billing by lazy { BillingManager(this) { owned -> vm.setAdsRemoved(owned) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { vm.adsRemoved.collect { ads.adsRemoved = it } }
        // Consent first (shown only where required), then ads. Consent from an earlier
        // launch lets ads start right away while the update runs.
        if (consent.canRequestAds) ads.initialize()
        consent.gather(this) { if (consent.canRequestAds) ads.initialize() }
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = settings.theme.isDark()
            AfterHoursTheme(dark) {
                SystemBars(dark)
                AfterHoursApp(vm, ads, consent, billing)
            }
        }
    }

    /** Match the status and navigation bars to the theme, with icons that stay visible on either. */
    @Composable
    private fun SystemBars(dark: Boolean) {
        val bars = MaterialTheme.colorScheme.background.toArgb()
        SideEffect {
            @Suppress("DEPRECATION")
            window.statusBarColor = bars
            @Suppress("DEPRECATION")
            window.navigationBarColor = bars
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    override fun onResume() {
        super.onResume()
        billing.connect() // picks up purchases completed outside the app
    }

    override fun onDestroy() {
        billing.close()
        super.onDestroy()
    }
}
