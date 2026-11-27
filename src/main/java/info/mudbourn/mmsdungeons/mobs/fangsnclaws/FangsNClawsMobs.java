package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.AcidCloud;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.AcidSplit;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyHyena;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyMob;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyOwlbear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyScorpion;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyShrike;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyWildWolf;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.CatchingClawHook;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.CaveOgre;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.DartGoblin;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.DesertScorpion;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.EvilBat;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.FeatherProjectile;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.FireGhost;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.FrostScorpion;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Ghost;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Goblin;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Golem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.HellOgre;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.HorseBat;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Hyena;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.IceGolem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Imp;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Mimic;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.NetherScorpion;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.NightmareHorse;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Ogre;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Owlbear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PoisonousDart;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PurpleWorm;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PurpleWormArm;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PurpleWormPart;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Scorpion;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Shrike;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.SilverSkeleton;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.SkeletonHorse;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownPitchfork;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownTomahawk;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.UndeadHorse;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.VelocityArrow;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Werewolf;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.WildWolf;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ZombieHorse;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

// The Fangs 'n Claws entity types.
public final class FangsNClawsMobs {

    public static EntityType<Goblin> GOBLIN;
    public static EntityType<DartGoblin> DART_GOBLIN;
    public static EntityType<Ogre> OGRE;
    public static EntityType<CaveOgre> CAVE_OGRE;
    public static EntityType<HellOgre> HELL_OGRE;
    public static EntityType<Imp> IMP;
    public static EntityType<Ghost> GHOST;
    public static EntityType<FireGhost> FIRE_GHOST;
    public static EntityType<EvilBat> EVIL_BAT;
    public static EntityType<Mimic> MIMIC;
    public static EntityType<SilverSkeleton> SILVER_SKELETON;
    public static EntityType<PoisonousDart> POISONOUS_DART;
    public static EntityType<Hyena> HYENA;
    public static EntityType<BabyHyena> BABY_HYENA;
    public static EntityType<WildWolf> WILD_WOLF;
    public static EntityType<BabyWildWolf> BABY_WILD_WOLF;
    public static EntityType<Owlbear> OWLBEAR;
    public static EntityType<BabyOwlbear> BABY_OWLBEAR;
    public static EntityType<Shrike> SHRIKE;
    public static EntityType<BabyShrike> BABY_SHRIKE;
    public static EntityType<Scorpion> SCORPION;
    public static EntityType<DesertScorpion> DESERT_SCORPION;
    public static EntityType<FrostScorpion> FROST_SCORPION;
    public static EntityType<NetherScorpion> NETHER_SCORPION;
    public static EntityType<BabyScorpion> BABY_SCORPION;
    public static EntityType<Werewolf> WEREWOLF;
    public static EntityType<ZombieHorse> ZOMBIE_HORSE;
    public static EntityType<SkeletonHorse> SKELETON_HORSE;
    public static EntityType<NightmareHorse> NIGHTMARE_HORSE;
    public static EntityType<HorseBat> HORSE_BAT;
    public static EntityType<Golem> GOLEM;
    public static EntityType<IceGolem> ICE_GOLEM;
    public static EntityType<PurpleWorm> PURPLE_WORM;
    public static EntityType<PurpleWormPart> PURPLE_WORM_PART;
    public static EntityType<PurpleWormArm> PURPLE_WORM_ARM;
    public static EntityType<AcidCloud> ACID_CLOUD;
    public static EntityType<AcidSplit> ACID_SPLIT;
    public static EntityType<FeatherProjectile> FEATHER_PROJECTILE;
    public static EntityType<VelocityArrow> VELOCITY_ARROW;
    public static EntityType<ThrownPitchfork> THROWN_PITCHFORK;
    public static EntityType<ThrownTomahawk> THROWN_TOMAHAWK;
    public static EntityType<CatchingClawHook> CATCHING_CLAW_HOOK;

    private FangsNClawsMobs() {
    }

    public static void register() {
        registerTypes();
        registerAttributes();
        registerSpawnPlacements();
    }

    private static void registerTypes() {
        GOBLIN = register("goblin", monster(Goblin::new, 0.6f, 1.4f, 8));
        DART_GOBLIN = register("dart_goblin", monster(DartGoblin::new, 0.6f, 1.4f, 8));
        OGRE = register("ogre", monster(Ogre::new, 1.95f, 2.95f, 10));
        CAVE_OGRE = register("cave_ogre", monster(CaveOgre::new, 1.95f, 2.95f, 10));
        HELL_OGRE = register("hell_ogre", monster(HellOgre::new, 1.95f, 2.95f, 10).fireImmune());
        IMP = register("imp", monster(Imp::new, 0.5f, 1.2f, 8).fireImmune());
        GHOST = register("ghost", monster(Ghost::new, 0.8f, 1.6f, 8));
        FIRE_GHOST = register("fire_ghost", monster(FireGhost::new, 0.8f, 1.6f, 8).fireImmune());
        EVIL_BAT = register("evil_bat", monster(EvilBat::new, 0.5f, 0.9f, 8));
        MIMIC = register("mimic", monster(Mimic::new, 0.9f, 0.9f, 8));
        SILVER_SKELETON = register("silver_skeleton", monster(SilverSkeleton::new, 0.6f, 1.95f, 8));
        HYENA = register("hyena", monster(Hyena::new, 0.78f, 1.105f, 8));
        BABY_HYENA = register("baby_hyena", young(BabyHyena::new, 0.5f, 0.7f));
        WILD_WOLF = register("wild_wolf", monster(WildWolf::new, 0.78f, 1.105f, 8));
        BABY_WILD_WOLF = register("baby_wild_wolf", young(BabyWildWolf::new, 0.5f, 0.7f));
        OWLBEAR = register("owlbear", monster(Owlbear::new, 1.95f, 1.95f, 10));
        BABY_OWLBEAR = register("baby_owlbear", young(BabyOwlbear::new, 0.9f, 0.9f));
        SHRIKE = register("shrike", monster(Shrike::new, 1.95f, 1.95f, 10));
        BABY_SHRIKE = register("baby_shrike", young(BabyShrike::new, 0.9f, 0.9f));
        SCORPION = register("scorpion", monster(Scorpion::new, 1.4f, 0.9f, 8));
        DESERT_SCORPION = register("desert_scorpion", monster(DesertScorpion::new, 1.4f, 0.9f, 8));
        FROST_SCORPION = register("frost_scorpion", monster(FrostScorpion::new, 1.4f, 0.9f, 8));
        NETHER_SCORPION = register("nether_scorpion", monster(NetherScorpion::new, 1.4f, 0.9f, 8).fireImmune());
        BABY_SCORPION = register("baby_scorpion", young(BabyScorpion::new, 0.6f, 0.4f));
        WEREWOLF = register("werewolf", monster(Werewolf::new, 1.0f, 1.95f, 8));
        ZOMBIE_HORSE = register("zombie_horse", monster(ZombieHorse::new, 1.4f, 1.6f, 10));
        SKELETON_HORSE = register("skeleton_horse", monster(SkeletonHorse::new, 1.4f, 1.6f, 10));
        NIGHTMARE_HORSE = register("nightmare_horse", monster(NightmareHorse::new, 1.4f, 1.6f, 10).fireImmune());
        HORSE_BAT = register("horse_bat", monster(HorseBat::new, 1.4f, 1.6f, 10));
        GOLEM = register("golem", monster(Golem::new, 2.4f, 3.6f, 10));
        ICE_GOLEM = register("ice_golem", monster(IceGolem::new, 2.4f, 3.6f, 10));
        PURPLE_WORM = register("purple_worm", monster(PurpleWorm::new, 3.3f, 3.0f, 16));
        PURPLE_WORM_PART = register(
            "purple_worm_part",
            EntityType.Builder.<PurpleWormPart>of(PurpleWormPart::new, MobCategory.MISC)
                .sized(2.6f, 2.0f)
                .noSave()
                .noSummon()
                .clientTrackingRange(16)
                .updateInterval(1)
        );
        PURPLE_WORM_ARM = register(
            "purple_worm_arm",
            EntityType.Builder.<PurpleWormArm>of(PurpleWormArm::new, MobCategory.MISC)
                .sized(1.4f, 3.0f)
                .noSave()
                .clientTrackingRange(10)
        );
        ACID_CLOUD = register(
            "acid_cloud",
            EntityType.Builder.<AcidCloud>of(AcidCloud::new, MobCategory.MISC)
                .sized(2.0f, 1.0f)
                .noSave()
                .clientTrackingRange(10)
                .updateInterval(20)
        );
        ACID_SPLIT = register(
            "acid_split",
            EntityType.Builder.<AcidSplit>of(AcidSplit::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(8)
                .updateInterval(2)
        );
        FEATHER_PROJECTILE = register(
            "feather_projectile",
            EntityType.Builder.<FeatherProjectile>of(FeatherProjectile::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(20)
        );
        VELOCITY_ARROW = register(
            "velocity_arrow",
            EntityType.Builder.<VelocityArrow>of(VelocityArrow::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(20)
        );
        POISONOUS_DART = register(
            "poisonous_dart",
            EntityType.Builder.<PoisonousDart>of(PoisonousDart::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(20)
        );
        THROWN_PITCHFORK = register(
            "thrown_pitchfork",
            EntityType.Builder.<ThrownPitchfork>of(ThrownPitchfork::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(20)
        );
        THROWN_TOMAHAWK = register(
            "thrown_tomahawk",
            EntityType.Builder.<ThrownTomahawk>of(ThrownTomahawk::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(4)
                .updateInterval(20)
        );
        CATCHING_CLAW_HOOK = register(
            "catching_claw_hook",
            EntityType.Builder.<CatchingClawHook>of(CatchingClawHook::new, MobCategory.MISC)
                .sized(0.25f, 0.25f)
                .clientTrackingRange(4)
                .updateInterval(5)
        );
    }

    private static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(GOBLIN, Goblin.createAttributes());
        FabricDefaultAttributeRegistry.register(DART_GOBLIN, DartGoblin.createAttributes());
        FabricDefaultAttributeRegistry.register(OGRE, Ogre.createAttributes());
        FabricDefaultAttributeRegistry.register(CAVE_OGRE, Ogre.createAttributes());
        FabricDefaultAttributeRegistry.register(HELL_OGRE, HellOgre.createAttributes());
        FabricDefaultAttributeRegistry.register(IMP, Imp.createAttributes());
        FabricDefaultAttributeRegistry.register(GHOST, Ghost.createAttributes());
        FabricDefaultAttributeRegistry.register(FIRE_GHOST, FireGhost.createAttributes());
        FabricDefaultAttributeRegistry.register(EVIL_BAT, EvilBat.createAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, Mimic.createAttributes());
        FabricDefaultAttributeRegistry.register(SILVER_SKELETON, SilverSkeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(HYENA, WildWolf.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_HYENA, BabyWildWolf.createAttributes());
        FabricDefaultAttributeRegistry.register(WILD_WOLF, WildWolf.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_WILD_WOLF, BabyWildWolf.createAttributes());
        FabricDefaultAttributeRegistry.register(OWLBEAR, Owlbear.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_OWLBEAR, BabyOwlbear.createAttributes());
        FabricDefaultAttributeRegistry.register(SHRIKE, Owlbear.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_SHRIKE, BabyOwlbear.createAttributes());
        FabricDefaultAttributeRegistry.register(SCORPION, Scorpion.createAttributes());
        FabricDefaultAttributeRegistry.register(DESERT_SCORPION, Scorpion.createAttributes());
        FabricDefaultAttributeRegistry.register(FROST_SCORPION, Scorpion.createAttributes());
        FabricDefaultAttributeRegistry.register(NETHER_SCORPION, Scorpion.createAttributes());
        FabricDefaultAttributeRegistry.register(BABY_SCORPION, BabyScorpion.createAttributes());
        FabricDefaultAttributeRegistry.register(WEREWOLF, Werewolf.createAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_HORSE, UndeadHorse.createAttributes());
        FabricDefaultAttributeRegistry.register(SKELETON_HORSE, UndeadHorse.createAttributes());
        FabricDefaultAttributeRegistry.register(NIGHTMARE_HORSE, NightmareHorse.createAttributes());
        FabricDefaultAttributeRegistry.register(HORSE_BAT, HorseBat.createAttributes());
        FabricDefaultAttributeRegistry.register(GOLEM, Golem.createAttributes());
        FabricDefaultAttributeRegistry.register(ICE_GOLEM, Golem.createAttributes());
        FabricDefaultAttributeRegistry.register(PURPLE_WORM, PurpleWorm.createAttributes());
        FabricDefaultAttributeRegistry.register(PURPLE_WORM_ARM, PurpleWormArm.createAttributes());
    }

    private static void registerSpawnPlacements() {
        place(GOBLIN, FangsNClawsSpawns.Region.ANY);
        place(DART_GOBLIN, FangsNClawsSpawns.Region.ANY);
        place(OGRE, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(CAVE_OGRE, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(HELL_OGRE, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(IMP, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(GHOST, FangsNClawsSpawns.Region.ANY);
        place(FIRE_GHOST, FangsNClawsSpawns.Region.ANY);
        place(EVIL_BAT, FangsNClawsSpawns.Region.ANY);
        place(SILVER_SKELETON, FangsNClawsSpawns.Region.ANY);
        place(HYENA, FangsNClawsSpawns.Region.ANY);
        place(WILD_WOLF, FangsNClawsSpawns.Region.ANY);
        place(OWLBEAR, FangsNClawsSpawns.Region.ANY);
        place(SHRIKE, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(SCORPION, FangsNClawsSpawns.Region.ANY);
        place(DESERT_SCORPION, FangsNClawsSpawns.Region.ANY);
        place(FROST_SCORPION, FangsNClawsSpawns.Region.ANY);
        place(NETHER_SCORPION, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(WEREWOLF, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(ZOMBIE_HORSE, FangsNClawsSpawns.Region.ANY);
        place(SKELETON_HORSE, FangsNClawsSpawns.Region.ANY);
        place(NIGHTMARE_HORSE, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(HORSE_BAT, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(GOLEM, FangsNClawsSpawns.Region.ANY);
        place(ICE_GOLEM, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
        place(PURPLE_WORM, FangsNClawsSpawns.Region.BELOW_SANCTUARY);
    }

    private static <T extends Monster> void place(EntityType<T> type, FangsNClawsSpawns.Region region) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (entityType, level, reason, pos, random) ->
                FangsNClawsSpawns.canSpawn(entityType, level, reason, pos, random, region)
        );
    }

    private static <T extends Monster> EntityType.Builder<T> monster(
        EntityType.EntityFactory<T> factory,
        float width,
        float height,
        int tracking
    ) {
        return EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).clientTrackingRange(tracking);
    }

    private static <T extends BabyMob> EntityType.Builder<T> young(
        EntityType.EntityFactory<T> factory,
        float width,
        float height
    ) {
        return EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height).clientTrackingRange(8);
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = MmsDungeons.id(name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }
}
