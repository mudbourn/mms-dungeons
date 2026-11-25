package info.mudbourn.mmsdungeons.mixin.features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.factionbedrock.aerialhell.Block.AerialHellPortalBlock;
import fr.factionbedrock.aerialhell.Item.StellarLighterItem;
import info.mudbourn.mmsdungeons.aerialhell.AerialHellPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Hooks the moment the Stellar Lighter opens an Aerial Hell portal.
@Mixin(StellarLighterItem.class)
public abstract class StellarLighterMixin {

    @WrapOperation(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lfr/factionbedrock/aerialhell/Block/AerialHellPortalBlock;trySpawnPortal(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean mmsDungeons$blastOnLight(
        AerialHellPortalBlock block,
        LevelAccessor level,
        BlockPos pos,
        Operation<Boolean> original,
        @Local(argsOnly = true) UseOnContext context
    ) {
        boolean lit = original.call(block, level, pos);
        if (lit && level instanceof ServerLevel serverLevel && context.getPlayer() instanceof ServerPlayer player) {
            AerialHellPortals.onLit(serverLevel, pos, player);
        }
        return lit;
    }
}
