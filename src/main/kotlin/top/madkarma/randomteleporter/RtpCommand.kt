package top.madkarma.randomteleporter

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec2

private typealias Context = CommandContext<CommandSourceStack>

private typealias Argument = RequiredArgumentBuilder<CommandSourceStack, *>

/**
 * The `/rtp` command, whose forms the README lists. The forms without a target teleport the player
 * who runs them, and the forms without an area use the whole world.
 */
object RtpCommand {
    private const val TARGET = "target"
    private const val RADIUS = "radius"
    private const val ORIGIN_POS = "originPos"
    private const val ORIGIN_ENTITY = "originEntity"
    private const val REGION_FROM_POS = "regionFromPos"
    private const val REGION_TO_POS = "regionToPos"
    private const val REGION_FROM_ENTITY = "regionFromEntity"
    private const val REGION_TO_ENTITY = "regionToEntity"

    // A number is also a valid player name, and when several sibling arguments parse the same
    // input, Brigadier prefers the one added first. The branches that take numbers therefore come
    // before the ones that take entities, which makes their order part of the command's behavior.
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("rtp")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes { teleport(it, it.player()) }
                .then(
                    Commands.literal("back")
                        .executes { teleportBack(it.source, it.player()) }
                        .then(entity(TARGET).executes { teleportBack(it.source, it.target()) }),
                )
                .then(
                    column(REGION_FROM_POS)
                        .then(
                            column(REGION_TO_POS)
                                .executes { teleport(it, it.player(), it.positionRegion()) }
                                .then(
                                    entity(TARGET).executes {
                                        teleport(it, it.target(), it.positionRegion())
                                    },
                                ),
                        ),
                )
                .then(
                    radius()
                        .executes { teleport(it, it.player(), it.aroundOrigin()) }
                        .then(
                            column(ORIGIN_POS).executes {
                                teleport(it, it.player(), it.aroundOrigin(it.column(ORIGIN_POS)))
                            },
                        )
                        .then(
                            withOrigins(
                                entity(TARGET).executes {
                                    teleport(it, it.target(), it.aroundOrigin())
                                },
                            ),
                        ),
                )
                .then(
                    entity(TARGET)
                        .executes { teleport(it, it.target()) }
                        .then(
                            Commands.literal("back").executes {
                                teleportBack(it.source, it.target())
                            },
                        )
                        .then(
                            column(REGION_FROM_POS)
                                .then(
                                    column(REGION_TO_POS).executes {
                                        teleport(it, it.target(), it.positionRegion())
                                    },
                                ),
                        )
                        .then(
                            withOrigins(
                                radius().executes { teleport(it, it.target(), it.aroundOrigin()) },
                            ),
                        ),
                )
                .then(
                    entity(REGION_FROM_ENTITY)
                        .then(
                            entity(REGION_TO_ENTITY)
                                .executes { teleport(it, it.player(), it.entityRegion()) }
                                .then(
                                    entity(TARGET).executes {
                                        teleport(it, it.target(), it.entityRegion())
                                    },
                                ),
                        ),
                ),
        )
    }

    /**
     * Adds the two ways to name the center of the radius after [node], which comes after both the
     * radius and the target.
     */
    private fun withOrigins(node: Argument): Argument =
        node
            .then(
                column(ORIGIN_POS).executes {
                    teleport(it, it.target(), it.aroundOrigin(it.column(ORIGIN_POS)))
                },
            )
            .then(
                entity(ORIGIN_ENTITY).executes {
                    teleport(it, it.target(), it.aroundOrigin(it.entityColumn(ORIGIN_ENTITY)))
                },
            )

    private fun teleport(
        context: Context,
        target: Entity?,
        area: TeleportArea = TeleportArea.WORLD,
    ): Int = randomTeleport(context.source, target, area)

    private fun radius(): Argument = Commands.argument(RADIUS, IntegerArgumentType.integer(0))

    private fun entity(name: String): Argument = Commands.argument(name, EntityArgument.entity())

    private fun column(name: String): Argument = Commands.argument(name, Vec2Argument.vec2())

    private fun Context.player(): Entity? = source.player

    private fun Context.target(): Entity = EntityArgument.getEntity(this, TARGET)

    private fun Context.column(name: String): Vec2 = Vec2Argument.getVec2(this, name)

    private fun Context.entityColumn(name: String): Vec2 =
        EntityArgument.getEntity(this, name).let { Vec2(it.x.toFloat(), it.z.toFloat()) }

    private fun Context.aroundOrigin(origin: Vec2 = Vec2.ZERO): TeleportArea =
        TeleportArea.around(origin, IntegerArgumentType.getInteger(this, RADIUS))

    private fun Context.positionRegion(): TeleportArea =
        TeleportArea.between(column(REGION_FROM_POS), column(REGION_TO_POS))

    private fun Context.entityRegion(): TeleportArea =
        TeleportArea.between(entityColumn(REGION_FROM_ENTITY), entityColumn(REGION_TO_ENTITY))
}
