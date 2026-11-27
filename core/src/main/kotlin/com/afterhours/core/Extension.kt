package com.afterhours.core

/**
 * Seam for a feature intentionally NOT implemented yet: ads (AdMob, later).
 * The UI depends only on this interface so AdMob can be plugged in without
 * touching game logic. Ads must never appear mid-card.
 */
enum class AdPlacement { HOME_BANNER, SESSION_END_INTERSTITIAL }

interface AdsProvider {
    fun shouldShow(placement: AdPlacement): Boolean
}

/** Default: no ads. */
object NoAds : AdsProvider {
    override fun shouldShow(placement: AdPlacement) = false
}
