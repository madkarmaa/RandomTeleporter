package top.madkarma.randomteleporter

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(MOD_ID)

/** Brigadier's result for a command that failed. */
private const val FAILURE = 0

// Clients without the mod show the fallbacks, since only the mod's language files hold the keys.
private fun noTarget(): Component =
    Component.translatableWithFallback(
        "commands.randomteleporter.no_target",
        "No one to teleport. Name the target when the command does not run as a player",
    )

/**
 * Starts teleporting each of [targets] to its own random safe position in [area] and returns how
 * many searches it started. A search reports its outcome once the chunk of its position loaded.
 */
fun randomTeleport(
    source: CommandSourceStack,
    targets: Collection<Entity>,
    area: TeleportArea,
): Int {
    if (targets.isEmpty()) return fail(source, noTarget())

    val wait = TeleportCooldown.timeLeft(source)
    if (wait.isPositive()) {
        return fail(
            source,
            Component.translatableWithFallback(
                "commands.randomteleporter.cooldown",
                "You can randomly teleport again in %s seconds",
                wait.inWholeSeconds + 1,
            ),
        )
    }

    TeleportCooldown.start(source)
    targets.forEach { startRandomTeleport(source, it, area) }
    return targets.size
}

private fun startRandomTeleport(source: CommandSourceStack, target: Entity, area: TeleportArea) {
    val level = target.level() as ServerLevel

    findLanding(level, area.intersect(TeleportArea.inside(level.worldBorder)))
        .whenCompleteAsync(
            { landing, error ->
                if (error != null) LOGGER.error("The search for a landing position failed", error)
                finishRandomTeleport(source, target, level, landing)
            },
            level.server,
        )
}

private fun finishRandomTeleport(
    source: CommandSourceStack,
    target: Entity,
    level: ServerLevel,
    landing: BlockPos?,
) {
    // The target can die or leave the server while the chunk loads.
    if (target.isRemoved) return

    if (landing == null) {
        fail(
            source,
            Component.translatableWithFallback(
                "commands.randomteleporter.no_safe_position",
                "Found no safe position to teleport %s to",
                target.name,
            ),
        )
        return
    }

    placePlatform(level, landing)
    TeleportHistory.remember(target)
    teleport(target, level, Vec3.atBottomCenterOf(landing))

    source.sendSuccess(
        {
            Component.translatableWithFallback(
                "commands.randomteleporter.teleported",
                "Teleported %s to %s %s %s",
                target.name,
                landing.x,
                landing.y,
                landing.z,
            )
        },
        true,
    )
}

/**
 * Teleports each of [targets] back to where it was before its last random teleport and returns how
 * many it teleported.
 */
fun teleportBack(source: CommandSourceStack, targets: Collection<Entity>): Int {
    if (targets.isEmpty()) return fail(source, noTarget())

    return targets.count { teleportBack(source, it) }
}

private fun teleportBack(source: CommandSourceStack, target: Entity): Boolean {
    val previous = TeleportHistory.previousPosition(target)
    val level = previous?.let { source.server.getLevel(it.dimension) }

    if (previous == null || level == null) {
        fail(
            source,
            Component.translatableWithFallback(
                "commands.randomteleporter.no_previous_position",
                "%s has not been randomly teleported yet, so it has nowhere to go back to",
                target.name,
            ),
        )
        return false
    }

    teleport(target, level, previous.position)

    source.sendSuccess(
        {
            Component.translatableWithFallback(
                "commands.randomteleporter.teleported_back",
                "Teleported %s back to the previous position",
                target.name,
            )
        },
        true,
    )
    return true
}

private fun teleport(target: Entity, level: ServerLevel, position: Vec3) {
    target.teleportTo(
        level,
        position.x,
        position.y,
        position.z,
        setOf(),
        target.yRot,
        target.xRot,
        false,
    )
}

private fun fail(source: CommandSourceStack, message: Component): Int {
    source.sendFailure(message)
    return FAILURE
}
