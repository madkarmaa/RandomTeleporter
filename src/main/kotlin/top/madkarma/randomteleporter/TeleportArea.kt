package top.madkarma.randomteleporter

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt
import net.minecraft.world.phys.Vec2

/**
 * The farthest a target may be from the center of the world on either axis, which keeps it well
 * inside the world border.
 */
const val MAX_DISTANCE = 28_990_000

/**
 * The rectangle of columns that a random teleport picks its target from. Both bounds are inside.
 */
data class TeleportArea(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int) {
    fun randomX(): Int = Random.nextInt(minX..maxX).coerceIn(-MAX_DISTANCE, MAX_DISTANCE)

    fun randomZ(): Int = Random.nextInt(minZ..maxZ).coerceIn(-MAX_DISTANCE, MAX_DISTANCE)

    companion object {
        /** The whole world that the mod teleports into. */
        val WORLD: TeleportArea = around(Vec2.ZERO, MAX_DISTANCE)

        /**
         * The square that reaches [radius] blocks from [center], whose y holds the z coordinate.
         */
        fun around(center: Vec2, radius: Int): TeleportArea =
            TeleportArea(
                (center.x - radius).toInt(),
                (center.y - radius).toInt(),
                (center.x + radius).toInt(),
                (center.y + radius).toInt(),
            )

        /** The rectangle between two opposite corners, whose y holds the z coordinate. */
        fun between(corner: Vec2, oppositeCorner: Vec2): TeleportArea =
            TeleportArea(
                min(corner.x, oppositeCorner.x).toInt(),
                min(corner.y, oppositeCorner.y).toInt(),
                max(corner.x, oppositeCorner.x).toInt(),
                max(corner.y, oppositeCorner.y).toInt(),
            )
    }
}
