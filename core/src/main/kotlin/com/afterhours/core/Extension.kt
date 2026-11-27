package com.afterhours.core

/**
 * Ad seam. The UI asks the provider whether a placement may be shown; the
 * AdMob-backed implementation lives in the app module. Ads must never appear
 * while a card (question or dare) is on screen: only home/setup banners, a
 * capped interstitial after a completed session, and an optional rewarded bonus.
 */
enum class AdPlacement { HOME_BANNER, SESSION_END_INTERSTITIAL, REWARDED_BONUS }

interface AdsProvider {
    fun shouldShow(placement: AdPlacement): Boolean
}

/** No ads. */
object NoAds : AdsProvider {
    override fun shouldShow(placement: AdPlacement) = false
}

/**
 * Pure frequency-cap logic (no Android dependencies, unit tested).
 * An interstitial is allowed only after every [sessionsPerInterstitial]-th completed
 * session AND at least [minIntervalMs] after the previous interstitial.
 */
class AdPolicy(
    private val sessionsPerInterstitial: Int = 2,
    private val minIntervalMs: Long = 120_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) : AdsProvider {
    private var completedSessions = 0
    private var lastInterstitialAt: Long? = null

    /** Call once each time a game finishes. */
    fun onSessionCompleted() { completedSessions++ }

    /** Call when an interstitial was actually displayed. */
    fun onInterstitialShown() { lastInterstitialAt = clock() }

    override fun shouldShow(placement: AdPlacement): Boolean = when (placement) {
        AdPlacement.HOME_BANNER, AdPlacement.REWARDED_BONUS -> true
        AdPlacement.SESSION_END_INTERSTITIAL -> {
            val last = lastInterstitialAt
            completedSessions > 0 &&
                completedSessions % sessionsPerInterstitial == 0 &&
                (last == null || clock() - last >= minIntervalMs)
        }
    }
}
