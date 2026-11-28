# Truth or Trouble (formerly AfterHours)

Offline, pass-the-phone 18+ party game for Android. Spin the Wheel (picks the player, then Truth or Dare), plus Truth, Dare, Would You Rather, Couple, Never Have I Ever, Most Likely To and Hot Seat modes; 2–8 players; plus Closer, a romantic, non-explicit game for exactly two consenting partners, with a pink-and-blue pose illustration on every card; Chill/Bold/Wild intensity; scoring with streak bonuses, a one-point skip penalty (never in Closer or when reporting a card), double-point final rounds and a 30-second turn clock on answer cards (can be switched off); session scoreboard.

Games are grouped by mood on the Games screen (`GameId` / `Mood` in `core/.../Catalog.kt`): Party (Spin the Wheel, Dare, Charades, Hot Seat, Most Likely To), Fun (This or That, Would You Rather, Never Have I Ever), Deep talk (Truth, How Well Do You Know Me, Love Language Quiz), Romance & couples (Couple, Couple Bingo, Closer) and 18+ Spicy (Wild-cards-only Truth or Dare, Never Have I Ever and Couple). Home shows a "Suggested tonight" row (`Tonight`) that starts a game in one tap with placeholder names. This or That and Would You Rather cards show "X% usually pick this" after a pick (`pickA` on the card: bundled estimates, since the app is offline). The love-language quiz ends in a result card shared as an image through the Android share sheet.

**18+ content:** 18+ Spicy games and Wild cards are labelled 18+, ask everyone playing to confirm they are adults (once per app run), and never show a banner, interstitial or rewarded-ad offer next to them.

- `core/` pure Kotlin (JVM) game engine, models, card loader and cards. Unit tested.
  - Card packs, one per `CardLanguage`: `cards_en.json` (English), `cards_hi.json` (Hindi), `cards_mr.json` (Marathi), `cards_gu.json` (Gujarati), `cards_es.json` (Spanish).
  - App text lives in `core/.../i18n`: one `UiStrings` language pack per language. Its constructor has no defaults, so a pack that misses a string does not compile. The language is picked on first launch and can be changed in Settings.
  - Cards can address players: `{name}` is the current player, `{other}` a different random player (`CardText`).
  - `timerSeconds` on a card makes it a timed dare. `ChaosEvents` / `ChaosScheduler` drop a group moment every 4–6 turns.
  - `SeenCardMemory` remembers played cards between games so regulars see fresh cards first.
- `app/` Android app: Jetpack Compose + Material 3, MVVM (`GameViewModel`), DataStore for settings, language, saved names, played and reported cards. Included in the build only when an Android SDK is found (`ANDROID_HOME` or `local.properties`).

Run engine tests: `gradle :core:test`. Build the app: open in Android Studio, or `gradle :app:assembleDebug` with the SDK installed.

## Before release

- **Ad IDs:** AdMob uses Google's **sample/test** app and unit IDs (`ads/AdIds.kt`, manifest meta-data). Replace them with your own.
- **Consent:** Google UMP (`ads/ConsentManager.kt`) runs before ads start. Create a GDPR message in AdMob > Privacy & messaging, or the form will not show.
- **Card reports:** reported cards are hidden on the device. Set `AppConfig.SUPPORT_EMAIL` to also receive reports by email.
- **Remove ads:** one-time Play Billing product `remove_ads` (`billing/BillingManager.kt`). Create a managed in-app product with that id in Play Console and test from an internal testing track.

Ads: home/mode/setup banner; one capped interstitial (every 2nd finished game, min 2 min apart, `AdPolicy` in core) as the game ends; optional rewarded ad on the results screen for a +5 round bonus game (free after "Remove ads"). No ads on the card screen.
