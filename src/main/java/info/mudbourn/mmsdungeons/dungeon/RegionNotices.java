package info.mudbourn.mmsdungeons.dungeon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// Tells players on screen when they go down into a deeper region of the dungeon.
public final class RegionNotices {

    private enum Region {
        SANCTUARY("The Sanctuary Region", "Safe ground under the roof", ChatFormatting.AQUA),
        DEEP("The Deep Region", "Notorious for its shifting tides", ChatFormatting.GOLD),
        PERICULUM("The Periculum Region", "Nobody is safe here. PvP is on.", ChatFormatting.DARK_RED);

        final String title;
        final String subtitle;
        final ChatFormatting color;

        Region(String title, String subtitle, ChatFormatting color) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
        }
    }

    private static final int CHECK_INTERVAL = 10;
    // Thirty seconds between notices.
    private static final int COOLDOWN_TICKS = 600;
    private static final Map<UUID, Region> CURRENT = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN_AT = new HashMap<>();

    private RegionNotices() {
    }

    public static void register() {
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
            boolean deeper = last == null || region.ordinal() > last.ordinal();
            boolean cooling = SHOWN_AT.containsKey(id) && now - SHOWN_AT.get(id) < COOLDOWN_TICKS;
            if (deeper && !cooling) {
                SHOWN_AT.put(id, now);
                announce(player, region);
            }
        }
    }

    private static void forget(UUID id) {
        CURRENT.remove(id);
        SHOWN_AT.remove(id);
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
    }
}
