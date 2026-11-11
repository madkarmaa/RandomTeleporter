package top.madkarma.randomteleporter

import java.util.concurrent.CompletableFuture
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ColumnPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap

/**
 * Most columns between the islands of the End and many in the oceans of the Overworld have no dry
 * ground, but the world generator rules those out without generating their chunks, so many tries
 * stay cheap.
 */
private const val MAX_COLUMN_ATTEMPTS = 256

/**
 * Generating a chunk is slow, and only what the world generator's estimate misses, such as a lake,
 * makes its column unusable, so a few chunks are enough.
 */
private const val MAX_CHUNK_ATTEMPTS = 4

/**
 * Picks random columns in [area] and completes on the server thread with where to stand in one, or
 * with null if none fits.
 */
fun findLanding(
    level: ServerLevel,
    area: TeleportArea,
    chunkAttempts: Int = MAX_CHUNK_ATTEMPTS,
): CompletableFuture<BlockPos?> {
    if (chunkAttempts == 0) return CompletableFuture.completedFuture(null)
    val column = findDryColumn(level, area) ?: return CompletableFuture.completedFuture(null)

    // Unlike getChunk, a ticket lets the server keep ticking while the chunk generates.
    return level.chunkSource
        .addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, column.toChunkPos(), 0)
        .thenComposeAsync(
            {
                val landing = landingIn(level, column)
                if (landing == null) findLanding(level, area, chunkAttempts - 1)
                else CompletableFuture.completedFuture(landing)
            },
            level.server,
        )
}

private fun findDryColumn(level: ServerLevel, area: TeleportArea): ColumnPos? {
    if (area.isEmpty) return null

    repeat(MAX_COLUMN_ATTEMPTS) {
        val column = ColumnPos(area.randomX(), area.randomZ())
        if (hasDryGround(level, column)) return column
    }

    return null
}

/**
 * Asks the world generator whether the column has ground that is not under water, which is much
 * faster than generating it.
 */
private fun hasDryGround(level: ServerLevel, column: ColumnPos): Boolean {
    val chunkSource = level.chunkSource
    val blocks =
        chunkSource.generator.getBaseColumn(column.x, column.z, level, chunkSource.randomState())

    for (y in level.maxY downTo level.minY) {
        val surface = blocks.getBlock(y)
        if (!surface.isAir) return surface.fluidState.isEmpty
    }

    return false
}

private fun landingIn(level: ServerLevel, column: ColumnPos): BlockPos? {
    // The ticket can expire before this runs, and the heightmap of a chunk that is not loaded
    // only reports the bottom of the world.
    val chunk = column.toChunkPos()
    level.getChunk(chunk.x, chunk.z)

    // A ceiling, such as the bedrock roof of the Nether, is the top of every column, so the
    // heightmap would put the target on the roof.
    val y =
        if (level.dimensionType().hasCeiling()) floorBelowCeiling(level, column)
        else surfaceY(level, column)

    return y?.let { BlockPos(column.x, it, column.z) }
}

private fun surfaceY(level: ServerLevel, column: ColumnPos): Int? {
    // Leaves count as ground, since landing below a treetop would put the target inside the leaves.
    val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.x, column.z)

    // The heightmap of a column without any ground reports the bottom of the world.
    if (y <= level.minY) return null

    // Lakes, which the world generator adds after its estimate, can still cover the ground.
    if (!level.getBlockState(BlockPos(column.x, y - 1, column.z)).fluidState.isEmpty) return null

    return y
}

/** Returns the highest feet position under the ceiling with room for the target to stand. */
private fun floorBelowCeiling(level: ServerLevel, column: ColumnPos): Int? {
    val position = BlockPos.MutableBlockPos(column.x, 0, column.z)
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
