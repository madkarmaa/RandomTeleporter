package top.madkarma.randomteleporter

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory

/**
 * The wait between the random teleports of a player without the admin permission, which the
 * `rtp_cooldown` game rule sets in seconds.
 */
object TeleportCooldown {
    private lateinit var rule: GameRule<Int>
    private val lastTeleports = mutableMapOf<UUID, TimeMark>()

    /** Registers the game rule, which the game only allows while the mod starts. */
    fun register() {
        rule =
            GameRuleBuilder.forInteger(0)
                .minValue(0)
                .category(GameRuleCategory.PLAYER)
                .buildAndRegister(
                    Identifier.fromNamespaceAndPath(MOD_ID, "rtp_cooldown"),
                )
    }

    /** Returns how long the player behind [source] still has to wait, if that is positive. */
    fun timeLeft(source: CommandSourceStack): Duration {
        val player = source.player ?: return Duration.ZERO
        if (RtpPermissions.ADMIN.test(source)) return Duration.ZERO

        val lastTeleport = lastTeleports[player.uuid] ?: return Duration.ZERO
        return source.level.gameRules.get(rule).seconds - lastTeleport.elapsedNow()
    }

    fun start(source: CommandSourceStack) {
        val player = source.player ?: return
        lastTeleports[player.uuid] = TimeSource.Monotonic.markNow()
    }

    fun clear() = lastTeleports.clear()
}
