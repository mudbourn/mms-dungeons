package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.Arrays;
import java.util.stream.LongStream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Counts Deep Region rebuilds and tracks which dungeon chunks were saved since the last one.
public final class DungeonGeneration extends SavedData {

    private static final Codec<DungeonGeneration> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("generation").forGetter(data -> data.generation),
        Codec.LONG.optionalFieldOf("last_rebuild", 0L).forGetter(data -> data.lastRebuild),
        Codec.LONG_STREAM.optionalFieldOf("fresh", LongStream.empty()).forGetter(data -> Arrays.stream(data.fresh.toLongArray()))
    ).apply(instance, (generation, lastRebuild, fresh) -> new DungeonGeneration(generation, lastRebuild, fresh.toArray())));

    public static final Codec<DungeonGeneration> CODEC = Codec.withAlternative(
        FULL_CODEC,
        Codec.INT.xmap(generation -> new DungeonGeneration(generation, 0L, new long[0]), data -> data.generation)
    );

    public static final SavedDataType<DungeonGeneration> TYPE = new SavedDataType<>(
        MmsDungeons.id("dungeon_generation"),
        DungeonGeneration::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    private static final long SEED_SALT = 0x9E3779B97F4A7C15L;

    // The loaded data and count, read by world generation and chunk loading threads.
    private static volatile DungeonGeneration loaded;
    private static volatile int current;

    // Chunks still loaded when the last rebuild ran; they never count as saved for the new generation.
    private static final LongSet PINNED = LongSets.synchronize(new LongOpenHashSet());

    private int generation;
    private long lastRebuild;
    private final LongSet fresh = LongSets.synchronize(new LongOpenHashSet());

    public DungeonGeneration() {
    }

    private DungeonGeneration(int generation, long lastRebuild, long[] fresh) {
        this.generation = generation;
        this.lastRebuild = lastRebuild;
        this.fresh.addAll(LongSet.of(fresh));
    }

    public static void register() {
        ServerLevelEvents.LOAD.register((server, level) -> {
            if (level.dimension() == Level.OVERWORLD) {
                DungeonGeneration data = get(server);
                loaded = data;
                current = data.generation;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            loaded = null;
            current = 0;
            PINNED.clear();
        });
    }

    public static int current() {
        return current;
    }

    // The world seed mixed with the generation, so every rebuild rolls a new layout; generation 0 keeps the world seed.
    public static long layoutSeed(long worldSeed) {
        int generation = current;
        if (generation == 0) {
            return worldSeed;
        }
        return LabyrinthCells.mix(worldSeed ^ generation * SEED_SALT);
    }

    // Whether a stored dungeon chunk predates the current generation and must be rebuilt below the Sanctuary.
    public static boolean isStale(ChunkPos pos) {
        DungeonGeneration data = loaded;
        return data != null && data.generation > 0 && !data.fresh.contains(pos.pack());
    }

    // Records that a stale chunk is being rebuilt, so later loads keep it.
    public static void claim(ChunkPos pos) {
        DungeonGeneration data = loaded;
        if (data != null) {
            PINNED.remove(pos.pack());
            data.fresh.add(pos.pack());
            data.setDirty();
        }
    }

    // Records that a chunk is being saved with current content.
    public static void markSaved(ChunkPos pos) {
        DungeonGeneration data = loaded;
        if (data != null && !PINNED.contains(pos.pack()) && data.fresh.add(pos.pack())) {
            data.setDirty();
        }
    }

    public static long lastRebuild(MinecraftServer server) {
        DungeonGeneration data = get(server);
        if (data.lastRebuild == 0L) {
            data.lastRebuild = System.currentTimeMillis();
            data.setDirty();
        }
        return data.lastRebuild;
    }

    // Moves to the next generation; every stored dungeon chunk becomes stale, and the given loaded chunks stay stale.
    public static void advance(MinecraftServer server, LongSet stillLoaded) {
        DungeonGeneration data = get(server);
        data.generation++;
        data.lastRebuild = System.currentTimeMillis();
        data.fresh.clear();
        data.setDirty();
        PINNED.clear();
        PINNED.addAll(stillLoaded);
        current = data.generation;
    }

    private static DungeonGeneration get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
