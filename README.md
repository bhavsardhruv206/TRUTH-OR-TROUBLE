# Truth or Trouble (formerly AfterHours)

Offline, pass-the-phone 18+ party game for Android. Spin the Wheel (picks the player, then Truth or Dare), plus Truth, Dare, Would You Rather, Couple, Never Have I Ever, Most Likely To and Hot Seat modes; 2–8 players; Chill/Bold/Wild intensity; skip with no penalty; session scoreboard.

- `core/` pure Kotlin (JVM) game engine, models, card loader and cards. Unit tested.
  - `cards_en.json`: 300 English cards. `cards_hinglish.json`: 96 Hinglish cards (language `hi-Latn`).
  - Cards can address players: `{name}` is the current player, `{other}` a different random player (`CardText`).
  - `timerSeconds` on a card makes it a timed dare. `ChaosEvents` / `ChaosScheduler` drop a group moment every 4–6 turns.
  - `SeenCardMemory` remembers played cards between games so regulars see fresh cards first.
- `app/` Android app: Jetpack Compose + Material 3, MVVM (`GameViewModel`), DataStore for settings, saved names, played and reported cards. Included in the build only when an Android SDK is found (`ANDROID_HOME` or `local.properties`).

Run engine tests: `gradle :core:test`. Build the app: open in Android Studio, or `gradle :app:assembleDebug` with the SDK installed.

## Before release

- **Ad IDs:** AdMob uses Google's **sample/test** app and unit IDs (`ads/AdIds.kt`, manifest meta-data). Replace them with your own.
- **Consent:** Google UMP (`ads/ConsentManager.kt`) runs before ads start. Create a GDPR message in AdMob > Privacy & messaging, or the form will not show.
- **Card reports:** reported cards are hidden on the device. Set `AppConfig.SUPPORT_EMAIL` to also receive reports by email.
- **Remove ads:** one-time Play Billing product `remove_ads` (`billing/BillingManager.kt`). Create a managed in-app product with that id in Play Console and test from an internal testing track.

Ads: home/mode/setup banner; one capped interstitial (every 2nd finished game, min 2 min apart, `AdPolicy` in core) as the game ends; optional rewarded ad on the results screen for a +5 round bonus game (free after "Remove ads"). No ads on the card screen.
