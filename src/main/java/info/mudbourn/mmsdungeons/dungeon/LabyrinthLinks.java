package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

// Corridors that join neighbouring labyrinths, and shafts that join a cell's upper and lower labyrinth, each half built by its own side.
public final class LabyrinthLinks {

    private static final long BRIDGE_SALT = 0x62726964676573L;
    private static final long SHAFT_SALT = 0x7368616674L;
    private static final int BRIDGE_PERCENT = 60;
    private static final int SHAFT_PERCENT = 50;
    private static final int EDGE_MARGIN = 40;
    private static final int FLAT_TAIL = 4;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    // Bottom of the upper half's shaft and of the lower half's top shaft step, which meet at Y 0.
    private static final int UPPER_HANDOFF = 0;
    private static final int LOWER_HANDOFF = -4;
    // Corridor meeting heights: the same span the labyrinth cores start in.
    private static final int UPPER_MEET_MIN = 20;
    private static final int LOWER_MEET_MIN = -100;
    private static final int MEET_RANGE = 31;

    private LabyrinthLinks() {
    }

    private record Doorway(BlockPos pos, Direction facing) {
    }

    public static List<StructurePiece> corridors(List<StructurePiece> kept, DddRuin ruin, long seed, StructureTemplateManager templates) {
        List<StructurePiece> out = new ArrayList<>();
        if (kept.isEmpty()) {
            return out;
        }
        BlockPos core = kept.get(0).getBoundingBox().getCenter();
        int cellX = LabyrinthCells.cellOfBlock(core.getX());
        int cellZ = LabyrinthCells.cellOfBlock(core.getZ());
        if (!LabyrinthCells.isLabyrinth(seed, cellX, cellZ)) {
            return out;
        }
        int minX = cellX * LabyrinthCells.CELL_BLOCKS;
        int minZ = cellZ * LabyrinthCells.CELL_BLOCKS;
        int size = LabyrinthCells.CELL_BLOCKS;
        BoundingBox territory = new BoundingBox(minX, ruin.minY(), minZ, minX + size - 1, ruin.maxY(), minZ + size - 1);
        List<BoundingBox> ruins = kept.stream().map(StructurePiece::getBoundingBox).toList();
        List<Doorway> doorways = doorways(kept, templates);
        if (doorways.isEmpty()) {
            return out;
        }
        long band = ruin.deep() ? 1L : 0L;

        for (int[] side : SIDES) {
            int otherX = cellX + side[0];
            int otherZ = cellZ + side[1];
            if (!LabyrinthCells.isLabyrinth(seed, otherX, otherZ)) {
                continue;
            }
            long roll = pairRoll(seed, cellX, cellZ, otherX, otherZ, band);
            if (Math.floorMod(roll, 100L) >= BRIDGE_PERCENT) {
                continue;
            }
            int along = EDGE_MARGIN + (int) Math.floorMod(roll >>> 8, (long) (size - 2 * EDGE_MARGIN));
            int low = ruin.deep() ? LOWER_MEET_MIN : UPPER_MEET_MIN;
            int y = low + (int) Math.floorMod(roll >>> 24, (long) MEET_RANGE);
            BlockPos target;
            Direction.Axis last;
            if (side[0] != 0) {
                target = new BlockPos(side[0] > 0 ? minX + size - 1 : minX, y, minZ + along);
                last = Direction.Axis.X;
            } else {
                target = new BlockPos(minX + along, y, side[1] > 0 ? minZ + size - 1 : minZ);
                last = Direction.Axis.Z;
            }
            Doorway from = nearest(doorways, target);
            out.add(new LabyrinthCorridor(plan(from.pos().relative(from.facing()), target, last), territory, ruins));
        }

        long shaft = LabyrinthCells.mix(seed ^ SHAFT_SALT ^ LabyrinthCells.mix(cellX * 31L + cellZ));
        if (Math.floorMod(shaft, 100L) < SHAFT_PERCENT) {
            int x = minX + EDGE_MARGIN + (int) Math.floorMod(shaft >>> 8, (long) (size - 2 * EDGE_MARGIN));
            int z = minZ + EDGE_MARGIN + (int) Math.floorMod(shaft >>> 24, (long) (size - 2 * EDGE_MARGIN));
            BlockPos target = new BlockPos(x, ruin.deep() ? LOWER_HANDOFF : UPPER_HANDOFF, z);
            Doorway from = nearest(doorways, target);
            out.add(new LabyrinthCorridor(plan(from.pos().relative(from.facing()), target, null), territory, ruins));
        }
        return out;
    }

    // Same value from both cells of a pair: the pair is ordered before hashing.
    private static long pairRoll(long seed, int ax, int az, int bx, int bz, long band) {
        boolean swap = ax > bx || (ax == bx && az > bz);
        long h = seed ^ BRIDGE_SALT ^ band;
        h = LabyrinthCells.mix(h + (swap ? bx : ax) * 0x9E3779B97F4A7C15L);
        h = LabyrinthCells.mix(h + (swap ? bz : az) * 0xC2B2AE3D27D4EB4FL);
        h = LabyrinthCells.mix(h + (swap ? ax : bx) * 0x165667B19E3779F9L);
        return LabyrinthCells.mix(h + (swap ? az : bz) * 0x27D4EB2F165667C5L);
    }

    private static List<Doorway> doorways(List<StructurePiece> kept, StructureTemplateManager templates) {
        List<Doorway> doorways = new ArrayList<>();
        RandomSource random = RandomSource.create(0L);
        for (StructurePiece piece : kept) {
            if (!(piece instanceof PoolElementStructurePiece pool)) {
                continue;
            }
            for (StructureTemplate.JigsawBlockInfo info : pool.getElement()
                .getShuffledJigsawBlocks(templates, pool.getPosition(), pool.getRotation(), random)) {
                Direction facing = JigsawBlock.getFrontFacing(info.state());
                if (facing.getAxis().isHorizontal()) {
                    doorways.add(new Doorway(info.pos(), facing));
                }
            }
        }
        return doorways;
    }

    private static Doorway nearest(List<Doorway> doorways, BlockPos target) {
        Doorway best = doorways.get(0);
        for (Doorway doorway : doorways) {
            if (doorway.pos().distSqr(target) < best.pos().distSqr(target)) {
                best = doorway;
            }
        }
        return best;
    }

    // An L-shaped path that climbs one block per step; with an end axis it ends level on that axis, without one it ends in a shaft.
    private static List<BlockPos> plan(BlockPos start, BlockPos target, Direction.Axis last) {
        List<BlockPos> path = new ArrayList<>();
        int x = start.getX();
        int y = start.getY();
        int z = start.getZ();
        path.add(new BlockPos(x, y, z));
        int flat = last == null ? 0 : FLAT_TAIL;
        int horizontal = Math.abs(target.getX() - x) + Math.abs(target.getZ() - z);
        int excess = Math.abs(target.getY() - y) - Math.max(0, horizontal - flat);
        if (last != null) {
            for (int i = 0; i < excess; i++) {
                y += Integer.signum(target.getY() - y);
                path.add(new BlockPos(x, y, z));
            }
        }
        Direction.Axis first = last == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        for (Direction.Axis axis : new Direction.Axis[] {first, first == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X}) {
            while (axis == Direction.Axis.X ? x != target.getX() : z != target.getZ()) {
                if (axis == Direction.Axis.X) {
                    x += Integer.signum(target.getX() - x);
                } else {
                    z += Integer.signum(target.getZ() - z);
                }
                int remaining = Math.abs(target.getX() - x) + Math.abs(target.getZ() - z);
                if (y != target.getY() && remaining >= flat) {
                    y += Integer.signum(target.getY() - y);
                }
                path.add(new BlockPos(x, y, z));
            }
        }
        while (y != target.getY()) {
            y += Integer.signum(target.getY() - y);
            path.add(new BlockPos(x, y, z));
        }
        return path;
    }
}
