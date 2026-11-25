package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

// Fills small air pockets in a chunk that no cave connects to the chunk's sides.
public final class SealedPockets {

    private static final int MAX_CELLS = 400;
    private static final byte SOLID = 0;
    private static final byte AIR = 1;
    private static final byte FLUID = 2;
    private static final byte SEEN = 3;
    private static final int[][] OFFSETS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private SealedPockets() {
    }

    public static void fill(ChunkAccess chunk) {
        int minY = chunk.getMinY();
        int height = chunk.getHeight();
        int total = height << 8;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        byte[] cells = new byte[total];
        boolean anyAir = false;
        for (int index = 0; index < total; index++) {
            BlockState state = chunk.getBlockState(pos.set(baseX + (index & 15), minY + (index >> 8), baseZ + ((index >> 4) & 15)));
            if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR)) {
                cells[index] = AIR;
                anyAir = true;
            } else if (!state.getFluidState().isEmpty()) {
                cells[index] = FLUID;
            }
        }
        if (!anyAir) {
            return;
        }

        int[] queue = new int[total];
        int tail = 0;
        for (int seed = 0; seed < total; seed++) {
            if (cells[seed] != AIR) {
                continue;
            }
            int start = tail;
            boolean open = false;
            cells[seed] = SEEN;
            queue[tail++] = seed;
            for (int head = start; head < tail; head++) {
                int index = queue[head];
                int x = index & 15;
                int z = (index >> 4) & 15;
                int y = index >> 8;
                if (x == 0 || x == 15 || z == 0 || z == 15) {
                    open = true;
                }
                for (int[] offset : OFFSETS) {
                    int nx = x + offset[0];
                    int ny = y + offset[1];
                    int nz = z + offset[2];
                    if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < 0 || ny >= height) {
                        continue;
                    }
                    int next = (ny << 8) | (nz << 4) | nx;
                    if (cells[next] == AIR) {
                        cells[next] = SEEN;
                        queue[tail++] = next;
                    } else if (cells[next] == FLUID) {
                        open = true;
                    }
                }
            }
            if (!open && tail - start < MAX_CELLS) {
                fillPocket(chunk, pos, cells, queue, start, tail, minY, baseX, baseZ);
            }
        }
    }

    private static void fillPocket(ChunkAccess chunk, BlockPos.MutableBlockPos pos, byte[] cells, int[] queue, int start, int end, int minY, int baseX, int baseZ) {
        BlockState[] seen = new BlockState[6 * (end - start)];
        int[] counts = new int[seen.length];
        int distinct = 0;
        int height = chunk.getHeight();
        for (int i = start; i < end; i++) {
            int index = queue[i];
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;
            for (int[] offset : OFFSETS) {
                int nx = x + offset[0];
                int ny = y + offset[1];
                int nz = z + offset[2];
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < 0 || ny >= height) {
                    continue;
                }
                if (cells[(ny << 8) | (nz << 4) | nx] != SOLID) {
                    continue;
                }
                BlockState state = chunk.getBlockState(pos.set(baseX + nx, minY + ny, baseZ + nz));
                int slot = 0;
                while (slot < distinct && seen[slot] != state) {
                    slot++;
                }
                if (slot == distinct) {
                    seen[distinct++] = state;
                }
                counts[slot]++;
            }
        }

        BlockState fill = null;
        int best = 0;
        for (int slot = 0; slot < distinct; slot++) {
            if (counts[slot] > best) {
                best = counts[slot];
                fill = seen[slot];
            }
        }
        for (int i = start; i < end; i++) {
            int index = queue[i];
            int y = minY + (index >> 8);
            BlockState state = fill != null ? fill : fallback(y);
            chunk.setBlockState(pos.set(baseX + (index & 15), y, baseZ + ((index >> 4) & 15)), state);
        }
    }

    private static BlockState fallback(int y) {
        Block block = y < 0 ? Blocks.DEEPSLATE : Blocks.STONE;
        return block.defaultBlockState();
    }
}
