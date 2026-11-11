package top.madkarma.randomteleporter

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt
import net.minecraft.world.phys.Vec3

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
        val WORLD: TeleportArea =
            TeleportArea(-MAX_DISTANCE, -MAX_DISTANCE, MAX_DISTANCE, MAX_DISTANCE)

        /** The square that reaches [radius] blocks from the column of [center]. */
        fun around(center: Vec3, radius: Int): TeleportArea {
            val reach = radius.toDouble()
            return between(center.subtract(reach, 0.0, reach), center.add(reach, 0.0, reach))
        }

        /** The rectangle between the columns of two opposite corners. */
        fun between(corner: Vec3, oppositeCorner: Vec3): TeleportArea =
            TeleportArea(
                floor(min(corner.x, oppositeCorner.x)).toInt(),
                floor(min(corner.z, oppositeCorner.z)).toInt(),
                floor(max(corner.x, oppositeCorner.x)).toInt(),
                floor(max(corner.z, oppositeCorner.z)).toInt(),
            )
    }
}
