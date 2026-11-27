# Truth or Trouble (formerly AfterHours)

Offline, pass-the-phone 18+ party game for Android. Truth, Dare, Would You Rather and Couple modes; 2–8 players; Chill/Bold/Wild intensity; skip with no penalty; session scoreboard.

- `core/` pure Kotlin (JVM) game engine, models, card loader, and the seed cards (`core/src/main/resources/cards_en.json`, 62 original cards). Unit tested.
- `app/` Android app: Jetpack Compose + Material 3, MVVM (`GameViewModel`), DataStore for the age-gate flag. Included in the build only when an Android SDK is found (`ANDROID_HOME` or `local.properties`).

Run engine tests: `gradle :core:test`. Build the app: open in Android Studio, or `gradle :app:assembleDebug` with the SDK installed (add a Gradle wrapper via `gradle wrapper`).

Not included yet: online play (removed from scope), ads (`AdsProvider` seam in core; AdMob later), Room, help/report form.
