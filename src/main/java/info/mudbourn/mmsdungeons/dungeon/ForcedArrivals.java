package info.mudbourn.mmsdungeons.dungeon;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

// Fixes where a player lands when a command or another mod moves them between the overworld and the dungeon
// without a dungeon portal: into the dungeon they land where an exit portal could stand in that column, and out
// of it they go to their spawn point, never the same coordinates in the other world.
public final class ForcedArrivals {

    private ForcedArrivals() {
    }

    public static void register() {
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(ForcedArrivals::onChange);
    }

    private static void onChange(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        if (DungeonPortals.portalMove()) {
            return;
        }
        boolean entering = origin.dimension() == Level.OVERWORLD && DungeonDimension.isDungeon(destination);
        boolean leaving = DungeonDimension.isDungeon(origin) && destination.dimension() == Level.OVERWORLD;
        if (entering) {
            Vec3 spot = DungeonPortals.arrivalSpot(destination, player.getBlockX(), player.getBlockZ());
            player.teleportTo(spot.x, spot.y, spot.z);
        } else if (leaving) {
            TeleportTransition home = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            if (home.newLevel() == destination) {
                player.teleportTo(home.position().x, home.position().y, home.position().z);
            } else {
                player.teleport(home);
            }
        }
    }
}
