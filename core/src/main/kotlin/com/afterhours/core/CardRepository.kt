package com.afterhours.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/** Source of cards. Bundled JSON today; could be swapped for a local database later. */
interface CardRepository {
    fun allCards(): List<Card>
}

class InMemoryCardRepository(private val cards: List<Card>) : CardRepository {
    override fun allCards(): List<Card> = cards
}

/** Loads the bundled seed cards from the classpath JSON resource. */
class BundledCardRepository(
    private val resource: String = "cards_en.json",
) : CardRepository {
    private val cards: List<Card> by lazy {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(resource)) {
            "Missing card resource $resource"
        }
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        parseCards(text)
    }

    override fun allCards(): List<Card> = cards

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parseCards(text: String): List<Card> =
            json.decodeFromString(ListSerializer(Card.serializer()), text)
    }
}
