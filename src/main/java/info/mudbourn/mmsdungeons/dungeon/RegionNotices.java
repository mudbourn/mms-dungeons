package info.mudbourn.mmsdungeons.dungeon;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.net.PlayerSounds;
import info.mudbourn.mmsdungeons.net.RegionNoticePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Tells players on screen when they move into another region of the dungeon, up or down. A move soon after a
// notice is held back until the player settles in one region, so hopping along a border shows one notice.
public final class RegionNotices {

    private enum Region {
        SANCTUARY("The Sanctuary Region", 0x7FD8E8, "sanctuary_enter"),
        DEEP("The Deep Region", 0xE0B050, "deep_enter"),
        PERICULUM("The Periculum Region", 0xC0302A, "periculum_enter");

        final String title;
        final int color;
        final SoundEvent sound;

        Region(String title, int color, String sound) {
            this.title = title;
            this.color = color;
            this.sound = SoundEvent.createVariableRangeEvent(MmsDungeons.id(sound));
        }
    }

    private static final int CHECK_INTERVAL = 10;
    // Window after a notice in which a move is held back.
    private static final int BURST_TICKS = 300;
    // Time a held-back player must stay in one region.
    private static final int SETTLE_TICKS = 200;
    private static final Map<UUID, Region> CURRENT = new HashMap<>();
    private static final Map<UUID, Region> SHOWN = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN_AT = new HashMap<>();
    private static final Map<UUID, Integer> HELD_SINCE = new HashMap<>();
    // Side of the square areas a region's notice is shown once per.
    private static final int AREA_SIZE = 500;
    private static final Map<UUID, Map<Region, Long>> SHOWN_IN_AREA = new HashMap<>();
    // Time after arriving in the dungeon before the first notice, while the client loads in.
    private static final int ARRIVAL_TICKS = 60;
    private static final Map<UUID, Integer> ARRIVED_AT = new HashMap<>();

    private RegionNotices() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(RegionNoticePayload.TYPE, RegionNoticePayload.CODEC);
        for (Region region : Region.values()) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, region.sound.location(), region.sound);
        }
        ServerTickEvents.END_SERVER_TICK.register(RegionNotices::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUUID()));
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Region region = regionOf(player);
            if (region == null) {
                forget(id);
                continue;
            }
            Integer arrivedAt = ARRIVED_AT.putIfAbsent(id, now);
            if (arrivedAt == null || now - arrivedAt < ARRIVAL_TICKS || player.isChangingDimension()) {
                continue;
            }
            Region last = CURRENT.put(id, region);
            if (region != last) {
                Integer shownAt = SHOWN_AT.get(id);
                boolean burst = HELD_SINCE.containsKey(id) || shownAt != null && now - shownAt < BURST_TICKS;
                if (burst) {
                    HELD_SINCE.put(id, now);
                } else {
                    show(player, id, region, now);
                }
            } else if (HELD_SINCE.containsKey(id) && now - HELD_SINCE.get(id) >= SETTLE_TICKS) {
                HELD_SINCE.remove(id);
                if (region != SHOWN.get(id)) {
                    show(player, id, region, now);
                }
            }
        }
    }

    private static void show(ServerPlayer player, UUID id, Region region, int now) {
        SHOWN.put(id, region);
        long area = areaOf(player);
        Map<Region, Long> areas = SHOWN_IN_AREA.computeIfAbsent(id, key -> new EnumMap<>(Region.class));
        Long previous = areas.put(region, area);
        if (previous != null && previous == area) {
            return;
        }
        SHOWN_AT.put(id, now);
        announce(player, region);
    }

    private static long areaOf(ServerPlayer player) {
        long areaX = Math.floorDiv(player.getBlockX(), AREA_SIZE);
        long areaZ = Math.floorDiv(player.getBlockZ(), AREA_SIZE);
        return areaX << 32 | areaZ & 0xFFFFFFFFL;
    }

    private static void forget(UUID id) {
        CURRENT.remove(id);
        SHOWN.remove(id);
        SHOWN_AT.remove(id);
        HELD_SINCE.remove(id);
        SHOWN_IN_AREA.remove(id);
        ARRIVED_AT.remove(id);
    }

    private static Region regionOf(ServerPlayer player) {
        if (!DungeonDimension.isDungeon(player.level())) {
            return null;
        }
        int y = player.blockPosition().getY();
        if (y >= DungeonDimension.SANCTUARY_BOTTOM) {
            return Region.SANCTUARY;
        }
        return y <= DungeonDimension.PERICULUM_TOP ? Region.PERICULUM : Region.DEEP;
    }

    private static void announce(ServerPlayer player, Region region) {
        ServerPlayNetworking.send(player, new RegionNoticePayload(region.title, region.color));
        PlayerSounds.play(
            player,
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(region.sound),
            SoundSource.MASTER,
            1.0F,
            1.0F
        );
    }
}
