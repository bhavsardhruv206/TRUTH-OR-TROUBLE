package com.afterhours.app

/** Values the owner must fill in before release. */
object AppConfig {
    /**
     * Address that receives card reports. Leave blank to only hide reported cards on the device.
     * Set it to a real support inbox before publishing.
     */
    const val SUPPORT_EMAIL = ""

    /** One-time in-app product id; create it in Play Console > Monetize > In-app products. */
    const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
}
