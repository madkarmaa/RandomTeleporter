package top.madkarma.randomteleporter

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec2Argument
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

private typealias Context = CommandContext<CommandSourceStack>

private typealias Argument = RequiredArgumentBuilder<CommandSourceStack, *>

/**
 * The `/rtp` command, whose forms the README lists. The forms without a target teleport the player
 * who runs them, and the forms without an area use the whole world. Every form that names a target
 * or an area needs the admin permission.
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
                .requires(RtpPermissions.USE)
                .executes { teleport(it, it.self()) }
                .then(
                    Commands.literal("back")
                        .requires(RtpPermissions.BACK)
                        .executes { teleportBack(it.source, it.self()) }
                        .then(targets().executes { teleportBack(it.source, it.targets()) }),
                )
                .then(
                    column(REGION_FROM_POS)
                        .then(
                            column(REGION_TO_POS)
                                .executes { teleport(it, it.self(), it.positionRegion()) }
                                .then(
                                    targets().executes {
                                        teleport(it, it.targets(), it.positionRegion())
                                    },
                                ),
                        ),
                )
                .then(
                    radius()
                        .executes { teleport(it, it.self(), it.aroundOrigin()) }
                        .then(
                            column(ORIGIN_POS).executes {
                                teleport(it, it.self(), it.aroundOrigin(it.column(ORIGIN_POS)))
                            },
                        )
                        .then(
                            withOrigins(
                                targets().executes {
                                    teleport(it, it.targets(), it.aroundOrigin())
                                },
                            ),
                        ),
                )
                .then(
                    targets()
                        .executes { teleport(it, it.targets()) }
                        .then(
                            Commands.literal("back").executes {
                                teleportBack(it.source, it.targets())
                            },
                        )
                        .then(
                            column(REGION_FROM_POS)
                                .then(
                                    column(REGION_TO_POS).executes {
                                        teleport(it, it.targets(), it.positionRegion())
                                    },
                                ),
                        )
                        .then(
                            withOrigins(
                                radius().executes { teleport(it, it.targets(), it.aroundOrigin()) },
                            ),
                        ),
                )
                .then(
                    entity(REGION_FROM_ENTITY)
                        .then(
                            entity(REGION_TO_ENTITY)
                                .executes { teleport(it, it.self(), it.entityRegion()) }
                                .then(
                                    targets().executes {
                                        teleport(it, it.targets(), it.entityRegion())
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
                    teleport(it, it.targets(), it.aroundOrigin(it.column(ORIGIN_POS)))
                },
            )
            .then(
                entity(ORIGIN_ENTITY).executes {
                    teleport(it, it.targets(), it.aroundOrigin(it.entityColumn(ORIGIN_ENTITY)))
                },
            )

    private fun teleport(
        context: Context,
        targets: Collection<Entity>,
        area: TeleportArea = TeleportArea.WORLD,
    ): Int = randomTeleport(context.source, targets, area)

    private fun radius(): Argument =
        Commands.argument(RADIUS, IntegerArgumentType.integer(0)).requires(RtpPermissions.ADMIN)

    private fun targets(): Argument =
        Commands.argument(TARGET, EntityArgument.entities()).requires(RtpPermissions.ADMIN)

    private fun entity(name: String): Argument =
        Commands.argument(name, EntityArgument.entity()).requires(RtpPermissions.ADMIN)

    private fun column(name: String): Argument =
        Commands.argument(name, Vec2Argument.vec2()).requires(RtpPermissions.ADMIN)

    /** The player who runs the command, or no one when the console or a command block runs it. */
    private fun Context.self(): Collection<Entity> = listOfNotNull(source.player)

    private fun Context.targets(): Collection<Entity> = EntityArgument.getEntities(this, TARGET)

    // Vec2Argument.getVec2 rounds to floats, which are too coarse far from the center of the world.
    private fun Context.column(name: String): Vec3 =
        getArgument(name, Coordinates::class.java).getPosition(source)

    private fun Context.entityColumn(name: String): Vec3 =
        EntityArgument.getEntity(this, name).position()

    private fun Context.aroundOrigin(origin: Vec3 = Vec3.ZERO): TeleportArea =
        TeleportArea.around(origin, IntegerArgumentType.getInteger(this, RADIUS))

    private fun Context.positionRegion(): TeleportArea =
        TeleportArea.between(column(REGION_FROM_POS), column(REGION_TO_POS))

    private fun Context.entityRegion(): TeleportArea =
        TeleportArea.between(entityColumn(REGION_FROM_ENTITY), entityColumn(REGION_TO_ENTITY))
}
