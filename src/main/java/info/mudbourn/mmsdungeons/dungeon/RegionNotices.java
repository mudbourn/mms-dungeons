package info.mudbourn.mmsdungeons.dungeon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.net.PlayerSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Tells players on screen when they move into another region of the dungeon, up or down. A move soon after a
// notice is held back until the player settles in one region, so hopping along a border shows one notice.
public final class RegionNotices {

    private enum Region {
        SANCTUARY(
            "The Sanctuary Region",
            "Safe zone where the mercy of the dungeon extends.",
            ChatFormatting.AQUA,
            "sanctuary_enter"
        ),
        DEEP("The Deep Region", "Labyrinth of trickery where maps mean nothing", ChatFormatting.GOLD, "deep_enter"),
        PERICULUM(
            "The Periculum Region",
            "Deadly depths where friends may turn foe",
            ChatFormatting.DARK_RED,
            "periculum_enter"
        );

        final String title;
        final String subtitle;
        final ChatFormatting color;
        final SoundEvent sound;

        Region(String title, String subtitle, ChatFormatting color, String sound) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.sound = SoundEvent.createVariableRangeEvent(MmsDungeons.id(sound));
        }
    }

    private static final int CHECK_INTERVAL = 10;
    // A move within five seconds of a notice is held back...
    private static final int BURST_TICKS = 100;
    // ...until the player has stayed in one region for ten seconds.
    private static final int SETTLE_TICKS = 200;
    private static final Map<UUID, Region> CURRENT = new HashMap<>();
    private static final Map<UUID, Region> SHOWN = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN_AT = new HashMap<>();
    private static final Map<UUID, Integer> HELD_SINCE = new HashMap<>();

    private RegionNotices() {
    }

    public static void register() {
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
        SHOWN_AT.put(id, now);
        announce(player, region);
    }

    private static void forget(UUID id) {
        CURRENT.remove(id);
        SHOWN.remove(id);
        SHOWN_AT.remove(id);
        HELD_SINCE.remove(id);
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
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(region.subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(region.title).withStyle(region.color)));
        PlayerSounds.play(
            player,
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(region.sound),
            SoundSource.MASTER,
            1.0F,
            1.0F
        );
    }
}
