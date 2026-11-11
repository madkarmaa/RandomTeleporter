package top.madkarma.randomteleporter

import java.util.UUID
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/** Where an entity was before a random teleport. */
data class PreviousPosition(val dimension: ResourceKey<Level>, val position: Vec3)

/** The position of each entity before its last random teleport, which `/rtp back` returns to. */
object TeleportHistory {
    private val previousPositions = mutableMapOf<UUID, PreviousPosition>()

    fun remember(entity: Entity) {
        previousPositions[entity.uuid] =
            PreviousPosition(entity.level().dimension(), entity.position())
    }

    fun previousPosition(entity: Entity): PreviousPosition? = previousPositions[entity.uuid]

    fun clear() = previousPositions.clear()
}
