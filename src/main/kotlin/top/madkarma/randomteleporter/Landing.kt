package top.madkarma.randomteleporter

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap

/**
 * Most columns between the islands of the End have no ground, but the world generator rules those
 * out without generating their chunks, so many tries stay cheap.
 */
private const val MAX_ATTEMPTS = 256

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

/** Picks a random column in [area] and returns where to stand in it, or null if none fits. */
fun findLanding(level: ServerLevel, area: TeleportArea): BlockPos? {
    repeat(MAX_ATTEMPTS) {
        val x = area.randomX()
        val z = area.randomZ()
        if (!hasTerrain(level, x, z)) return@repeat

        // The heightmap of a chunk that is not loaded yet only reports the bottom of the world.
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))

        val y = landingY(level, x, z) ?: return@repeat
        return BlockPos(x, y, z)
    }

    return null
}

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

private fun landingY(level: ServerLevel, x: Int, z: Int): Int? {
    // A ceiling, such as the bedrock roof of the Nether, is the top of every column, so the
    // heightmap would put the target on the roof.
    if (level.dimensionType().hasCeiling()) return floorBelowCeiling(level, x, z)

    val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)

    // The heightmap of a column without any ground reports the bottom of the world.
    return y.takeIf { it > level.minY }
}

/**
 * Asks the world generator whether the column has terrain, which is much faster than generating it.
 */
private fun hasTerrain(level: ServerLevel, x: Int, z: Int): Boolean {
    val chunkSource = level.chunkSource
    val terrainHeight =
        chunkSource.generator.getBaseHeight(
            x,
            z,
            Heightmap.Types.WORLD_SURFACE_WG,
            level,
            chunkSource.randomState(),
        )

    return terrainHeight > level.minY
}

/** Returns the highest feet position under the ceiling with room for the target to stand. */
private fun floorBelowCeiling(level: ServerLevel, x: Int, z: Int): Int? {
    val position = BlockPos.MutableBlockPos(x, 0, z)
    val ceilingY = level.minY + level.logicalHeight

    for (feetY in ceilingY - 2 downTo level.minY + 1) {
        if (!level.getBlockState(position.setY(feetY + 1)).isAir) continue
        if (!level.getBlockState(position.setY(feetY)).isAir) continue

        // The platform covers liquids. Bedrock never counts, since the air pockets inside the
        // bedrock roof of the Nether would otherwise trap the target in the roof.
        val ground = level.getBlockState(position.setY(feetY - 1))
        if ((ground.isSolid || ground.liquid()) && !ground.`is`(Blocks.BEDROCK)) return feetY
    }

    return null
}
