package com.afterhours.app.ads

import android.app.Activity
import android.content.Context
import com.afterhours.core.AdPlacement
import com.afterhours.core.AdPolicy
import com.afterhours.core.AdsProvider
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AdMob-backed [AdsProvider] (test ads only, see [AdIds]).
 * Interstitial and rewarded ads are only ever shown from non-gameplay moments
 * (the game-over transition), never while a card is on screen.
 */
class AdManager(context: Context, private val policy: AdPolicy = AdPolicy()) : AdsProvider by policy {
    private val appContext = context.applicationContext
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var interstitialLoading = false
    private var rewardedLoading = false

    private val _rewardedReady = MutableStateFlow(false)
    /** True when a rewarded ad is loaded and the bonus button can be offered. */
    val rewardedReady: StateFlow<Boolean> = _rewardedReady

    private var initialized = false

    private val _ready = MutableStateFlow(false)
    /** True once the SDK was started after consent; banners wait for this. */
    val ready: StateFlow<Boolean> = _ready

    /** Set from the "Remove ads" purchase. Banners and interstitials stop; the optional rewarded bonus becomes free. */
    @Volatile var adsRemoved: Boolean = false

    /** Call only after consent has been gathered and [ConsentManager.canRequestAds] is true. */
    fun initialize() {
        if (initialized) return
        initialized = true
        // Adults-only app: never request child-directed or under-age treatment, cap content at mature.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_MA)
                .build(),
        )
        MobileAds.initialize(appContext) {
            _ready.value = true
            loadInterstitial()
            loadRewarded()
        }
    }

    private fun loadInterstitial() {
        if (interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(appContext, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; interstitialLoading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; interstitialLoading = false }
            })
    }

    private fun loadRewarded() {
        if (rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(appContext, AdIds.REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedLoading = false; _rewardedReady.value = true }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null; rewardedLoading = false; _rewardedReady.value = false }
            })
    }

    /**
     * Call once when a game has finished. Shows a capped interstitial over the
     * results screen if the policy allows and one is loaded; otherwise does nothing.
     */
    fun onSessionCompleted(activity: Activity) {
        policy.onSessionCompleted()
        if (adsRemoved || !initialized) return
        val ad = interstitial
        if (ad == null) { loadInterstitial(); return }
        if (!shouldShow(AdPlacement.SESSION_END_INTERSTITIAL)) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { interstitial = null; loadInterstitial() }
        }
        policy.onInterstitialShown()
        ad.show(activity)
    }

    /** Optional rewarded bonus. [onReward] runs only if the user earned the reward. */
    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewarded ?: return
        rewarded = null
        _rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadRewarded() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { loadRewarded() }
        }
        ad.show(activity) { onReward() }
    }
}
