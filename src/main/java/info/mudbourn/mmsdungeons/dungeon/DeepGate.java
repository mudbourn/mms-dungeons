package info.mudbourn.mmsdungeons.dungeon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// The portal lets in iron-level players, but only those who meet the original bar may go below the Sanctuary Region.
// Anyone else who drops into the Deep Region is put back on the Sanctuary floor above.
public final class DeepGate {

    private static final int CHECK_INTERVAL = 10;

    private DeepGate() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeepGate::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)
                || !DungeonDimension.isDungeon(level)
                || player.getBlockY() >= DungeonDimension.SANCTUARY_BOTTOM
                || player.isCreative()
                || player.isSpectator()
                || GateCheck.passesDeep(player)) {
                continue;
            }
            Vec3 spot = DungeonPortals.arrivalSpot(level, player.getBlockX(), player.getBlockZ());
            player.teleportTo(spot.x, spot.y, spot.z);
            player.resetFallDistance();
            player.sendSystemMessage(Component.literal(String.format(
                java.util.Locale.ROOT,
                "The Deep Region is beyond you for now. Defence %.0f of %.0f needed, weapon %.1f of %.1f needed.",
                GateCheck.defence(player), GateCheck.DEEP_DEFENCE, GateCheck.bestWeapon(player), GateCheck.DEEP_WEAPON
            )));
        }
    }
}
