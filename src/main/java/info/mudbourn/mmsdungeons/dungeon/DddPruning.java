package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

// Cuts dead ends out of Deadly Deadly Dungeon ruins and opens up to four side tunnels as entrances.
public final class DddPruning {

    private enum Kind {
        START,
        HALL,
        ROOM,
        ATTACHMENT,
        SURFACE,
        REMOVED
    }

    private static final String[] ATTACHMENT_FOLDERS = {"decoration", "feature", "spawner", "cleaner", "loot"};
    private static final String[] ROADBLOCKS = {"dungeon_obstructed_hall", "fallen_hall", "floded_hall", "death_stairs"};

    private static final int ENCAPSULATE_REACH = 12;
    private static final int MAX_TUNNEL = 24;
    private static final int[] DROP_INTERVALS = {0, 3, 2};

    private static final int CORE_HEADROOM = 8;
    private static final int TUNNEL_SPACING = 24;

    private DddPruning() {
    }

    // Returns the surviving pieces inside the ruin's Y band with a trimmed tower and entrance tunnels, or an empty list when no entrance reaches cave air.
    public static List<StructurePiece> prune(
        List<StructurePiece> pieces,
        DddRuin ruin,
        ChunkGenerator generator,
        RandomState randomState,
        StructureTemplateManager templates,
        LevelHeightAccessor heights
    ) {
        int count = pieces.size();
        if (count == 0 || !(pieces.get(0) instanceof PoolElementStructurePiece)) {
            return pieces;
        }
        if (!inBand(pieces.get(0), ruin)) {
            return List.of();
        }
        Kind[] kinds = new Kind[count];
        for (int i = 0; i < count; i++) {
            kinds[i] = i == 0 ? Kind.START : inBand(pieces.get(i), ruin) ? classify(pieces.get(i)) : Kind.REMOVED;
        }
        boolean[][] links = links(pieces);
        boolean[] alive = new boolean[count];
        for (int i = 0; i < count; i++) {
            alive[i] = kinds[i] != Kind.REMOVED;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < count; i++) {
                if (alive[i] && kinds[i] == Kind.HALL && structuralNeighbours(i, links, kinds, alive) <= 1) {
                    alive[i] = false;
                    changed = true;
                }
            }
        }

        boolean[] reached = reachable(links, alive);
        capCore(pieces, templates);
        boolean towered = trimTowers(pieces, kinds, reached, randomState, heights);
        List<DddEntranceTunnel> tunnels = findTunnels(pieces, kinds, reached, ruin, generator, randomState, templates, heights);
        if (!towered && tunnels.isEmpty()) {
            return List.of();
        }
        List<StructurePiece> kept = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (reached[i]) {
                kept.add(pieces.get(i));
            }
        }
        kept.addAll(tunnels);
        return kept;
    }

    // Trims each reachable tower to the cave air it meets and drops the ones that never meet any.
    // Cuts the core's spire off a few blocks above its highest side doorway.
    private static void capCore(List<StructurePiece> pieces, StructureTemplateManager templates) {
        if (!(pieces.get(0) instanceof PoolElementStructurePiece core)) {
            return;
        }
        int highest = Integer.MIN_VALUE;
        for (StructureTemplate.JigsawBlockInfo doorway : core.getElement()
            .getShuffledJigsawBlocks(templates, core.getPosition(), core.getRotation(), RandomSource.create(0L))) {
            if (JigsawBlock.getFrontFacing(doorway.state()).getAxis().isHorizontal()) {
                highest = Math.max(highest, doorway.pos().getY());
            }
        }
        if (highest != Integer.MIN_VALUE) {
            ((TowerPiece) (Object) core).mmsDungeons$setTowerTop(highest + CORE_HEADROOM);
        }
    }

    private static boolean inBand(StructurePiece piece, DddRuin ruin) {
        BoundingBox box = piece.getBoundingBox();
        return box.minY() >= ruin.minY() && box.maxY() <= ruin.maxY();
    }

    private static boolean trimTowers(
        List<StructurePiece> pieces,
        Kind[] kinds,
        boolean[] reached,
        RandomState randomState,
        LevelHeightAccessor heights
    ) {
        List<BoundingBox> reachBoxes = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            if (reached[i] && kinds[i] != Kind.SURFACE) {
                reachBoxes.add(pieces.get(i).getBoundingBox().inflatedBy(ENCAPSULATE_REACH));
            }
        }
        boolean towered = false;
        for (int i = 0; i < pieces.size(); i++) {
            if (!reached[i] || kinds[i] != Kind.SURFACE) {
                continue;
            }
            int top = DddTowerTrim.trimTop(pieces.get(i).getBoundingBox(), reachBoxes, randomState, heights);
            if (top == TowerPiece.UNSET) {
                reached[i] = false;
            } else {
                ((TowerPiece) (Object) pieces.get(i)).mmsDungeons$setTowerTop(top);
                towered = true;
            }
        }
        return towered;
    }

    private static Kind classify(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece pool) || !(pool.getElement() instanceof SinglePoolElement single)) {
            return Kind.ATTACHMENT;
        }
        String path = single.getTemplateLocation().getPath();
        for (String roadblock : ROADBLOCKS) {
            if (path.contains(roadblock)) {
                return Kind.REMOVED;
            }
        }
        if (path.contains("/terminals")) {
            return Kind.REMOVED;
        }
        if (path.contains("/surfaces")) {
            return Kind.REMOVED;
        }
        for (String folder : ATTACHMENT_FOLDERS) {
            if (path.contains(folder)) {
                return Kind.ATTACHMENT;
            }
        }
        return path.contains("/halls") ? Kind.HALL : Kind.ROOM;
    }

    private static int structuralNeighbours(int index, boolean[][] links, Kind[] kinds, boolean[] alive) {
        int total = 0;
        for (int other = 0; other < links.length; other++) {
            if (links[index][other] && alive[other] && kinds[other] != Kind.ATTACHMENT && kinds[other] != Kind.SURFACE) {
                total++;
            }
        }
        return total;
    }

    private static boolean[] reachable(boolean[][] links, boolean[] alive) {
        boolean[] seen = new boolean[alive.length];
        Deque<Integer> queue = new ArrayDeque<>();
        seen[0] = true;
        queue.add(0);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (int other = 0; other < alive.length; other++) {
                if (links[current][other] && alive[other] && !seen[other]) {
                    seen[other] = true;
                    queue.add(other);
                }
            }
        }
        return seen;
    }

    private static boolean[][] links(List<StructurePiece> pieces) {
        int count = pieces.size();
        boolean anyJunctions = false;
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece pool && !pool.getJunctions().isEmpty()) {
                anyJunctions = true;
            }
        }
        boolean[][] links = new boolean[count][count];
        for (int a = 0; a < count; a++) {
            for (int b = a + 1; b < count; b++) {
                boolean linked = touching(pieces.get(a), pieces.get(b))
                    && (!anyJunctions || junctionPoints(pieces.get(a), pieces.get(b)) || junctionPoints(pieces.get(b), pieces.get(a)));
                links[a][b] = linked;
                links[b][a] = linked;
            }
        }
        return links;
    }

    private static boolean touching(StructurePiece a, StructurePiece b) {
        return a.getBoundingBox().inflatedBy(1).intersects(b.getBoundingBox());
    }

    private static boolean junctionPoints(StructurePiece from, StructurePiece to) {
        if (!(from instanceof PoolElementStructurePiece pool)) {
            return false;
        }
        BoundingBox box = to.getBoundingBox().inflatedBy(1);
        for (JigsawJunction junction : pool.getJunctions()) {
            boolean inside = junction.getSourceX() >= box.minX() && junction.getSourceX() <= box.maxX()
                && junction.getSourceZ() >= box.minZ() && junction.getSourceZ() <= box.maxZ();
            if (inside) {
                return true;
            }
        }
        return false;
    }

    // Short tunnels, a few per ruin, from doorways spread around the ruin out to cave air.
    private static List<DddEntranceTunnel> findTunnels(
        List<StructurePiece> pieces,
        Kind[] kinds,
        boolean[] alive,
        DddRuin ruin,
        ChunkGenerator generator,
        RandomState randomState,
        StructureTemplateManager templates,
        LevelHeightAccessor heights
    ) {
        List<BoundingBox> ruins = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            if (alive[i]) {
                ruins.add(pieces.get(i).getBoundingBox());
            }
        }
        List<BoundingBox> reachBoxes = ruins.stream().map(box -> box.inflatedBy(ENCAPSULATE_REACH)).toList();

        RandomSource random = RandomSource.create(0L);
        List<List<BlockPos>> candidates = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            if (!alive[i] || kinds[i] == Kind.ATTACHMENT || !(pieces.get(i) instanceof PoolElementStructurePiece pool)) {
                continue;
            }
            List<StructureTemplate.JigsawBlockInfo> doorways = pool.getElement()
                .getShuffledJigsawBlocks(templates, pool.getPosition(), pool.getRotation(), random);
            for (StructureTemplate.JigsawBlockInfo doorway : doorways) {
                Direction facing = JigsawBlock.getFrontFacing(doorway.state());
                if (facing.getAxis().isVertical()) {
                    continue;
                }
                List<BlockPos> shortest = null;
                for (int dropEvery : DROP_INTERVALS) {
                    List<BlockPos> path = walk(doorway.pos(), facing, dropEvery, ruin, reachBoxes, generator, randomState, heights);
                    if (path != null && (shortest == null || path.size() < shortest.size())) {
                        shortest = path;
                    }
                }
                if (shortest != null) {
                    candidates.add(shortest);
                }
            }
        }
        candidates.sort(java.util.Comparator.comparingInt(List::size));
        List<BlockPos> doors = new ArrayList<>();
        List<DddEntranceTunnel> tunnels = new ArrayList<>();
        for (List<BlockPos> path : candidates) {
            BlockPos door = path.get(0);
            if (tunnels.size() >= ruin.maxTunnels() || doors.stream().anyMatch(other -> other.distSqr(door) < TUNNEL_SPACING * TUNNEL_SPACING)) {
                continue;
            }
            doors.add(door);
            tunnels.add(new DddEntranceTunnel(path, ruins));
        }
        return tunnels;
    }

    // Steps outward from a doorway until two stacked air blocks lie outside every ruin reach box.
    private static List<BlockPos> walk(
        BlockPos doorway,
        Direction facing,
        int dropEvery,
        DddRuin ruin,
        List<BoundingBox> reachBoxes,
        ChunkGenerator generator,
        RandomState randomState,
        LevelHeightAccessor heights
    ) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos pos = doorway.relative(facing);
        for (int step = 0; step <= MAX_TUNNEL; step++) {
            path.add(pos);
            if (!insideAny(reachBoxes, pos) && openAir(pos, generator, randomState, heights)) {
                return path;
            }
            pos = pos.relative(facing);
            if (dropEvery > 0 && (step + 1) % dropEvery == 0) {
                pos = pos.below();
            }
            if (pos.getY() < ruin.minY() || pos.getY() + 3 > ruin.maxY()) {
                return null;
            }
        }
        return null;
    }

    private static boolean insideAny(List<BoundingBox> boxes, BlockPos pos) {
        return insideAny(boxes, pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean insideAny(List<BoundingBox> boxes, int x, int y, int z) {
        for (BoundingBox box : boxes) {
            if (box.isInside(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static boolean openAir(BlockPos pos, ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights) {
        if (pos.getY() < heights.getMinY() || pos.getY() + 1 >= heights.getMaxY()) {
            return false;
        }
        DensityFunction density = TerrainAir.finalDensity(randomState);
        return TerrainAir.isAir(randomState, density, pos.getX(), pos.getY(), pos.getZ())
            && TerrainAir.isAir(randomState, density, pos.getX(), pos.getY() + 1, pos.getZ());
    }
}
