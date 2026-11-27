package info.mudbourn.mmsdungeons.mixin.dungeon;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
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

// Keeps bosses out of the Sanctuary and rare below it: a natural spawn of anything in c:bosses passes one roll in
// RARITY and needs no other boss within BOSS_RANGE blocks. Useless Reptile's dragons spawn in their own mob category in every
// shared cave biome, so a Moleclaw rolls again in MOLECLAW_RARITY, and the dragons never spawn with a new chunk,
// which would also read past the chunk.
@Mixin(SpawnPlacements.class)
public abstract class DragonSpawnSpacingMixin {

    @Unique
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
    @Unique
    private static final Identifier MOLECLAW = Identifier.fromNamespaceAndPath("uselessreptile", "moleclaw");
    @Unique
    private static final double BOSS_RANGE = 96.0;
    @Unique
    private static final int RARITY = 4;
    @Unique
    private static final int MOLECLAW_RARITY = 3;

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T> void mmsDungeons$spaceBosses(
        EntityType<?> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!type.builtInRegistryHolder().is(BOSSES) || !DungeonDimension.isDungeon(level.getLevel())) {
            return;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        boolean dragon = "uselessreptile".equals(id.getNamespace());
        if (DungeonDimension.isSanctuary(level.getLevel(), pos) && (reason == EntitySpawnReason.NATURAL
            || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.STRUCTURE)) {
            cir.setReturnValue(false);
            return;
        }
        if (dragon && reason == EntitySpawnReason.CHUNK_GENERATION) {
            cir.setReturnValue(false);
            return;
        }
        if (reason != EntitySpawnReason.NATURAL) {
            return;
        }
        if (random.nextInt(RARITY) != 0 || MOLECLAW.equals(id) && random.nextInt(MOLECLAW_RARITY) != 0) {
            cir.setReturnValue(false);
            return;
        }
        AABB around = new AABB(pos).inflate(BOSS_RANGE);
        if (!level.getEntitiesOfClass(Entity.class, around, entity -> entity.getType().builtInRegistryHolder().is(BOSSES)).isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}
