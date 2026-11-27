# AfterHours (working title)

Offline, pass-the-phone 18+ party game for Android. Truth, Dare, Would You Rather and Couple modes; 2–8 players; Chill/Bold/Wild intensity; skip with no penalty; session scoreboard.

- `core/` pure Kotlin (JVM) game engine, models, card loader, and the seed cards (`core/src/main/resources/cards_en.json`, 62 original cards). Unit tested.
- `app/` Android app: Jetpack Compose + Material 3, MVVM (`GameViewModel`), DataStore for the age-gate flag. Included in the build only when an Android SDK is found (`ANDROID_HOME` or `local.properties`).

Run engine tests: `gradle :core:test`. Build the app: open in Android Studio, or `gradle :app:assembleDebug` with the SDK installed (add a Gradle wrapper via `gradle wrapper`).

Ads: AdMob with Google's **sample/test** app and ad unit IDs only (`app/.../ads/AdIds.kt`, manifest meta-data). Home/mode/setup banner; one capped interstitial (every 2nd finished game, min 2 min apart, `AdPolicy` in core) as the game ends; optional rewarded ad on the results screen for a +5 round bonus game. No ads on the card screen. Swap in real IDs and add a consent flow before release.

Not included yet: online play (removed from scope), Room, help/report form.
