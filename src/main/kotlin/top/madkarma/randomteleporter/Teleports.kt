package top.madkarma.randomteleporter

import com.mojang.brigadier.Command
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/** Brigadier's result for a command that failed. */
private const val FAILURE = 0

// Clients without the mod show the fallbacks, since only the mod's language files hold the keys.
private fun noTarget(): Component =
    Component.translatableWithFallback(
        "commands.randomteleporter.no_target",
        "No one to teleport. Name the target when the command does not run as a player",
    )

/** Teleports [target] to a random safe position in [area] and remembers where it came from. */
fun randomTeleport(source: CommandSourceStack, target: Entity?, area: TeleportArea): Int {
    if (target == null) return fail(source, noTarget())

    val level = target.level() as ServerLevel
    val landing =
        findLanding(level, area.intersect(TeleportArea.inside(level.worldBorder)))
            ?: return fail(
                source,
                Component.translatableWithFallback(
                    "commands.randomteleporter.no_safe_position",
                    "Found no safe position to teleport %s to",
                    target.name,
                ),
            )

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
    return Command.SINGLE_SUCCESS
}

/** Teleports [target] back to where it was before its last random teleport. */
fun teleportBack(source: CommandSourceStack, target: Entity?): Int {
    if (target == null) return fail(source, noTarget())

    val previous = TeleportHistory.previousPosition(target)
    val level = previous?.let { source.server.getLevel(it.dimension) }

    if (previous == null || level == null) {
        return fail(
            source,
            Component.translatableWithFallback(
                "commands.randomteleporter.no_previous_position",
                "%s has not been randomly teleported yet, so it has nowhere to go back to",
                target.name,
            ),
        )
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
    return Command.SINGLE_SUCCESS
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
