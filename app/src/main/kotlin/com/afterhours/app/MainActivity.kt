package com.afterhours.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.ui.AfterHoursApp
import com.afterhours.app.ui.AfterHoursTheme
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
        setContent { AfterHoursTheme { AfterHoursApp(vm, ads, consent, billing) } }
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
