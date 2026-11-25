package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Counts how many times the Deep Region has been rebuilt.
public final class DungeonGeneration extends SavedData {

    public static final Codec<DungeonGeneration> CODEC = Codec.INT.xmap(DungeonGeneration::new, data -> data.generation);

    public static final SavedDataType<DungeonGeneration> TYPE = new SavedDataType<>(
        MmsDungeons.id("dungeon_generation"),
        DungeonGeneration::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    // The loaded count, read by world generation threads.
    private static volatile int current;

    private int generation;

    public DungeonGeneration() {
    }

    private DungeonGeneration(int generation) {
        this.generation = generation;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> current = get(server).generation);
    }

    public static int current() {
        return current;
    }

    // Moves to the next generation; the Deep Region reset calls this before rebuilding.
    public static void advance(MinecraftServer server) {
        DungeonGeneration data = get(server);
        data.generation++;
        data.setDirty();
        current = data.generation;
    }

    private static DungeonGeneration get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
