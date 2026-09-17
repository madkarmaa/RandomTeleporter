package xyz.thewhitedog9487

import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.thewhitedog9487.Event.ResourceReloaderListenerRegister
import xyz.thewhitedog9487.Event.ServerLifecycleListenerRegister

const val ModID = "randomteleporter"
const val FriendlyModID = "RandomTeleporter"
val ModLogger: Logger = LoggerFactory.getLogger(FriendlyModID)

object RandomTeleporter : ModInitializer {

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        ServerLifecycleListenerRegister()
		ResourceReloaderListenerRegister()
//		↑ 命令注册在这个里面，在资源加载完成之后注册事件监听器
		if (FabricLoader.getInstance().environmentType == EnvType.SERVER){
//			独立服务器根本不可能重载资源，需要手动注册
			CommandRegister() } } }