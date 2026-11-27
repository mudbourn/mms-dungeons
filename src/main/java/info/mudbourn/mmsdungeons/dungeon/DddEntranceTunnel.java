package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

// Clears a 3 by 3 passage of air from a ruin doorway out through its stone shell.
public final class DddEntranceTunnel extends StructurePiece {

    public static StructurePieceType TYPE;

    private final long[] path;
    private final int[] ruinBoxes;

    public DddEntranceTunnel(List<BlockPos> path, List<BoundingBox> ruinBoxes) {
        super(TYPE, 0, boundsOf(path));
        this.path = path.stream().mapToLong(BlockPos::asLong).toArray();
        List<BoundingBox> near = ruinBoxes.stream().filter(box -> box.intersects(this.boundingBox)).toList();
        this.ruinBoxes = new int[near.size() * 6];
        for (int i = 0; i < near.size(); i++) {
            BoundingBox box = near.get(i);
            int[] values = {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()};
            System.arraycopy(values, 0, this.ruinBoxes, i * 6, 6);
        }
    }

    public DddEntranceTunnel(CompoundTag tag) {
        super(TYPE, tag);
        this.path = tag.getLongArray("Path").orElse(new long[0]);
        this.ruinBoxes = tag.getIntArray("Ruins").orElse(new int[0]);
    }

    public static void register() {
        TYPE = Registry.register(
            BuiltInRegistries.STRUCTURE_PIECE,
            MmsDungeons.id("ddd_entrance_tunnel"),
            (StructurePieceType.ContextlessType) DddEntranceTunnel::new
        );
    }

    private static BoundingBox boundsOf(List<BlockPos> path) {
        BoundingBox box = BoundingBox.encapsulatingPositions(path).orElseThrow();
        return new BoundingBox(box.minX() - 1, box.minY(), box.minZ() - 1, box.maxX() + 1, box.maxY() + 2, box.maxZ() + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLongArray("Path", this.path);
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
        List<BoundingBox> ruins = new ArrayList<>();
        for (int i = 0; i + 5 < this.ruinBoxes.length; i += 6) {
            ruins.add(new BoundingBox(
                this.ruinBoxes[i],
                this.ruinBoxes[i + 1],
                this.ruinBoxes[i + 2],
                this.ruinBoxes[i + 3],
                this.ruinBoxes[i + 4],
                this.ruinBoxes[i + 5]
            ));
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long packed : this.path) {
            BlockPos center = BlockPos.of(packed);
            if (!chunkBB.intersects(center.getX() - 1, center.getZ() - 1, center.getX() + 1, center.getZ() + 1)) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (chunkBB.isInside(pos) && !insideAny(ruins, pos) && !level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private static boolean insideAny(List<BoundingBox> boxes, BlockPos pos) {
        for (BoundingBox box : boxes) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
