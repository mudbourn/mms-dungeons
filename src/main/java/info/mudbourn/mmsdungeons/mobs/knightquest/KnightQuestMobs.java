package info.mudbourn.mmsdungeons.mobs.knightquest;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.BadPatch;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.EldBomb;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.EldKnight;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.FallenKnight;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Ghastling;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Ghosty;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Gremlin;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Lizzy;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.MommaLizzy;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.NethermanCharge;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.NethermanClone;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Ratman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Samhain;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Swampman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.SwampmanAxe;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

public final class KnightQuestMobs {

    public static EntityType<Gremlin> GREMLIN;
    public static EntityType<BadPatch> BAD_PATCH;
    public static EntityType<EldKnight> ELD_KNIGHT;
    public static EntityType<EldBomb> ELD_BOMB;
    public static EntityType<Lizzy> LIZZY;
    public static EntityType<MommaLizzy> MOMMA_LIZZY;
    public static EntityType<Samhain> SAMHAIN;
    public static EntityType<Swampman> SWAMPMAN;
    public static EntityType<SwampmanAxe> SWAMPMAN_AXE;
    public static EntityType<Ratman> RATMAN;
    public static EntityType<Ghastling> GHASTLING;
    public static EntityType<Ghosty> GHOSTY;
    public static EntityType<FallenKnight> FALLEN_KNIGHT;
    public static EntityType<Netherman> NETHERMAN;
    public static EntityType<NethermanClone> NETHERMAN_CLONE;
    public static EntityType<NethermanCharge> NETHERMAN_CHARGE;

    private KnightQuestMobs() {
    }

    public static void register() {
        registerTypes();
        registerAttributes();
        registerSpawnPlacements();
    }

    private static void registerTypes() {
        GREMLIN = register("gremlin", EntityType.Builder.of(Gremlin::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8));
        BAD_PATCH = register("bad_patch", EntityType.Builder.of(BadPatch::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8));
        ELD_KNIGHT = register("eld_knight", EntityType.Builder.of(EldKnight::new, MobCategory.MONSTER).sized(1.0f, 2.6f).clientTrackingRange(10));
        ELD_BOMB = register("eld_bomb", EntityType.Builder.of(EldBomb::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8));
        LIZZY = register("lizzy", EntityType.Builder.of(Lizzy::new, MobCategory.CREATURE).sized(1.0f, 0.3f).clientTrackingRange(8));
        MOMMA_LIZZY = register("momma_lizzy", EntityType.Builder.of(MommaLizzy::new, MobCategory.CREATURE).sized(1.1f, 0.55f).clientTrackingRange(8));
        SAMHAIN = register("samhain", EntityType.Builder.of(Samhain::new, MobCategory.MONSTER).sized(1.0f, 1.5f).clientTrackingRange(8));
        SWAMPMAN = register("swampman", EntityType.Builder.of(Swampman::new, MobCategory.MONSTER).sized(1.0f, 2.0f).clientTrackingRange(10));
        SWAMPMAN_AXE = register(
            "swampman_axe",
            EntityType.Builder.<SwampmanAxe>of(SwampmanAxe::new, MobCategory.MISC).sized(0.3f, 1.0f).clientTrackingRange(4).updateInterval(1)
        );
        RATMAN = register("ratman", EntityType.Builder.of(Ratman::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8));
        GHASTLING = register("ghastling", EntityType.Builder.of(Ghastling::new, MobCategory.MONSTER).sized(0.65f, 0.65f).clientTrackingRange(8));
        GHOSTY = register("ghosty", EntityType.Builder.of(Ghosty::new, MobCategory.MONSTER).sized(1.0f, 1.0f).clientTrackingRange(8));
        FALLEN_KNIGHT = register("fallen_knight", EntityType.Builder.of(FallenKnight::new, MobCategory.MONSTER).sized(1.0f, 2.0f).clientTrackingRange(10));
        NETHERMAN = register("netherman", EntityType.Builder.of(Netherman::new, MobCategory.MONSTER).sized(0.8f, 2.8f).clientTrackingRange(16));
        NETHERMAN_CLONE = register("netherman_clone", EntityType.Builder.of(NethermanClone::new, MobCategory.MONSTER).sized(0.8f, 2.8f).clientTrackingRange(10));
        NETHERMAN_CHARGE = register(
            "netherman_charge",
            EntityType.Builder.<NethermanCharge>of(NethermanCharge::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(1)
        );
    }

    private static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(GREMLIN, Gremlin.createAttributes());
        FabricDefaultAttributeRegistry.register(BAD_PATCH, BadPatch.createAttributes());
        FabricDefaultAttributeRegistry.register(ELD_KNIGHT, EldKnight.createAttributes());
        FabricDefaultAttributeRegistry.register(ELD_BOMB, EldBomb.createAttributes());
        FabricDefaultAttributeRegistry.register(LIZZY, Lizzy.createAttributes());
        FabricDefaultAttributeRegistry.register(MOMMA_LIZZY, MommaLizzy.createAttributes());
        FabricDefaultAttributeRegistry.register(SAMHAIN, Samhain.createAttributes());
        FabricDefaultAttributeRegistry.register(SWAMPMAN, Swampman.createAttributes());
        FabricDefaultAttributeRegistry.register(RATMAN, Ratman.createAttributes());
        FabricDefaultAttributeRegistry.register(GHASTLING, Ghastling.createAttributes());
        FabricDefaultAttributeRegistry.register(GHOSTY, Ghosty.createAttributes());
        FabricDefaultAttributeRegistry.register(FALLEN_KNIGHT, FallenKnight.createAttributes());
        FabricDefaultAttributeRegistry.register(NETHERMAN, Netherman.createAttributes());
        FabricDefaultAttributeRegistry.register(NETHERMAN_CLONE, NethermanClone.createAttributes());
    }

    private static void registerSpawnPlacements() {
        registerMonster(GREMLIN);
        registerMonster(BAD_PATCH);
        registerMonster(ELD_KNIGHT);
        registerMonster(ELD_BOMB);
        registerMonster(SWAMPMAN);
        registerMonster(RATMAN);
        registerMonster(GHOSTY);
        registerMonster(FALLEN_KNIGHT);
        registerCreature(LIZZY);
        registerCreature(MOMMA_LIZZY);
    }

    private static <T extends Monster> void registerMonster(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );
    }

    private static <T extends Mob> void registerCreature(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Mob::checkMobSpawnRules
        );
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }
}
