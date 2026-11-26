package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Keeps natural spawns in the Aerial Hell cavern on solid ground.
@Mixin(SpawnPlacements.class)
public abstract class CavernSpawnGroundMixin {

    @Unique
    private static final ResourceKey<Biome> CAVERN = ResourceKey.create(Registries.BIOME, MmsDungeons.id("aerial_hell_cavern"));

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T> void mmsDungeons$groundOnly(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }
        if (!DungeonDimension.isDungeon(level.getLevel()) || !level.getBiome(pos).is(CAVERN)) {
            return;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            cir.setReturnValue(false);
        }
    }
}
