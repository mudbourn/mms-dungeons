package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Thins Useless Reptile's underground dragons in the dungeon. They spawn in their own mob category, so the
// mod's default variants spawn in every shared cave biome with no spacing. In the dungeon a natural Moleclaw
// passes one roll in RARITY and needs no other Moleclaw within MOLECLAW_RANGE blocks; a Swamp Wyvern needs no
// other within WYVERN_RANGE. Neither spawns with a new chunk, which would also read past the chunk.
@Mixin(SpawnPlacements.class)
public abstract class DragonSpawnSpacingMixin {

    @Unique
    private static final Identifier MOLECLAW = Identifier.fromNamespaceAndPath("uselessreptile", "moleclaw");
    @Unique
    private static final Identifier WYVERN = Identifier.fromNamespaceAndPath("uselessreptile", "wyvern");
    @Unique
    private static final double MOLECLAW_RANGE = 48.0;
    @Unique
    private static final double WYVERN_RANGE = 64.0;
    @Unique
    private static final int RARITY = 4;

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T> void mmsDungeons$spaceDragons(
        EntityType<?> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        boolean moleclaw = MOLECLAW.equals(id);
        if (!moleclaw && !WYVERN.equals(id)) {
            return;
        }
        if (!DungeonDimension.isDungeon(level.getLevel())) {
            return;
        }
        if (reason == EntitySpawnReason.CHUNK_GENERATION) {
            cir.setReturnValue(false);
            return;
        }
        if (reason != EntitySpawnReason.NATURAL) {
            return;
        }
        if (moleclaw && random.nextInt(RARITY) != 0) {
            cir.setReturnValue(false);
            return;
        }
        double range = moleclaw ? MOLECLAW_RANGE : WYVERN_RANGE;
        AABB around = new AABB(pos).inflate(range);
        if (!level.getEntitiesOfClass(Entity.class, around, entity -> entity.getType() == type).isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}
