package com.afterhours.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A simple illustration for a couples card: two faceless, clothed mannequin figures
 * (one pink, one blue) drawn as round-capped strokes on a [WIDTH] x [HEIGHT] canvas.
 * Poses are deliberately limited to affectionate, non-explicit ones (hand holding,
 * slow dance, cuddles, massage, kisses on the cheek or hand, talking face to face).
 */
@Serializable
data class Pose(
    val id: String,
    /** Short name shown above the card, e.g. "Slow dance". */
    val title: String,
    /** One line telling the couple how to get into the pose. */
    val hint: String,
    val pink: Map<String, List<Float>>,
    val blue: Map<String, List<Float>>,
    /** Optional bench drawn under seated figures: [fromX, toX, y]. */
    val seat: List<Float>? = null,
) {
    companion object {
        const val WIDTH = 200f
        const val HEIGHT = 140f
        const val GROUND_Y = 132f
        /** Every figure has these joints. h = head centre, n = neck/shoulders, p = hips. */
        val JOINTS = listOf("h", "n", "p", "le", "lh", "re", "rh", "lk", "lf", "rk", "rf")
        /** Limb strokes, as joint pairs. */
        val LIMBS = listOf("n" to "le", "le" to "lh", "n" to "re", "re" to "rh", "p" to "lk", "lk" to "lf", "p" to "rk", "rk" to "rf")
    }
}

object PoseLibrary {
    private val json = Json { ignoreUnknownKeys = true }

    val all: List<Pose> by lazy {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("closer_poses.json")) { "Missing closer_poses.json" }
        json.decodeFromString(ListSerializer(Pose.serializer()), stream.use { it.readBytes().toString(Charsets.UTF_8) })
    }

    private val byId by lazy { all.associateBy { it.id } }

    fun get(id: String?): Pose? = id?.let { byId[it] }
}
