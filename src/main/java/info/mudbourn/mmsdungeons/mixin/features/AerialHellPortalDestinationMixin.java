package info.mudbourn.mmsdungeons.mixin.features;

import fr.factionbedrock.aerialhell.Block.AerialHellPortalBlock;
import fr.factionbedrock.aerialhell.Registry.Worldgen.AerialHellDimensions;
import info.mudbourn.mmsdungeons.aerialhell.AerialHellPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Sends players leaving Aerial Hell to their respawn point and stops portals from working outside the dungeon and Aerial Hell.
@Mixin(AerialHellPortalBlock.class)
public abstract class AerialHellPortalDestinationMixin {

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$route(
        ServerLevel level,
        Entity entity,
        BlockPos pos,
        CallbackInfoReturnable<TeleportTransition> cir
    ) {
        if (level.dimension().equals(AerialHellDimensions.AERIAL_HELL_DIMENSION)) {
            TeleportTransition exit = entity instanceof ServerPlayer player
                ? player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING)
                : null;
            cir.setReturnValue(exit);
        } else if (!AerialHellPortals.allowsPortals(level)) {
            cir.setReturnValue(null);
        }
    }
}
