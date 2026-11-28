package com.afterhours.core.i18n

import com.afterhours.core.GameMode
import com.afterhours.core.Intensity
import com.afterhours.core.PlayerRules

/** Hindi in Latin letters. Game names stay in English, the way people say them. */
val HinglishStrings = UiStrings(
    back = "Wapas",
    next = "Aage",
    skip = "Skip",
    done = "Ho gaya",
    close = "Band karo",
    cancel = "Cancel",
    continueLabel = "Aage chalo",
    languageTitle = "Apni bhasha chuno",
    languageBody = "Menu aur cards isi bhasha mein honge. Settings mein kabhi bhi badal sakte ho.",
    language = "Bhasha",
    adultsOnly = "Sirf adults ke liye · 18+",
    ageGateBody = "Badon ke liye naughty sawaal aur dares. Kuch bhi, kabhi bhi skip karo, koi penalty nahi.",
    ageConfirm = "Meri umar 18 saal ya usse zyada hai.",
    enter = "Andar chalo",
    tutorial = listOf(
        TutorialStep("Phone pass karo", "2 se 8 players add karo. Har turn pe phone us insaan ke paas jaata hai jiska naam upar likha hai. Cards aksar players ko naam se bulaate hain."),
        TutorialStep("Apna game chuno", "Games kholo aur koi bhi game tap karo, phir uski difficulty chuno.\nSpin the Wheel: wheel decide karta hai kaun khelega, phir Truth ya Dare.\nTruth, Dare, Would You Rather, Couple.\nPoore group ke liye Never Have I Ever, Most Likely To aur Hot Seat."),
        TutorialStep("Heat set karo", "Chill halka-phulka hai. Bold mein zyada naughty cards hain. Wild mein sab kuch hai. Bold cards pe zyada points: 1, 2 ya 3."),
        TutorialStep("Skip karna free hai", "Comfortable nahi ho? Skip karo. Koi point nahi katega, koi sawaal nahi. Card report bhi kar sakte ho, phir woh kabhi nahi dikhega."),
        TutorialStep("Chaos ke liye ready raho", "Har kuch turns mein ek group moment game ko beech mein rokta hai. Kuch dares mein timer hota hai. Chaho to Settings mein chaos band kar do."),
    ),
    startPlaying = "Khelna shuru karo",
    homeTagline = { n -> "2 se 8 adults ke liye $n games.\nSaath mein Closer, sirf couples ke liye." },
    games = "Games",
    spinTheWheel = "Wheel ghumao",
    howToPlay = "Kaise khelein",
    settings = "Settings",
    helpSafety = "Help aur safety",
    helpBody = "• Koi bhi card, kabhi bhi skip karo, koi penalty nahi.\n" +
        "• Kabhi private info share mat karo, strangers se contact mat karo, intimate photos-videos share mat karo, jo na chaho woh mat piyo, aur kuch bhi unsafe mat karo.\n" +
        "• Closer do raazi adult partners ke liye hai. Kisi bhi touch ya kiss se pehle poocho, aur tum mein se koi bhi kabhi bhi ruk sakta hai.\n" +
        "• Card ko hamesha ke liye chhupane ke liye Report dabao.\n" +
        "• Scores sirf mazze ke liye hain, inki koi value nahi.",
    sound = "Sound",
    vibration = "Vibration",
    chaosMoments = "Chaos moments",
    chaosHint = "Har kuch turns mein group events",
    forgetPlayed = "Khele hue cards bhool jao",
    playedForgotten = "Khele hue cards bhula diye",
    privacyChoices = "Privacy choices",
    adsRemoved = "Ads hat gaye. Shukriya.",
    removeAds = { price -> "Ads hatao · $price" },
    removeAdsUnavailable = "Ads hatao (abhi available nahi)",
    restorePurchase = "Purchase wapas lao",
    gamesCount = { n -> "$n games · khelne ke liye ek chuno" },
    groupFeatured = "Featured",
    groupClassic = "Classic",
    groupWholeGroup = "Poora group",
    groupCouples = "Couples ke liye",
    newBadge = "Naya",
    game = { m ->
        when (m) {
            null -> GameText("Wheel decide karega", "Ghuma ke chuno kaun khelega, phir dobara ghumao Truth ya Dare ke liye. Sabko barabar turns milte hain.")
            GameMode.TRUTH -> GameText("Imaandari se jawaab do", "Sawaal zor se padho aur sach sach jawaab do. Na chaho to skip karo.")
            GameMode.DARE -> GameText("Jo card bole woh karo", "Group ke saamne dare karo. Kuch dares mein timer hota hai.")
            GameMode.WOULD_YOU_RATHER -> GameText("Chuno, phir defend karo", "Do mein se ek option chuno aur group ko convince karo ki sahi chuna.")
            GameMode.COUPLE -> GameText("Partners ke liye", "Un do logon ke liye sawaal aur dares jo ek dusre ko achhe se jaante hain.")
            GameMode.NEVER_HAVE_I_EVER -> GameText("Kiya hai to haath upar", "Line padho. Jisne bhi kiya hai woh haath uthaye, aur group poori kahani pooch sakta hai.")
            GameMode.MOST_LIKELY_TO -> GameText("Teen pe point karo", "Sawaal padho, teen tak gino aur sab us pe point karo jo sabse fit baithta hai. Sabse zyada votes wala explain kare.")
            GameMode.HOT_SEAT -> GameText("60 second ke sawaal", "Jiska naam aaya woh hot seat pe baithta hai aur group timer khatam hone tak fatafat sawaal poochta hai.")
            GameMode.CLOSER -> GameText("Sirf tum dono", "Do partners ke liye ek romantic game. Har card pose dikhata hai aur batata hai kya karna hai. Kisi bhi touch se pehle poocho, aur kuch bhi skip karo.")
        }
    },
    couplesGateTitle = "Raazi couples ke liye",
    couplesGateBody = "Closer un do adult partners ke liye hai jo dono khelna chahte hain.\n\n" +
        "• Kisi bhi touch ya kiss se pehle poocho.\n" +
        "• Tum mein se koi bhi kabhi bhi card skip kar sakta hai ya ruk sakta hai, koi wajah nahi chahiye.\n" +
        "• Isme kapde utaarne wala kuch nahi hai. Ise sweet aur romantic rakho.",
    couplesAgree = "Hum dono ready hain",
    notNow = "Abhi nahi",
    difficulty = "Difficulty",
    cardLanguage = "Card ki bhasha",
    intensityDescription = {
        when (it) {
            Intensity.CHILL -> "Halka aur easy"
            Intensity.BOLD -> "Zyada naughty, zyada personal"
            Intensity.WILD -> "Sab kuch, sabse bold bhi"
        }
    },
    points = { n -> "$n pt" },
    nextPlayers = "Aage: players",
    whosPlaying = "Kaun khel raha hai?",
    couplesHint = "Sirf tum dono · dono ke naam likho",
    playersHint = { n -> "$n players · naam agli baar ke liye save rahenge" },
    partnerN = { n -> "Partner $n" },
    playerN = { n -> "Player $n" },
    remove = "Hatao",
    addPlayer = "Player add karo",
    rounds = "Rounds",
    startGame = "Game shuru karo",
    problem = {
        when (it) {
            PlayerRules.Problem.COUPLE_COUNT -> "Closer theek ${PlayerRules.COUPLE_PLAYERS} players ke liye hai: tum aur tumhara partner."
            PlayerRules.Problem.TOO_FEW -> "Kam se kam ${PlayerRules.MIN_PLAYERS} players add karo."
            PlayerRules.Problem.TOO_MANY -> "Zyada se zyada ${PlayerRules.MAX_PLAYERS} players khel sakte hain."
            PlayerRules.Problem.EMPTY_NAME -> "Har player ka ek nickname hona chahiye."
            PlayerRules.Problem.NAME_TOO_LONG -> "Nickname zyada se zyada ${PlayerRules.MAX_NAME_LENGTH} letters ka ho sakta hai."
            PlayerRules.Problem.DUPLICATE_NAME -> "Sabke nickname alag hone chahiye."
        }
    },
    noCardsLeft = "Is game aur difficulty ke liye koi card nahi bacha.",
    roundOf = { r, t -> "Round $r / $t" },
    endGame = "Game khatam karo",
    yourTurn = "tumhari baari",
    whatToDo = "Kya karna hai",
    reportCard = "Card report karo",
    donePoints = { n -> "Ho gaya  +$n" },
    endGameTitle = "Ye game khatam karein?",
    scoresLost = "Scores mit jaayenge.",
    keepPlaying = "Khelte raho",
    reportTitle = "Is card ko report karo",
    reportBody = "Ye skip ho jaayega aur is phone pe phir kabhi nahi dikhega.",
    reportReasons = listOf("Offensive ya dil dukhane wala", "Karna unsafe hai", "Bahut zyada explicit", "Samajh nahi aata"),
    whosNext = "Agla kaun?",
    truthOrDare = { name -> "$name, truth ya dare?" },
    spinToPick = "Player chunne ke liye ghumao",
    spinToFind = "Jaanne ke liye ghumao",
    spin = "Ghumao",
    spinning = "Ghoom raha hai…",
    nextTruthOrDare = "Aage: truth ya dare",
    showMyCard = "Mera card dikhao",
    time = "Time khatam",
    startTimer = "Timer shuru karo",
    pause = "Roko",
    chaosMoment = "Chaos moment",
    chaos = mapOf(
        "swap-seats" to ChaosText("Seat badlo", "Sab apne saamne wale se seat badlo. Jo sabse last mein baithega woh agla card zor se padhega."),
        "point-vote" to ChaosText("Point karo aur vote do", "Teen pe, sab us pe point karo jo apni hi shaadi mein sabse late aayega. Jis pe sabse zyada point hon woh jhuk ke salaam kare."),
        "silent-round" to ChaosText("Silent round", "Timer khatam hone tak koi nahi bolega. Jo bola use group ko ek chhota sa harmless raaz batana padega."),
        "fist-five" to ChaosText("Mutthi se paanch", "Teen pe apne din ko zero se paanch ungliyon mein rate karo. Sabse kam rating wala ek line mein wajah bataye."),
        "compliment-chain" to ChaosText("Compliment chain", "Left wale player se shuru karke, har koi agle ko ek sachchi tareef de."),
        "statue" to ChaosText("Statue", "Sab jis pose mein ho wahin jam jao. Jo pehle hila woh agla card dramatic awaaz mein padhega."),
        "thumbs" to ChaosText("Thumb war", "Group do players chune jo thumb war karein. Jeetne wala decide kare agla kaun khelega."),
        "rhyme-chain" to ChaosText("Tukbandi chain", "Gol ghoomo: har koi aisa shabd bole jo pichhle se rhyme kare. Jo atke ya repeat kare woh haara."),
        "never-have" to ChaosText("Never have I ever", "Ek harmless 'never have I ever' bolo. Jisne bhi kiya hai woh haath uthaye."),
        "duel" to ChaosText("Fatafat duel", "Do players peeth se peeth laga ke khade hon aur teen pe mudein. Jo pehle funny pose banaye woh jeeta."),
        "story" to ChaosText("Ek shabd ki kahani", "Milke ek kahani banao, har koi ek shabd. Jo kahani khatam kare woh agla dare dene wala chune."),
        "hands-up" to ChaosText("Haath upar", "Sab dono haath upar rakho. Jab koi aisi baat bole jo tumpe bhi laagu ho to ek haath neeche karo. Topic group decide karega."),
    ),
    gameOver = "Game khatam",
    nobodyScored = "Kisi ne score nahi kiya",
    everyoneSafe = "Sabne safe khela.",
    sharedWin = "Jeet baant li",
    takesNight = "aaj raat ka winner",
    skipped = { n -> "$n skip kiye" },
    playAgain = "Phir se khelo",
    home = "Home",
    bonusGame = "Bonus game, +5 rounds",
    watchAdBonus = "Bonus game ke liye ad dekho, +5 rounds",
    mode = { it.label },
    intensity = { it.label },
    poses = mapOf(
        "hands" to PoseText("Haath pakdo", "Ek dusre ki taraf mooh karke khade raho ya baitho aur dono haath pakdo."),
        "slow_dance" to PoseText("Slow dance", "Paas khade ho, ek dusre ko baahon mein lo, aur dheere dheere jhoomo."),
        "hug_behind" to PoseText("Peeche se hug", "Ek partner dusre ko peeche se baahon mein bhare."),
        "cuddle" to PoseText("Cuddle", "Saath saath baitho aur ek dusre pe tik jao."),
        "massage" to PoseText("Kandhe ki massage", "Ek baithe, dusra peeche khada hoke kandhon ki massage kare."),
        "kiss_cheek" to PoseText("Gaal pe kiss", "Paas khade ho; ek jhuk ke gaal pe halki si kiss de."),
        "back_to_back" to PoseText("Peeth se peeth", "Peeth se peeth laga ke baitho ya khade raho aur ek dusre pe tiko."),
        "whisper" to PoseText("Kaan mein bolo", "Paas jhuko aur partner ke kaan mein dheere se bolo."),
        "foreheads" to PoseText("Maathe se maatha", "Aamne saamne ho, maathe mile hon, aur haath pakdo."),
        "talk" to PoseText("Aamne saamne", "Ek dusre ki taraf mooh karke, ghutne paas rakh ke baitho aur baat karo."),
        "hand_touch" to PoseText("Halka touch", "Aamne saamne baitho; ek dusre ke haath ko halke se sehlaye ya ungli phere."),
        "lie_down" to PoseText("Saath leto", "Kandhe se kandha mila ke ek dusre ke paas leto."),
        "feed" to PoseText("Taste test", "Ek aankhein band karke baithe; dusra use kuch meetha sa khilaye."),
        "dance_solo" to PoseText("Unke liye naacho", "Ek partner masti mein naache aur dusra dekhe."),
        "hand_kiss" to PoseText("Haath pe kiss", "Ek dusre ka haath pakde aur uske upar kiss kare."),
    ),
)
