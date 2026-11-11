package top.madkarma.randomteleporter

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks

private val PLATFORM_BLOCK = Blocks.GLASS

/** The blocks under the landing position that the platform replaces, so the target cannot fall. */
private val PLATFORM_REPLACEABLE_BLOCKS =
    setOf(
        Blocks.AIR,
        Blocks.VOID_AIR,
        Blocks.CAVE_AIR,
        Blocks.WATER,
        Blocks.LAVA,
        Blocks.SHORT_GRASS,
        Blocks.VINE,
    )

/** Turns the 3×3 blocks under [landing] into a platform where they would not hold the target. */
fun placePlatform(level: ServerLevel, landing: BlockPos) {
    val position = BlockPos.MutableBlockPos()

    for (offsetX in -1..1) {
        for (offsetZ in -1..1) {
            position.set(landing.x + offsetX, landing.y - 1, landing.z + offsetZ)
            if (level.getBlockState(position).block !in PLATFORM_REPLACEABLE_BLOCKS) continue

            level.setBlockAndUpdate(position, PLATFORM_BLOCK.defaultBlockState())
        }
    }
}
