package top.madkarma.randomteleporter

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object RandomTeleporter : ModInitializer {
    override fun onInitialize() {
        TeleportCooldown.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            RtpCommand.register(dispatcher)
        }

        // Singleplayer worlds start an integrated server each time the player opens one, and the
        // positions and cooldowns of one world mean nothing in the next.
        ServerLifecycleEvents.SERVER_STARTED.register {
            TeleportHistory.clear()
            TeleportCooldown.clear()
        }
    }
}
