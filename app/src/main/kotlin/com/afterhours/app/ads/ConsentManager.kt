package com.afterhours.app.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google User Messaging Platform (UMP) consent. Shows Google's consent form where the law
 * requires it (for example the EEA and UK) before any ad is requested. The form's wording
 * and regions are configured in the AdMob console under Privacy & messaging.
 */
class ConsentManager(activity: Activity) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when Settings must offer a "Privacy choices" entry. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    val canRequestAds: Boolean get() = info.canRequestAds()

    /** Updates consent and shows the form if needed; [onDone] runs once either way. */
    fun gather(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false) // adults-only app behind an 18+ gate
            .build()
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    refreshPrivacyStatus()
                    onDone()
                }
            },
            { _ ->
                refreshPrivacyStatus()
                onDone()
            },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> refreshPrivacyStatus() }
    }

    private fun refreshPrivacyStatus() {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
