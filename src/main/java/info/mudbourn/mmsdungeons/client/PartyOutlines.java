package info.mudbourn.mmsdungeons.client;

import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.party.PartyView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

// Decides which entities the client outlines through walls: teammates within the configured range.
public final class PartyOutlines {

    public static final int COLOR = 0x55FFFF;

    private PartyOutlines() {
    }

    public static boolean outlined(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof Player) || minecraft.player == null || entity == minecraft.player) {
            return false;
        }
        double range = FeaturesConfig.get().outlineDistance;
        return PartyView.isMember(entity.getUUID()) && entity.distanceToSqr(minecraft.player) <= range * range;
    }
}
