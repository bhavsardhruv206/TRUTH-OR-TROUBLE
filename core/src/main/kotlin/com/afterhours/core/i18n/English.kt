package com.afterhours.core.i18n

import com.afterhours.core.GameMode
import com.afterhours.core.Intensity
import com.afterhours.core.LoveLanguage
import com.afterhours.core.LoveQuiz
import com.afterhours.core.LoveQuizText
import com.afterhours.core.Mood
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
        TutorialStep("Start in one tap", "Tap a suggested game on Home to start right away, and add names later. Or tap Play game, choose how many are playing, from 2 to 8, and add names and pictures. The phone goes to the person named at the top."),
        TutorialStep("Pick a mood", "Games are grouped by mood: Party, Fun, Deep talk and Romance & couples. 18+ Spicy games are clearly marked and ask everyone to confirm they are adults. Couples games open when 2 are playing."),
        TutorialStep("Set the heat", "Chill is light. Bold adds cheekier cards. Wild includes everything and is 18+. Bolder cards score more: 1, 2 or 3 points."),
        TutorialStep("Skip if you want", "Not comfortable? Skip, no questions asked. In party games a skip costs one point; in couples games skipping is always free. Reporting a card never costs anything, and it never shows again."),
        TutorialStep("Raise the stakes", "Complete cards in a row for streak bonuses. Skips cost a point, and the final rounds score double. Answers are on the clock, and chaos moments interrupt every few turns. Switch the clock or chaos off in Settings."),
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
            GameMode.THIS_OR_THAT -> GameText("Quick picks", "Two options, no thinking. Tap your pick to see how other players usually choose, then defend it.")
            GameMode.CHARADES -> GameText("Act it out", "Only the named player reads the card, then acts it out without words. The group shouts guesses before the timer ends.")
            GameMode.COUPLE_BINGO -> GameText("How much have you two done?", "A board of things couples do. Tap every square that is true for you two and race for three in a line.")
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
    soundHint = "Beeps on new cards and timers",
    readAloud = "Read cards aloud",
    readAloudHint = "Or tap the speaker on any card",
    stopReading = "Stop reading",
    readCardAloud = "Read card aloud",
    moodTitle = {
        when (it) {
            Mood.PARTY -> "Party"
            Mood.FUN -> "Fun"
            Mood.DEEP -> "Deep talk"
            Mood.COUPLES -> "Romance & couples"
            Mood.SPICY -> "18+ Spicy"
        }
    },
    moodBlurb = {
        when (it) {
            Mood.PARTY -> "Loud, quick and everyone in"
            Mood.FUN -> "Easy choices and big laughs"
            Mood.DEEP -> "Get to know each other"
            Mood.COUPLES -> "Just the two of you"
            Mood.SPICY -> "Adults only · Wild cards"
        }
    },
    adultBadge = "18+",
    wildAdultNote = "18+ · for adults only",
    spicyGateTitle = "18+ Spicy",
    spicyGateBody = "These games use only the boldest cards and are for adults.\n\n• Everyone playing must be 18 or older.\n• Skip any card at any time, no reason needed.\n• Never pressure anyone, and stop whenever someone wants to.",
    spicyAgree = "Everyone here is 18+",
    spicySpin = "Spicy Truth or Dare",
    spicySpinGame = GameText("The wheel, turned up", "Spin to pick who plays, then Truth or Dare, using only Wild cards. Skip anything you like."),
    spicyNever = "Spicy Never Have I Ever",
    spicyNeverGame = GameText("Bold confessions", "Read the line out. Everyone who has done it raises a hand. Wild cards only, so expect cheeky stories."),
    spicyCouple = "Spicy Couple",
    spicyCoupleGame = GameText("Turn up the heat", "Flirty questions and dares for partners, Wild cards only. Ask first, and skip anything at all."),
    suggestedTonight = "Suggested tonight",
    oneTapHint = "Tap one to start now. Names can wait.",
    newHere = "New here? See how to play",
    votePrompt = "Tap your pick",
    voteFirst = "First one",
    voteSecond = "Second one",
    usuallyPick = { n -> "$n% usually pick this" },
    onlyYouLook = { name -> "Only $name looks at the screen" },
    bingoHint = "Tap every square that is true for you two. Three in a line is a bingo.",
    bingoCount = { n -> if (n == 1) "Bingo!" else "$n bingos!" },
    bingoNone = "No line yet",
    bingoFull = "Full house! You two have done it all.",
    newBoard = "New board",
    loveQuiz = "Love Language Quiz",
    loveQuizGame = GameText("Find how you feel loved", "Ten quick choices, one person at a time. You get your love language on a card you can share."),
    quizIntro = "Ten quick choices. Each time, pick the moment that would make you feel more loved. There are no wrong answers.",
    quizStart = "Start quiz",
    quizQuestion = { n, t -> "Question $n of $t" },
    quizPrompt = "Which would make you feel more loved?",
    quizResultEyebrow = "Your love language",
    quizTie = { a, b -> "$a and $b" },
    quizMix = "Your mix",
    shareResult = "Share result",
    nextPerson = "Next person",
    quizShareText = { r, p -> "My love language is $r ($p%). Find yours in Truth or Trouble." },
    loveQuizText = LoveQuizText(
        questions = LoveQuiz.questions(listOf(
            "My partner tells me they are proud of me" to "We spend an evening together, phones away",
            "They bring me a little surprise that made them think of me" to "They take care of a chore I have been dreading",
            "A big hug the moment I walk in" to "A sweet note left where I will find it",
            "A long walk where we really talk" to "A thoughtful present for no reason at all",
            "Breakfast made for me without asking" to "Holding hands while we are out",
            "A compliment in front of our friends" to "They remember something I wanted and get it",
            "A whole day planned just for us" to "Help finishing something stressful",
            "A souvenir they picked up on a trip" to "Cuddling up together on the sofa",
            "They fix something that has been bugging me" to "A message saying they are thinking of me",
            "A long hug after a hard day" to "Their full attention while I talk",
        )),
        names = mapOf(LoveLanguage.WORDS to "Words of affirmation", LoveLanguage.TIME to "Quality time", LoveLanguage.GIFTS to "Thoughtful gifts", LoveLanguage.ACTS to "Acts of service", LoveLanguage.TOUCH to "Physical touch"),
        descriptions = mapOf(
            LoveLanguage.WORDS to "Kind words, praise and \"I love you\" said out loud mean the most to you.",
            LoveLanguage.TIME to "Undivided attention and time together, just the two of you, fill your cup.",
            LoveLanguage.GIFTS to "A small, thoughtful gift tells you someone was thinking of you.",
            LoveLanguage.ACTS to "Help that lightens your load says \"I care\" louder than words.",
            LoveLanguage.TOUCH to "Hugs, hand-holding and closeness make you feel safe and loved.",
        ),
    ),
    mode = { it.label },
    intensity = { it.label },
    // English pose text comes straight from closer_poses.json.
    poses = emptyMap(),
)
