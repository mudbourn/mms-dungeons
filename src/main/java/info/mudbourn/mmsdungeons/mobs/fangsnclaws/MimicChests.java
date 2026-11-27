package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DddPlacement;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Mimic;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// Swaps a share of the chests in Deadly Deadly Dungeon structures for Mimics the first time each dungeon chunk
// loads. A saved mark on the chunk keeps it to once, so chunks generated before this ran get their turn too.
public final class MimicChests {

    private static final float SHARE = 0.2f;

    private static AttachmentType<Boolean> SWAPPED;

    private MimicChests() {
    }

    public static void register() {
        SWAPPED = AttachmentRegistry.<Boolean>builder()
            .persistent(Codec.BOOL)
            .buildAndRegister(MmsDungeons.id("mimics_swapped"));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (DungeonDimension.isDungeon(level) && !chunk.hasAttached(SWAPPED)) {
                level.getServer().execute(() -> swap(level, chunk));
            }
        });
    }

    private static void swap(ServerLevel level, LevelChunk chunk) {
        if (level.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z()) != chunk) {
            return;
        }
        if (chunk.hasAttached(SWAPPED)) {
            return;
        }
        chunk.setAttached(SWAPPED, true);
        chunk.markUnsaved();
        List<StructureStart> starts = new ArrayList<>();
        for (Structure structure : chunk.getAllReferences().keySet()) {
            Identifier key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
            if (key != null && DddPlacement.isPlacedDdd(key)) {
                starts.addAll(level.structureManager().startsForStructure(chunk.getPos().x(), chunk.getPos().z(), structure));
            }
        }
        if (starts.isEmpty()) {
            return;
        }
        for (BlockEntity entity : new ArrayList<>(chunk.getBlockEntities().values())) {
            BlockPos pos = entity.getBlockPos();
            if (entity instanceof ChestBlockEntity && level.getRandom().nextFloat() < SHARE && inside(starts, pos)) {
                replace(level, pos);
            }
        }
    }

    private static boolean inside(List<StructureStart> starts, BlockPos pos) {
        return starts.stream().anyMatch(start -> start.getBoundingBox().isInside(pos));
    }

    private static void replace(ServerLevel level, BlockPos pos) {
        Mimic mimic = FangsNClawsMobs.MIMIC.create(level, EntitySpawnReason.STRUCTURE);
        if (mimic == null) {
            return;
        }
        mimic.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        mimic.consumeChest(pos);
        level.addFreshEntity(mimic);
    }
}
