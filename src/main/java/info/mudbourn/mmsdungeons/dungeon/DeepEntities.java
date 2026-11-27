package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

// Stamps dungeon entities and players with their Deep Region generation and clears out what a rebuild left below the Sanctuary.
public final class DeepEntities {

    private static final int STAMP_INTERVAL = 20;
    private static final Component AWAY = Component.literal("The Deep Region rebuilt while you were away. You wake up on the surface.");

    public static AttachmentType<Integer> GENERATION;

    private static final List<Entity> LEFT_BEHIND = new ArrayList<>();

    private DeepEntities() {
    }

    public static void register() {
        GENERATION = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT)
            .buildAndRegister(MmsDungeons.id("dungeon_generation"));
        ServerEntityEvents.ENTITY_LOAD.register(DeepEntities::onLoad);
        ServerTickEvents.END_SERVER_TICK.register(DeepEntities::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
    }

    // Gives a dungeon entity read from disk without a stamp the first generation.
    public static void stampLegacy(Entity entity) {
        if (!(entity instanceof Player) && DungeonDimension.isDungeon(entity.level()) && !entity.hasAttached(GENERATION)) {
            entity.setAttached(GENERATION, 0);
        }
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (entity instanceof Player || !DungeonDimension.isDungeon(level)) {
            return;
        }
        Integer stamp = entity.getAttached(GENERATION);
        int now = DungeonGeneration.current();
        if (stamp != null && stamp < now && entity.getY() < DungeonDimension.SANCTUARY_BOTTOM) {
            LEFT_BEHIND.add(entity);
        } else if (stamp == null || stamp != now) {
            entity.setAttached(GENERATION, now);
        }
    }

    private static void tick(MinecraftServer server) {
        for (Entity entity : LEFT_BEHIND) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        LEFT_BEHIND.clear();
        if (server.getTickCount() % STAMP_INTERVAL != 0) {
            return;
        }
        int now = DungeonGeneration.current();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Integer stamp = player.getAttached(GENERATION);
            if (DungeonDimension.isDungeon(player.level()) && (stamp == null || stamp != now)) {
                player.setAttached(GENERATION, now);
            }
        }
    }

    private static void onJoin(ServerPlayer player) {
        if (!DungeonDimension.isDungeon(player.level())) {
            return;
        }
        Integer stamp = player.getAttached(GENERATION);
        boolean stale = (stamp == null ? 0 : stamp) < DungeonGeneration.current()
            && player.getY() < DungeonDimension.SANCTUARY_BOTTOM;
        if (stale || DeepRebuild.isClosed()) {
            DeepRebuild.evacuate(player);
            player.sendSystemMessage(stale ? AWAY : DeepRebuild.CLOSED);
        }
    }
}
