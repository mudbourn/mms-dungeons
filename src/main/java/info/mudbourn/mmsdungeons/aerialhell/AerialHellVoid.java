package info.mudbourn.mmsdungeons.aerialhell;

import fr.factionbedrock.aerialhell.Registry.Worldgen.AerialHellDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

// Drops players who fall out of Aerial Hell into the overworld sky, high above build height, before void damage.
public final class AerialHellVoid {

    private static final int HEIGHT_ABOVE_BUILD = 90;

    private AerialHellVoid() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AerialHellVoid::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();
            boolean fellOut = level.dimension().equals(AerialHellDimensions.AERIAL_HELL_DIMENSION)
                && player.getY() < level.getMinY();
            if (fellOut) {
                drop(player, overworld);
            }
        }
    }

    private static void drop(ServerPlayer player, ServerLevel overworld) {
        Vec3 sky = new Vec3(player.getX(), overworld.getMaxY() + HEIGHT_ABOVE_BUILD, player.getZ());
        player.teleport(new TeleportTransition(
            overworld,
            sky,
            player.getDeltaMovement(),
            player.getYRot(),
            player.getXRot(),
            TeleportTransition.DO_NOTHING
        ));
    }
}
