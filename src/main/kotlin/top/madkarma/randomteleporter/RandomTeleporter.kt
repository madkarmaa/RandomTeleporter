package top.madkarma.randomteleporter

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object RandomTeleporter : ModInitializer {
    const val MOD_ID: String = "randomteleporter"

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            RtpCommand.register(dispatcher)
        }

        // Singleplayer worlds start an integrated server each time the player opens one, and the
        // positions of one world mean nothing in the next.
        ServerLifecycleEvents.SERVER_STARTED.register { TeleportHistory.clear() }
    }
}
