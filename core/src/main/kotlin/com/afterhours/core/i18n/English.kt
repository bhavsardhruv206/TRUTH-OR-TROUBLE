package com.afterhours.core.i18n

import com.afterhours.core.GameMode
import com.afterhours.core.Intensity
import com.afterhours.core.PlayerRules

private fun enPoints(n: Int) = if (n == 1) "1 Point" else "$n Points"

val EnglishStrings = UiStrings(
    back = "Back",
    next = "Next",
    skip = "Skip",
    done = "Done",
    close = "Close",
    cancel = "Cancel",
    continueLabel = "Continue",
    languageTitle = "Choose your language",
    languageBody = "Menus and cards will be in this language. You can change it any time in Settings.",
    language = "Language",
    adultsOnly = "Adults only · 18+",
    ageGateBody = "Cheeky questions and dares for grown-ups. Skip anything, any time, with no penalty.",
    ageConfirm = "I am 18 years or older.",
    enter = "Enter",
    tutorial = listOf(
        TutorialStep("Pass the phone", "Add 2 to 8 players. On each turn the phone goes to the person named at the top. Cards often call players by name."),
        TutorialStep("Pick your game", "Open Games and tap any game, then choose its difficulty.\nSpin the Wheel: the wheel picks who plays, then Truth or Dare.\nTruth, Dare, Would You Rather, Couple.\nNever Have I Ever, Most Likely To and Hot Seat for the whole group."),
        TutorialStep("Set the heat", "Chill is light. Bold adds cheekier cards. Wild includes everything. Bolder cards score more: 1, 2 or 3 points."),
        TutorialStep("Skipping is free", "Not comfortable? Skip. No points lost, no questions asked. You can also report a card and it never shows again."),
        TutorialStep("Expect chaos", "Every few turns a group moment interrupts the game. Some dares come with a timer. Switch chaos off in Settings if you prefer."),
    ),
    startPlaying = "Start playing",
    homeTagline = { n -> "$n games for 2 to 8 adults.\nPlus Closer, just for couples." },
    games = "Games",
    spinTheWheel = "Spin the wheel",
    howToPlay = "How to play",
    settings = "Settings",
    helpSafety = "Help & safety",
    helpBody = "• Skip any card, any time, with no penalty.\n" +
        "• Never share private info, contact strangers, share intimate media, drink anything you don't want to, or do anything unsafe.\n" +
        "• Closer is for two consenting adult partners. Ask before any touch or kiss, and either of you can stop at any time.\n" +
        "• Use Report on a card to hide it for good.\n" +
        "• Scores are just for fun and have no value.",
    sound = "Sound",
    vibration = "Vibration",
    chaosMoments = "Chaos moments",
    chaosHint = "Group events every few turns",
    forgetPlayed = "Forget played cards",
    playedForgotten = "Played cards forgotten",
    privacyChoices = "Privacy choices",
    adsRemoved = "Ads removed. Thank you.",
    removeAds = { price -> "Remove ads · $price" },
    removeAdsUnavailable = "Remove ads (unavailable)",
    restorePurchase = "Restore purchase",
    gamesCount = { n -> "$n games · pick one to play" },
    groupFeatured = "Featured",
    groupClassic = "Classic",
    groupWholeGroup = "Whole group",
    groupCouples = "For couples",
    newBadge = "New",
    game = { m ->
        when (m) {
            null -> GameText("The wheel decides", "Spin to pick who plays, then spin again for Truth or Dare. Everyone gets an even number of turns.")
            GameMode.TRUTH -> GameText("Answer honestly", "Read the question out loud and answer it truthfully. Skip if you'd rather not.")
            GameMode.DARE -> GameText("Do what the card says", "Do the dare in front of the group. Some dares come with a timer.")
            GameMode.WOULD_YOU_RATHER -> GameText("Choose, then defend it", "Pick one of the two options and convince the group you chose right.")
            GameMode.COUPLE -> GameText("For partners", "Questions and dares made for two people who know each other well.")
            GameMode.NEVER_HAVE_I_EVER -> GameText("Hands up if you have", "Read the line out. Everyone who has done it raises a hand and the group can ask for the story.")
            GameMode.MOST_LIKELY_TO -> GameText("Point on three", "Read the question, count to three and everyone points at who fits best. Most votes explains.")
            GameMode.HOT_SEAT -> GameText("60 seconds of questions", "The named player sits in the hot seat while the group asks rapid-fire questions until the timer ends.")
            GameMode.CLOSER -> GameText("Just the two of you", "A romantic game for two partners. Each card shows the pose and tells you what to do. Ask before any touch, and skip anything at all.")
            GameMode.KNOW_ME -> GameText("Guess their answer", "The named player secretly thinks of their answer about themselves. Everyone else guesses, then they reveal. Every correct guess scores the card's points.")
        }
    },
    couplesGateTitle = "For consenting couples",
    couplesGateBody = "Closer is for two adult partners who both want to play.\n\n" +
        "• Ask before any touch or kiss.\n" +
        "• Either of you can skip a card or stop at any time, no reason needed.\n" +
        "• Nothing here involves undressing. Keep it sweet and romantic.",
    couplesAgree = "We're both in",
    notNow = "Not now",
    difficulty = "Difficulty",
    cardLanguage = "Card language",
    intensityDescription = {
        when (it) {
            Intensity.CHILL -> "Light and easy"
            Intensity.BOLD -> "Cheekier, more personal"
            Intensity.WILD -> "Everything, including the boldest"
        }
    },
    points = { n -> enPoints(n) },
    nextPlayers = "Next: players",
    whosPlaying = "Who's playing?",
    couplesHint = "Just the two of you · enter both names",
    playersHint = { n -> "$n players · names are saved for next time" },
    partnerN = { n -> "Partner $n" },
    playerN = { n -> "Player $n" },
    remove = "Remove",
    addPlayer = "Add player",
    rounds = "Rounds",
    startGame = "Start game",
    problem = {
        when (it) {
            PlayerRules.Problem.COUPLE_COUNT -> "Closer is for exactly ${PlayerRules.COUPLE_PLAYERS} players: you and your partner."
            PlayerRules.Problem.TOO_FEW -> "Add at least ${PlayerRules.MIN_PLAYERS} players."
            PlayerRules.Problem.TOO_MANY -> "At most ${PlayerRules.MAX_PLAYERS} players can play."
            PlayerRules.Problem.EMPTY_NAME -> "Every player needs a nickname."
            PlayerRules.Problem.NAME_TOO_LONG -> "Nicknames can be up to ${PlayerRules.MAX_NAME_LENGTH} characters."
            PlayerRules.Problem.DUPLICATE_NAME -> "Nicknames must be different."
        }
    },
    noCardsLeft = "No cards left for this mode and intensity.",
    roundOf = { r, t -> "Round $r of $t" },
    endGame = "End game",
    yourTurn = "your turn",
    whatToDo = "What to do",
    reportCard = "Report card",
    donePoints = { n -> "Done  +${enPoints(n)}" },
    endGameTitle = "End this game?",
    scoresLost = "Scores will be lost.",
    keepPlaying = "Keep playing",
    reportTitle = "Report this card",
    reportBody = "It will be skipped and never shown on this phone again.",
    reportReasons = listOf("Offensive or hurtful", "Unsafe to do", "Too explicit", "Doesn't make sense"),
    whosNext = "Who's next?",
    truthOrDare = { name -> "$name, truth or dare?" },
    spinToPick = "Spin to pick a player",
    spinToFind = "Spin to find out",
    spin = "Spin",
    spinning = "Spinning…",
    nextTruthOrDare = "Next: truth or dare",
    showMyCard = "Show my card",
    time = "Time",
    startTimer = "Start timer",
    pause = "Pause",
    chaosMoment = "Chaos moment",
    // English chaos text comes straight from ChaosEvents.
    chaos = emptyMap(),
    gameOver = "Game over",
    nobodyScored = "Nobody scored",
    everyoneSafe = "Everyone played it safe.",
    sharedWin = "Shared the win",
    takesNight = "takes the night",
    skipped = { n -> "$n skipped" },
    playAgain = "Play again",
    home = "Home",
    bonusGame = "Bonus game, +5 rounds",
    watchAdBonus = "Watch an ad for a bonus game, +5 rounds",
    playGame = "Play game",
    howManyPlayers = "How many players?",
    passPhone = { a, b -> "Pass one phone around. $a to $b people can play." },
    playersWord = "players",
    countAllOpen = "Every game is open, including Closer for couples.",
    countNoCloser = "Every game except Closer, which is just for 2.",
    nextChooseGame = "Next: choose a game",
    pickAGame = { n -> "$n players · pick a game" },
    twoPlayersOnly = "2 players only",
    avatarHint = { n -> "$n players · tap a picture to change it" },
    profilePicture = "Profile picture",
    theme = "Theme",
    themeHint = "Auto follows your phone's light or dark setting.",
    themeAuto = "Auto",
    themeLight = "Light",
    themeDark = "Dark",
    turnClock = "Turn clock",
    turnClockHint = { s -> "$s seconds to answer, or it counts as a skip" },
    streakLine = { s, b -> "$s in a row · next one +$b bonus" },
    showdown = "Showdown · double points",
    longestStreak = { name, n -> "Longest streak: $name, $n in a row" },
    skipCost = { n -> "Skip  −${enPoints(n)}" },
    keepItSecret = "think of your answer, keep it secret",
    whoGotIt = "Say your answer out loud. Who got it right?",
    guessThenReveal = { name -> "Everyone else, take turns guessing out loud. Then $name reveals." },
    revealAnswer = "Reveal answer",
    nobodyGotIt = "Nobody got it",
    scoreEach = { n -> "Score  +${enPoints(n)} each" },
    mode = { it.label },
    intensity = { it.label },
    // English pose text comes straight from closer_poses.json.
    poses = emptyMap(),
)
