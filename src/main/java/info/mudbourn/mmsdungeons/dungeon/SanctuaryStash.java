package info.mudbourn.mmsdungeons.dungeon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Keeps the Sanctuary part of stale dungeon chunks while the Deep Region below them generates again.
public final class SanctuaryStash {

    private static final Logger LOG = LoggerFactory.getLogger("mms_dungeons");
    private static final int BOTTOM = DungeonDimension.SANCTUARY_BOTTOM;

    private static final Map<Long, SerializableChunkData> PENDING = new ConcurrentHashMap<>();
    private static final Set<Long> ON_DISK = ConcurrentHashMap.newKeySet();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mms_dungeons sanctuary stash");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile Path folder;

    private SanctuaryStash() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            folder = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("mms_dungeons_sanctuary");
            ON_DISK.clear();
            if (Files.isDirectory(folder)) {
                try (Stream<Path> files = Files.list(folder)) {
                    files.map(SanctuaryStash::keyOf).filter(key -> key != null).forEach(ON_DISK::add);
                } catch (IOException e) {
                    LOG.error("Failed to list {}", folder, e);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.clear();
            ON_DISK.clear();
            folder = null;
        });
    }

    // Stashes a stale chunk's Sanctuary and hands back an empty chunk for the generator to fill.
    public static ProtoChunk rebuild(SerializableChunkData data, ServerLevel level, PoiManager poi, ChunkPos pos) {
        if (data.chunkStatus().isOrAfter(ChunkStatus.FEATURES)) {
            long key = pos.pack();
            PENDING.put(key, data);
            ON_DISK.add(key);
            CompoundTag tag = data.write();
            Path file = fileFor(key);
            if (file != null) {
                IO.execute(() -> write(tag, file));
            }
        }
        List<BlockPos> deepPoi = poi.getInChunk(type -> true, pos, PoiManager.Occupancy.ANY)
            .map(PoiRecord::getPos)
            .filter(at -> at.getY() < BOTTOM)
            .toList();
        deepPoi.forEach(poi::remove);
        DungeonGeneration.claim(pos);
        return new ProtoChunk(pos, UpgradeData.EMPTY, level, data.containerFactory(), null);
    }

    // Puts a stashed Sanctuary back over a rebuilt chunk before its light is worked out.
    public static void restore(ServerLevel level, ChunkAccess chunk) {
        long key = chunk.getPos().pack();
        if (!ON_DISK.contains(key)) {
            return;
        }
        SerializableChunkData data = PENDING.remove(key);
        if (data == null) {
            data = read(level, key);
        }
        ON_DISK.remove(key);
        Path file = fileFor(key);
        if (file != null) {
            IO.execute(() -> delete(file));
        }
        if (data == null) {
            return;
        }
        LevelChunkSection[] sections = chunk.getSections();
        boolean[] restored = new boolean[sections.length];
        for (SerializableChunkData.SectionData stored : data.sectionData()) {
            int index = chunk.getSectionIndexFromSectionY(stored.y());
            if (stored.chunkSection() == null || index < 0 || index >= sections.length || stored.y() * 16 + 15 < BOTTOM) {
                continue;
            }
            LevelChunkSection kept = stored.chunkSection();
            int minY = stored.y() * 16;
            for (int y = 0; y < BOTTOM - minY; y++) {
                copyLayer(sections[index], kept, y, null);
            }
            kept.recalcBlockCounts();
            sections[index] = kept;
            restored[index] = true;
        }
        for (int index = 0; index < sections.length; index++) {
            int minY = chunk.getSectionYFromSectionIndex(index) * 16;
            if (restored[index] || minY + 15 < BOTTOM) {
                continue;
            }
            for (int y = Math.max(0, BOTTOM - minY); y < 16; y++) {
                copyLayer(sections[index], sections[index], y, Blocks.AIR.defaultBlockState());
            }
            sections[index].recalcBlockCounts();
        }
        List<BlockPos> generated = chunk.getBlockEntitiesPos().stream().filter(pos -> pos.getY() >= BOTTOM).toList();
        generated.forEach(chunk::removeBlockEntity);
        for (CompoundTag blockEntity : data.blockEntities()) {
            if (blockEntity.getIntOr("y", Integer.MIN_VALUE) >= BOTTOM) {
                chunk.setBlockEntityNbt(blockEntity);
            }
        }
        chunk.markUnsaved();
    }

    private static void copyLayer(LevelChunkSection from, LevelChunkSection to, int y, BlockState fill) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                to.setBlockState(x, y, z, fill == null ? from.getBlockState(x, y, z) : fill, false);
            }
        }
    }

    private static SerializableChunkData read(ServerLevel level, long key) {
        Path file = fileFor(key);
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            return SerializableChunkData.parse(level, level.palettedContainerFactory(), tag);
        } catch (IOException | RuntimeException e) {
            LOG.error("Failed to read the stashed Sanctuary at {}", file, e);
            return null;
        }
    }

    private static void write(CompoundTag tag, Path file) {
        try {
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            LOG.error("Failed to stash the Sanctuary at {}", file, e);
        }
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.error("Failed to delete {}", file, e);
        }
    }

    private static Path fileFor(long key) {
        Path root = folder;
        ChunkPos pos = ChunkPos.unpack(key);
        return root == null ? null : root.resolve("c." + pos.x() + "." + pos.z() + ".dat");
    }

    private static Long keyOf(Path file) {
        String[] parts = file.getFileName().toString().split("\\.");
        if (parts.length != 4 || !parts[0].equals("c")) {
            return null;
        }
        try {
            return ChunkPos.pack(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
