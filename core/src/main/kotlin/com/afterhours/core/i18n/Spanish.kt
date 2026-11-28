package com.afterhours.core.i18n

import com.afterhours.core.GameMode
import com.afterhours.core.Intensity
import com.afterhours.core.PlayerRules

private fun esPoints(n: Int) = if (n == 1) "1 punto" else "$n puntos"

val SpanishStrings = UiStrings(
    back = "Atrás",
    next = "Siguiente",
    skip = "Saltar",
    done = "Hecho",
    close = "Cerrar",
    cancel = "Cancelar",
    continueLabel = "Continuar",
    languageTitle = "Elige tu idioma",
    languageBody = "Los menús y las cartas estarán en este idioma. Puedes cambiarlo cuando quieras en Ajustes.",
    language = "Idioma",
    adultsOnly = "Solo adultos · 18+",
    ageGateBody = "Preguntas y retos atrevidos para adultos. Salta lo que quieras, cuando quieras, sin penalización.",
    ageConfirm = "Tengo 18 años o más.",
    enter = "Entrar",
    tutorial = listOf(
        TutorialStep("Pasa el móvil", "Añade de 2 a 8 jugadores. En cada turno el móvil pasa a la persona cuyo nombre aparece arriba. Las cartas suelen llamar a los jugadores por su nombre."),
        TutorialStep("Elige tu juego", "Abre Juegos y toca cualquiera, luego elige su dificultad.\nGira la ruleta: la ruleta elige quién juega y luego Verdad o Reto.\nVerdad, Reto, ¿Qué prefieres?, Pareja.\nYo nunca, ¿Quién es más probable? y Silla caliente para todo el grupo."),
        TutorialStep("Elige el nivel", "Suave es ligero. Atrevido añade cartas más pícaras. Salvaje lo incluye todo. Las cartas más atrevidas dan más puntos: 1, 2 o 3."),
        TutorialStep("Saltar es gratis", "¿No te apetece? Sáltala. No pierdes puntos y nadie pregunta. También puedes denunciar una carta y no volverá a salir."),
        TutorialStep("Prepárate para el caos", "Cada pocos turnos, un momento de grupo interrumpe la partida. Algunos retos llevan temporizador. Si prefieres, desactiva el caos en Ajustes."),
    ),
    startPlaying = "Empezar a jugar",
    homeTagline = { n -> "$n juegos para 2 a 8 adultos.\nAdemás, Closer, solo para parejas." },
    games = "Juegos",
    spinTheWheel = "Gira la ruleta",
    howToPlay = "Cómo se juega",
    settings = "Ajustes",
    helpSafety = "Ayuda y seguridad",
    helpBody = "• Salta cualquier carta, cuando quieras, sin penalización.\n" +
        "• Nunca compartas datos privados, contactes con desconocidos, compartas contenido íntimo, bebas lo que no quieras ni hagas nada peligroso.\n" +
        "• Closer es para dos adultos en pareja que dan su consentimiento. Pregunta antes de cualquier caricia o beso; cualquiera de los dos puede parar cuando quiera.\n" +
        "• Usa Denunciar en una carta para ocultarla para siempre.\n" +
        "• Los puntos son solo por diversión y no tienen ningún valor.",
    sound = "Sonido",
    vibration = "Vibración",
    chaosMoments = "Momentos de caos",
    chaosHint = "Eventos de grupo cada pocos turnos",
    forgetPlayed = "Olvidar cartas jugadas",
    playedForgotten = "Cartas jugadas olvidadas",
    privacyChoices = "Opciones de privacidad",
    adsRemoved = "Anuncios eliminados. ¡Gracias!",
    removeAds = { price -> "Quitar anuncios · $price" },
    removeAdsUnavailable = "Quitar anuncios (no disponible)",
    restorePurchase = "Restaurar compra",
    gamesCount = { n -> "$n juegos · elige uno para jugar" },
    groupFeatured = "Destacado",
    groupClassic = "Clásicos",
    groupWholeGroup = "Todo el grupo",
    groupCouples = "Para parejas",
    newBadge = "Nuevo",
    game = { m ->
        when (m) {
            null -> GameText("La ruleta decide", "Gira para elegir quién juega y vuelve a girar para Verdad o Reto. Todos juegan el mismo número de turnos.")
            GameMode.TRUTH -> GameText("Responde con sinceridad", "Lee la pregunta en voz alta y responde con la verdad. Sáltala si prefieres no hacerlo.")
            GameMode.DARE -> GameText("Haz lo que dice la carta", "Haz el reto delante del grupo. Algunos retos llevan temporizador.")
            GameMode.WOULD_YOU_RATHER -> GameText("Elige y defiéndelo", "Escoge una de las dos opciones y convence al grupo de que acertaste.")
            GameMode.COUPLE -> GameText("Para parejas", "Preguntas y retos para dos personas que se conocen bien.")
            GameMode.NEVER_HAVE_I_EVER -> GameText("Mano arriba si lo has hecho", "Lee la frase. Quien lo haya hecho levanta la mano y el grupo puede pedir la historia.")
            GameMode.MOST_LIKELY_TO -> GameText("Señalad a la de tres", "Lee la pregunta, contad hasta tres y todos señalan a quien encaje mejor. El más votado se explica.")
            GameMode.HOT_SEAT -> GameText("60 segundos de preguntas", "El jugador nombrado se sienta en la silla caliente y el grupo le hace preguntas rápidas hasta que acabe el tiempo.")
            GameMode.CLOSER -> GameText("Solo vosotros dos", "Un juego romántico para dos. Cada carta muestra la postura y dice qué hacer. Pregunta antes de cualquier caricia y salta lo que quieras.")
            GameMode.KNOW_ME -> GameText("Adivina su respuesta", "El jugador nombrado piensa en secreto su respuesta sobre sí mismo. Los demás adivinan y luego la revela. Cada acierto suma los puntos de la carta.")
        }
    },
    couplesGateTitle = "Para parejas que están de acuerdo",
    couplesGateBody = "Closer es para dos adultos en pareja que quieren jugar los dos.\n\n" +
        "• Pregunta antes de cualquier caricia o beso.\n" +
        "• Cualquiera de los dos puede saltar una carta o parar cuando quiera, sin dar explicaciones.\n" +
        "• Aquí nadie se desnuda. Que sea dulce y romántico.",
    couplesAgree = "Los dos queremos",
    notNow = "Ahora no",
    difficulty = "Dificultad",
    cardLanguage = "Idioma de las cartas",
    intensityDescription = {
        when (it) {
            Intensity.CHILL -> "Ligero y fácil"
            Intensity.BOLD -> "Más pícaro, más personal"
            Intensity.WILD -> "Todo, hasta lo más atrevido"
        }
    },
    points = { n -> esPoints(n) },
    nextPlayers = "Siguiente: jugadores",
    whosPlaying = "¿Quién juega?",
    couplesHint = "Solo vosotros dos · escribid los dos nombres",
    playersHint = { n -> "$n jugadores · los nombres se guardan para la próxima vez" },
    partnerN = { n -> "Pareja $n" },
    playerN = { n -> "Jugador $n" },
    remove = "Quitar",
    addPlayer = "Añadir jugador",
    rounds = "Rondas",
    startGame = "Empezar partida",
    problem = {
        when (it) {
            PlayerRules.Problem.COUPLE_COUNT -> "Closer es para exactamente ${PlayerRules.COUPLE_PLAYERS} jugadores: tú y tu pareja."
            PlayerRules.Problem.TOO_FEW -> "Añade al menos ${PlayerRules.MIN_PLAYERS} jugadores."
            PlayerRules.Problem.TOO_MANY -> "Pueden jugar como máximo ${PlayerRules.MAX_PLAYERS} jugadores."
            PlayerRules.Problem.EMPTY_NAME -> "Cada jugador necesita un apodo."
            PlayerRules.Problem.NAME_TOO_LONG -> "Los apodos pueden tener hasta ${PlayerRules.MAX_NAME_LENGTH} caracteres."
            PlayerRules.Problem.DUPLICATE_NAME -> "Los apodos tienen que ser distintos."
        }
    },
    noCardsLeft = "No quedan cartas para este juego y esta dificultad.",
    roundOf = { r, t -> "Ronda $r de $t" },
    endGame = "Terminar",
    yourTurn = "te toca",
    whatToDo = "Qué hacer",
    reportCard = "Denunciar carta",
    donePoints = { n -> "Hecho  +${esPoints(n)}" },
    endGameTitle = "¿Terminar esta partida?",
    scoresLost = "Se perderán los puntos.",
    keepPlaying = "Seguir jugando",
    reportTitle = "Denunciar esta carta",
    reportBody = "Se saltará y no volverá a salir en este móvil.",
    reportReasons = listOf("Ofensiva o hiriente", "Peligrosa de hacer", "Demasiado explícita", "No tiene sentido"),
    whosNext = "¿Quién sigue?",
    truthOrDare = { name -> "$name, ¿verdad o reto?" },
    spinToPick = "Gira para elegir jugador",
    spinToFind = "Gira para descubrirlo",
    spin = "Girar",
    spinning = "Girando…",
    nextTruthOrDare = "Siguiente: verdad o reto",
    showMyCard = "Ver mi carta",
    time = "Tiempo",
    startTimer = "Iniciar temporizador",
    pause = "Pausa",
    chaosMoment = "Momento de caos",
    chaos = mapOf(
        "swap-seats" to ChaosText("Cambio de sitio", "Todos cambian de sitio con quien tengan enfrente. El último en sentarse lee la siguiente carta en voz alta."),
        "point-vote" to ChaosText("Señala y vota", "A la de tres, todos señalan a quien es más probable que llegue tarde a su propia boda. El más señalado hace una reverencia."),
        "silent-round" to ChaosText("Ronda en silencio", "Nadie habla hasta que acabe el tiempo. Quien hable tiene que contar al grupo un secreto inofensivo."),
        "fist-five" to ChaosText("Del puño al cinco", "A la de tres, puntúa tu día de cero a cinco dedos. Quien dé la nota más baja explica por qué en una frase."),
        "compliment-chain" to ChaosText("Cadena de cumplidos", "Empezando por el jugador de la izquierda, cada uno le hace un cumplido sincero al siguiente."),
        "statue" to ChaosText("Estatuas", "Todos se quedan congelados en la postura en la que estén. Quien se mueva primero lee la siguiente carta con voz dramática."),
        "thumbs" to ChaosText("Guerra de pulgares", "Dos jugadores elegidos por el grupo echan una guerra de pulgares. Quien gane elige quién juega después."),
        "rhyme-chain" to ChaosText("Cadena de rimas", "En círculo, cada uno dice una palabra que rime con la anterior. Quien se atasque o repita, pierde."),
        "never-have" to ChaosText("Yo nunca", "Di un 'yo nunca' inofensivo. Todos los que lo hayan hecho levantan la mano."),
        "duel" to ChaosText("Duelo relámpago", "Dos jugadores se ponen espalda con espalda y se giran a la de tres. Quien haga antes una pose ridícula gana el duelo."),
        "story" to ChaosText("Historia palabra a palabra", "Cread una historia juntos, una palabra cada uno. Quien la termine elige al siguiente que pone un reto."),
        "hands-up" to ChaosText("Manos arriba", "Todos levantan las dos manos. Baja una cuando alguien diga algo con lo que te identifiques. El grupo elige el tema."),
    ),
    gameOver = "Fin de la partida",
    nobodyScored = "Nadie ha puntuado",
    everyoneSafe = "Todos jugaron a lo seguro.",
    sharedWin = "Victoria compartida",
    takesNight = "se lleva la noche",
    skipped = { n -> "$n saltadas" },
    playAgain = "Jugar otra vez",
    home = "Inicio",
    bonusGame = "Partida extra, +5 rondas",
    watchAdBonus = "Mira un anuncio para una partida extra, +5 rondas",
    playGame = "Jugar",
    howManyPlayers = "¿Cuántos jugadores?",
    passPhone = { a, b -> "Pasad un solo móvil. Pueden jugar de $a a $b personas." },
    playersWord = "jugadores",
    countAllOpen = "Todos los juegos están abiertos, incluido Closer para parejas.",
    countNoCloser = "Todos los juegos menos Closer, que es solo para 2.",
    nextChooseGame = "Siguiente: elige un juego",
    pickAGame = { n -> "$n jugadores · elige un juego" },
    twoPlayersOnly = "Solo 2 jugadores",
    avatarHint = { n -> "$n jugadores · toca una imagen para cambiarla" },
    profilePicture = "Foto de perfil",
    theme = "Tema",
    themeHint = "Auto sigue el modo claro u oscuro de tu móvil.",
    themeAuto = "Auto",
    themeLight = "Claro",
    themeDark = "Oscuro",
    turnClock = "Reloj de turno",
    turnClockHint = { s -> "$s segundos para responder, o cuenta como saltada" },
    streakLine = { s, b -> "$s seguidas · la siguiente +$b de bonus" },
    showdown = "Duelo final · puntos dobles",
    longestStreak = { name, n -> "Mejor racha: $name, $n seguidas" },
    skipCost = { n -> "Saltar  −${esPoints(n)}" },
    keepItSecret = "piensa tu respuesta y guárdala en secreto",
    whoGotIt = "Di tu respuesta en voz alta. ¿Quién acertó?",
    guessThenReveal = { name -> "Los demás, adivinad por turnos en voz alta. Luego $name lo revela." },
    revealAnswer = "Revelar respuesta",
    nobodyGotIt = "Nadie acertó",
    scoreEach = { n -> "+${esPoints(n)} cada uno" },
    mode = {
        when (it) {
            GameMode.TRUTH -> "Verdad"
            GameMode.DARE -> "Reto"
            GameMode.WOULD_YOU_RATHER -> "¿Qué prefieres?"
            GameMode.COUPLE -> "Pareja"
            GameMode.NEVER_HAVE_I_EVER -> "Yo nunca"
            GameMode.MOST_LIKELY_TO -> "¿Quién es más probable?"
            GameMode.HOT_SEAT -> "Silla caliente"
            GameMode.CLOSER -> "Closer"
            GameMode.KNOW_ME -> "¿Cuánto me conoces?"
        }
    },
    intensity = {
        when (it) {
            Intensity.CHILL -> "Suave"
            Intensity.BOLD -> "Atrevido"
            Intensity.WILD -> "Salvaje"
        }
    },
    poses = mapOf(
        "hands" to PoseText("De la mano", "De pie o sentados, uno frente al otro, cogeos de las dos manos."),
        "slow_dance" to PoseText("Baile lento", "Muy cerca, abrazados, balanceaos despacio."),
        "hug_behind" to PoseText("Abrazo por detrás", "Uno rodea al otro con los brazos desde atrás."),
        "cuddle" to PoseText("Acurrucados", "Sentaos uno al lado del otro y apoyaos el uno en el otro."),
        "massage" to PoseText("Masaje de hombros", "Uno se sienta y el otro, de pie detrás, le masajea los hombros."),
        "kiss_cheek" to PoseText("Beso en la mejilla", "Muy cerca; uno se acerca para dar un beso suave en la mejilla."),
        "back_to_back" to PoseText("Espalda con espalda", "Sentados o de pie, espalda con espalda, apoyaos el uno en el otro."),
        "whisper" to PoseText("Susurro", "Acércate y susúrrale algo al oído a tu pareja."),
        "foreheads" to PoseText("Frente con frente", "Cara a cara, con las frentes juntas y de la mano."),
        "talk" to PoseText("Cara a cara", "Sentaos frente a frente, con las rodillas cerca, y hablad."),
        "hand_touch" to PoseText("Caricia suave", "Sentados frente a frente; uno acaricia o dibuja suavemente sobre la mano del otro."),
        "lie_down" to PoseText("Tumbados juntos", "Tumbaos uno al lado del otro, hombro con hombro."),
        "feed" to PoseText("Prueba de sabor", "Uno se sienta con los ojos cerrados; el otro le da a probar algo rico."),
        "dance_solo" to PoseText("Baila para tu pareja", "Uno baila con gracia mientras el otro mira."),
        "hand_kiss" to PoseText("Beso en la mano", "Uno coge la mano del otro y le besa el dorso."),
    ),
)
