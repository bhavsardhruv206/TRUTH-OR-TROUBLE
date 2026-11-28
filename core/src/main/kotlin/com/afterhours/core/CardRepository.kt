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

/** Loads the bundled card packs (one JSON file per [CardLanguage]) from the classpath. */
class BundledCardRepository(
    private val resources: List<String> = CardLanguage.entries.map { it.resource },
) : CardRepository {
    constructor(resource: String) : this(listOf(resource))

    private val cards: List<Card> by lazy {
        resources.flatMap { resource ->
            val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(resource)) {
                "Missing card resource $resource"
            }
            parseCards(stream.use { it.readBytes().toString(Charsets.UTF_8) })
        }
    }

    override fun allCards(): List<Card> = cards

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parseCards(text: String): List<Card> =
            json.decodeFromString(ListSerializer(Card.serializer()), text)
    }
}
