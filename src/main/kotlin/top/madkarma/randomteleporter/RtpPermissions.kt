package top.madkarma.randomteleporter

import java.util.function.Predicate
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.server.permissions.PermissionLevel

/**
 * The permissions of `/rtp`. A permissions mod can grant each of them, and without one they fall
 * back to the permission level of the vanilla `/tp` command.
 */
object RtpPermissions {
    /** Teleports the player who runs the command anywhere in the world. */
    val USE: Predicate<CommandSourceStack> = permission("rtp")

    /** Teleports the player who runs the command back. */
    val BACK: Predicate<CommandSourceStack> = permission("rtp/back")

    /** Names targets and areas, and skips the cooldown. */
    val ADMIN: Predicate<CommandSourceStack> = permission("rtp/admin")

    private fun permission(path: String): Predicate<CommandSourceStack> =
        PermissionPredicates.require(
            Identifier.fromNamespaceAndPath(MOD_ID, path),
            PermissionLevel.GAMEMASTERS,
        )
}
