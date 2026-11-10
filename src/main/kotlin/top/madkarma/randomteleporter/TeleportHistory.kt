package top.madkarma.randomteleporter

import java.util.UUID
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/** The position of each entity before its last random teleport, which `/rtp back` returns to. */
object TeleportHistory {
    private val previousPositions = mutableMapOf<UUID, Vec3>()

    fun remember(entity: Entity) {
        previousPositions[entity.uuid] = entity.position()
    }

    fun previousPosition(entity: Entity): Vec3? = previousPositions[entity.uuid]

    fun clear() = previousPositions.clear()
}
