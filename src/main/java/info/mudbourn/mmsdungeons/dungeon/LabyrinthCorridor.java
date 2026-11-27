package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

// A 3 wide, 4 tall stone corridor from a labyrinth doorway to the edge of its own cell or band, where the neighbour's half meets it.
public final class LabyrinthCorridor extends StructurePiece {

    public static StructurePieceType TYPE;

    private static final int HEIGHT = 4;
    private static final int LANTERN_EVERY = 10;

    private final long[] path;
    private final int[] territory;
    private final int[] ruinBoxes;

    public LabyrinthCorridor(List<BlockPos> path, BoundingBox territory, List<BoundingBox> ruins) {
        super(TYPE, 0, boundsOf(path, territory));
        this.path = path.stream().mapToLong(BlockPos::asLong).toArray();
        this.territory = pack(List.of(territory));
        this.ruinBoxes = pack(ruins.stream().filter(box -> box.intersects(this.boundingBox)).toList());
    }

    public LabyrinthCorridor(CompoundTag tag) {
        super(TYPE, tag);
        this.path = tag.getLongArray("Path").orElse(new long[0]);
        this.territory = tag.getIntArray("Territory").orElse(new int[6]);
        this.ruinBoxes = tag.getIntArray("Ruins").orElse(new int[0]);
    }

    public static void register() {
        TYPE = Registry.register(
            BuiltInRegistries.STRUCTURE_PIECE,
            MmsDungeons.id("labyrinth_corridor"),
            (StructurePieceType.ContextlessType) LabyrinthCorridor::new
        );
    }

    private static BoundingBox boundsOf(List<BlockPos> path, BoundingBox territory) {
        BoundingBox box = BoundingBox.encapsulatingPositions(path).orElseThrow();
        BoundingBox grown = new BoundingBox(box.minX() - 2, box.minY() - 1, box.minZ() - 2, box.maxX() + 2, box.maxY() + HEIGHT, box.maxZ() + 2);
        return new BoundingBox(
            Math.max(grown.minX(), territory.minX()),
            Math.max(grown.minY(), territory.minY()),
            Math.max(grown.minZ(), territory.minZ()),
            Math.min(grown.maxX(), territory.maxX()),
            Math.min(grown.maxY(), territory.maxY()),
            Math.min(grown.maxZ(), territory.maxZ())
        );
    }

    private static int[] pack(List<BoundingBox> boxes) {
        int[] out = new int[boxes.size() * 6];
        for (int i = 0; i < boxes.size(); i++) {
            BoundingBox box = boxes.get(i);
            int[] values = {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()};
            System.arraycopy(values, 0, out, i * 6, 6);
        }
        return out;
    }

    private static List<BoundingBox> unpack(int[] values) {
        List<BoundingBox> boxes = new ArrayList<>();
        for (int i = 0; i + 5 < values.length; i += 6) {
            boxes.add(new BoundingBox(values[i], values[i + 1], values[i + 2], values[i + 3], values[i + 4], values[i + 5]));
        }
        return boxes;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLongArray("Path", this.path);
        tag.putIntArray("Territory", this.territory);
        tag.putIntArray("Ruins", this.ruinBoxes);
    }

    @Override
    public void postProcess(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator generator,
        RandomSource random,
        BoundingBox chunkBB,
        ChunkPos chunkPos,
        BlockPos referencePos
    ) {
        BoundingBox own = unpack(this.territory).get(0);
        BoxIndex ruins = new BoxIndex(unpack(this.ruinBoxes));
        DddRuin ruin = DddRuin.placing();
        DddMaterial material = DddRuin.placingMaterial();
        if (material == null) {
            material = ruin != null ? DddMaterial.of(ruin) : DddMaterial.STONE;
        }

        LongOpenHashSet interior = new LongOpenHashSet();
        LongOpenHashSet shaft = new LongOpenHashSet();
        // Only path cells whose 3 by 3 cross section, plus its wall, reaches into this chunk.
        BoundingBox near = chunkBB.inflatedBy(2);
        for (int i = 0; i < this.path.length; i++) {
            BlockPos center = BlockPos.of(this.path[i]);
            if (center.getX() < near.minX() || center.getX() > near.maxX() || center.getZ() < near.minZ() || center.getZ() > near.maxZ()) {
                continue;
            }
            boolean vertical = sameColumn(i - 1, center) || sameColumn(i + 1, center);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy < HEIGHT; dy++) {
                        long block = BlockPos.asLong(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        interior.add(block);
                        if (vertical) {
                            shaft.add(block);
                        }
                    }
                }
            }
        }

        BlockState wall = material.apply(Blocks.STONE_BRICKS.defaultBlockState());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long packed : interior) {
            pos.set(packed);
            if (writable(pos, chunkBB, own, ruins)) {
                level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
            }
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                // Reads stay inside this chunk: the wall is checked for room before the world is read.
                if (interior.contains(next.asLong()) || !writable(next, chunkBB, own, ruins)) {
                    continue;
                }
                if (shaft.contains(packed) || !level.getBlockState(next).isAir()) {
                    level.setBlock(next, wall, 2);
                }
            }
        }

        for (int i = 1; i < this.path.length; i++) {
            BlockPos from = BlockPos.of(this.path[i - 1]);
            BlockPos to = BlockPos.of(this.path[i]);
            if (from.getX() == to.getX() && from.getZ() == to.getZ()) {
                ladder(level, from, interior, chunkBB, own, ruins);
                ladder(level, to, interior, chunkBB, own, ruins);
            } else if (from.getY() != to.getY()) {
                stairs(level, from, to, material, chunkBB, own, ruins);
            }
        }

        for (int i = LANTERN_EVERY / 2; i + 1 < this.path.length; i += LANTERN_EVERY) {
            BlockPos center = BlockPos.of(this.path[i]);
            if (center.getY() != BlockPos.of(this.path[i + 1]).getY() || center.getY() != BlockPos.of(this.path[i - 1]).getY()) {
                continue;
            }
            BlockPos lamp = center.above(HEIGHT - 1);
            if (writable(lamp, chunkBB, own, ruins) && level.getBlockState(lamp).isAir()) {
                level.setBlock(lamp, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
            }
        }
    }

    private static boolean writable(BlockPos pos, BoundingBox chunkBB, BoundingBox own, BoxIndex ruins) {
        return chunkBB.isInside(pos) && own.isInside(pos) && !ruins.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    // A row of stairs across the corridor on the upper step's floor, climbing toward it.
    private static void stairs(
        WorldGenLevel level,
        BlockPos from,
        BlockPos to,
        DddMaterial material,
        BoundingBox chunkBB,
        BoundingBox own,
        BoxIndex ruins
    ) {
        BlockPos low = from.getY() < to.getY() ? from : to;
        BlockPos high = low == from ? to : from;
        Direction climb = Direction.getApproximateNearest(high.getX() - low.getX(), 0, high.getZ() - low.getZ());
        BlockState step = material.apply(Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, climb));
        Direction across = climb.getClockWise();
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos pos = new BlockPos(high.getX(), low.getY(), high.getZ()).relative(across, offset);
            if (writable(pos, chunkBB, own, ruins)) {
                level.setBlock(pos, step, 2);
            }
        }
    }

    private boolean sameColumn(int index, BlockPos center) {
        if (index < 0 || index >= this.path.length) {
            return false;
        }
        BlockPos other = BlockPos.of(this.path[index]);
        return other.getX() == center.getX() && other.getZ() == center.getZ();
    }

    // A ladder step only where the block behind it is, or is about to become, the shaft's own wall or other solid rock.
    private static void ladder(
        WorldGenLevel level,
        BlockPos center,
        LongOpenHashSet interior,
        BoundingBox chunkBB,
        BoundingBox own,
        BoxIndex ruins
    ) {
        for (int dy = 0; dy < HEIGHT; dy++) {
            BlockPos pos = center.offset(0, dy, -1);
            BlockPos behind = pos.north();
            boolean wallBuilt = !interior.contains(behind.asLong())
                && own.isInside(behind)
                && !ruins.contains(behind.getX(), behind.getY(), behind.getZ());
            if (!writable(pos, chunkBB, own, ruins)) {
                continue;
            }
            boolean backed = wallBuilt || chunkBB.isInside(behind) && level.getBlockState(behind).isFaceSturdy(level, behind, Direction.SOUTH);
            if (backed && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 2);
            }
        }
    }
}
