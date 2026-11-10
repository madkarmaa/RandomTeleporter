package top.madkarma.randomteleporter

import com.mojang.brigadier.Command
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

/** Brigadier's result for a command that failed. */
private const val FAILURE = 0

// Clients without the mod show the fallbacks, since only the mod's language files hold the keys.
private fun noTarget(): Component =
    Component.translatableWithFallback(
        "commands.randomteleporter.no_target",
        "No one to teleport. Name the target when the command does not run as a player",
    )

/** Teleports [target] to a random position in [area] and remembers where it came from. */
fun randomTeleport(source: CommandSourceStack, target: Entity?, area: TeleportArea): Int {
    if (target == null) return fail(source, noTarget())

    val level = target.level() as ServerLevel
    val landing = findLanding(level, area)

    placePlatform(level, landing)
    TeleportHistory.remember(target)
    target.teleportTo(
        level,
        landing.x + 0.5,
        landing.y.toDouble(),
        landing.z + 0.5,
        setOf(),
        target.yRot,
        target.xRot,
        false,
    )

    source.sendSuccess(
        {
            Component.translatableWithFallback(
                "commands.randomteleporter.teleported",
                "Teleported %s to %d %d %d",
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

    val previousPosition =
        TeleportHistory.previousPosition(target)
            ?: return fail(
                source,
                Component.translatableWithFallback(
                    "commands.randomteleporter.no_previous_position",
                    "%s has not been randomly teleported yet, so it has nowhere to go back to",
                    target.name,
                ),
            )

    target.teleportTo(
        target.level() as ServerLevel,
        previousPosition.x,
        previousPosition.y,
        previousPosition.z,
        setOf(),
        target.yRot,
        target.xRot,
        false,
    )

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

private fun fail(source: CommandSourceStack, message: Component): Int {
    source.sendFailure(message)
    return FAILURE
}
