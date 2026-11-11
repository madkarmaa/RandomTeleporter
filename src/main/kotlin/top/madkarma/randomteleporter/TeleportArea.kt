package top.madkarma.randomteleporter

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.phys.Vec3

/**
 * The farthest a target may be from the center of the world on either axis, which keeps it well
 * inside the largest world border.
 */
const val MAX_DISTANCE = 28_990_000

/**
 * The rectangle of columns that a random teleport picks its target from. Both bounds are inside,
 * and an area whose bounds cross holds no columns.
 */
data class TeleportArea(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int) {
    val isEmpty: Boolean
        get() = minX > maxX || minZ > maxZ

    fun randomX(): Int = Random.nextInt(minX..maxX)

    fun randomZ(): Int = Random.nextInt(minZ..maxZ)

    /** The columns of this area that are also in [other]. */
    fun intersect(other: TeleportArea): TeleportArea =
        TeleportArea(
            max(minX, other.minX),
            max(minZ, other.minZ),
            min(maxX, other.maxX),
            min(maxZ, other.maxZ),
        )

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
                .intersect(WORLD)

        /** The columns that are completely inside [border]. */
        fun inside(border: WorldBorder): TeleportArea =
            TeleportArea(
                ceil(border.minX).toInt(),
                ceil(border.minZ).toInt(),
                floor(border.maxX).toInt() - 1,
                floor(border.maxZ).toInt() - 1,
            )
    }
}
