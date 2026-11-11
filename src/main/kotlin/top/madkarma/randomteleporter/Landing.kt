package top.madkarma.randomteleporter

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap

/**
 * Most columns between the islands of the End and many in the oceans of the Overworld have no dry
 * ground, but the world generator rules those out without generating their chunks, so many tries
 * stay cheap.
 */
private const val MAX_ATTEMPTS = 256

/** Picks a random column in [area] and returns where to stand in it, or null if none fits. */
fun findLanding(level: ServerLevel, area: TeleportArea): BlockPos? {
    if (area.isEmpty) return null

    repeat(MAX_ATTEMPTS) {
        val x = area.randomX()
        val z = area.randomZ()
        if (!hasDryGround(level, x, z)) return@repeat

        // The heightmap of a chunk that is not loaded yet only reports the bottom of the world.
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))

        val y = landingY(level, x, z) ?: return@repeat
        return BlockPos(x, y, z)
    }

    return null
}

private fun landingY(level: ServerLevel, x: Int, z: Int): Int? {
    // A ceiling, such as the bedrock roof of the Nether, is the top of every column, so the
    // heightmap would put the target on the roof.
    if (level.dimensionType().hasCeiling()) return floorBelowCeiling(level, x, z)

    // Leaves count as ground, since landing below a treetop would put the target inside the leaves.
    val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)

    // The heightmap of a column without any ground reports the bottom of the world.
    if (y <= level.minY) return null

    // Lakes, which the world generator adds after its estimate, can still cover the ground.
    if (!level.getBlockState(BlockPos(x, y - 1, z)).fluidState.isEmpty) return null

    return y
}

/**
 * Asks the world generator whether the column has ground that is not under water, which is much
 * faster than generating it.
 */
private fun hasDryGround(level: ServerLevel, x: Int, z: Int): Boolean {
    val surfaceY = generatedHeight(level, x, z, Heightmap.Types.WORLD_SURFACE_WG)
    if (surfaceY <= level.minY) return false

    // The surface of an ocean is above its floor, while dry ground is both.
    return surfaceY == generatedHeight(level, x, z, Heightmap.Types.OCEAN_FLOOR_WG)
}

private fun generatedHeight(level: ServerLevel, x: Int, z: Int, type: Heightmap.Types): Int {
    val chunkSource = level.chunkSource
    return chunkSource.generator.getBaseHeight(x, z, type, level, chunkSource.randomState())
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
