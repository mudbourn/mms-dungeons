package info.mudbourn.mmsdungeons.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.ColdSkeleton;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.CrimsonWalker;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.MinerZombie;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.RevenantSkeleton;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.SunkenSkeleton;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

public final class LuminousMobs {

    public static EntityType<MinerZombie> MINER_ZOMBIE;
    public static EntityType<Zombie> ICY_ZOMBIE;
    public static EntityType<Zombie> SWAMP_ZOMBIE;
    public static EntityType<Zombie> DARK_OAK_ZOMBIE;
    public static EntityType<Zombie> CHERRY_ZOMBIE;
    public static EntityType<Zombie> SAVANNA_ZOMBIE;
    public static EntityType<CrimsonWalker> CRIMSON_WALKER;
    public static EntityType<Skeleton> MINER_SKELETON;
    public static EntityType<ColdSkeleton> ICY_SKELETON;
    public static EntityType<Skeleton> SWAMP_SKELETON;
    public static EntityType<Skeleton> DARK_OAK_SKELETON;
    public static EntityType<Skeleton> HOLLOW_SKELETON;
    public static EntityType<Skeleton> CHERRY_SKELETON;
    public static EntityType<Skeleton> SAVANNA_SKELETON;
    public static EntityType<SunkenSkeleton> SUNKEN_SKELETON;
    public static EntityType<RevenantSkeleton> REVENANT_SKELETON;

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.95f;

    private LuminousMobs() {
    }

    public static void register() {
        registerTypes();
        registerAttributes();
        registerSpawnPlacements();
    }

    private static void registerTypes() {
        MINER_ZOMBIE = register("miner_zombie", EntityType.Builder.of(MinerZombie::new, MobCategory.MONSTER), HEIGHT);
        ICY_ZOMBIE = register("icy_zombie", EntityType.Builder.of(Zombie::new, MobCategory.MONSTER), HEIGHT);
        SWAMP_ZOMBIE = register("swamp_zombie", EntityType.Builder.of(Zombie::new, MobCategory.MONSTER), HEIGHT);
        DARK_OAK_ZOMBIE = register("dark_oak_zombie", EntityType.Builder.of(Zombie::new, MobCategory.MONSTER), 1.8f);
        CHERRY_ZOMBIE = register("cherry_zombie", EntityType.Builder.of(Zombie::new, MobCategory.MONSTER), HEIGHT);
        SAVANNA_ZOMBIE = register("savanna_zombie", EntityType.Builder.of(Zombie::new, MobCategory.MONSTER), HEIGHT);
        CRIMSON_WALKER = register(
            "crimson_walker",
            EntityType.Builder.of(CrimsonWalker::new, MobCategory.MONSTER).fireImmune(),
            HEIGHT
        );
        MINER_SKELETON = register("miner_skeleton", EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER), HEIGHT);
        ICY_SKELETON = register("icy_skeleton", EntityType.Builder.of(ColdSkeleton::new, MobCategory.MONSTER), HEIGHT);
        SWAMP_SKELETON = register("swamp_skeleton", EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER), HEIGHT);
        DARK_OAK_SKELETON = register(
            "dark_oak_skeleton",
            EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
        HOLLOW_SKELETON = register(
            "hollow_skeleton",
            EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
        CHERRY_SKELETON = register(
            "cherry_skeleton",
            EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
        SAVANNA_SKELETON = register(
            "savanna_skeleton",
            EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
        SUNKEN_SKELETON = register(
            "sunken_skeleton",
            EntityType.Builder.of(SunkenSkeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
        REVENANT_SKELETON = register(
            "revenant_skeleton",
            EntityType.Builder.of(RevenantSkeleton::new, MobCategory.MONSTER),
            HEIGHT
        );
    }

    private static void registerAttributes() {
        AttributeSupplier zombie = Zombie.createAttributes().build();
        AttributeSupplier skeleton = AbstractSkeleton.createAttributes().build();
        FabricDefaultAttributeRegistry.register(MINER_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(ICY_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(SWAMP_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(DARK_OAK_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(CHERRY_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(SAVANNA_ZOMBIE, zombie);
        FabricDefaultAttributeRegistry.register(
            CRIMSON_WALKER,
            Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
        );
        FabricDefaultAttributeRegistry.register(MINER_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(ICY_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(SWAMP_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(DARK_OAK_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(HOLLOW_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(CHERRY_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(SAVANNA_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(SUNKEN_SKELETON, skeleton);
        FabricDefaultAttributeRegistry.register(
            REVENANT_SKELETON,
            AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
        );
    }

    private static void registerSpawnPlacements() {
        registerMonster(MINER_ZOMBIE);
        registerMonster(ICY_ZOMBIE);
        registerMonster(SWAMP_ZOMBIE);
        registerMonster(DARK_OAK_ZOMBIE);
        registerMonster(CHERRY_ZOMBIE);
        registerMonster(SAVANNA_ZOMBIE);
        registerMonster(MINER_SKELETON);
        registerMonster(ICY_SKELETON);
        registerMonster(SWAMP_SKELETON);
        registerMonster(DARK_OAK_SKELETON);
        registerMonster(HOLLOW_SKELETON);
        registerMonster(CHERRY_SKELETON);
        registerMonster(SAVANNA_SKELETON);
        registerMonster(SUNKEN_SKELETON);
        registerBelowSanctuary(CRIMSON_WALKER);
        registerBelowSanctuary(REVENANT_SKELETON);
    }

    private static <T extends Monster> void registerMonster(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );
    }

    private static <T extends Monster> void registerBelowSanctuary(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            LuminousMobs::checkBelowSanctuarySpawn
        );
    }

    // The nether-born variants rise only in the Deep and Periculum Regions.
    private static boolean checkBelowSanctuarySpawn(
        EntityType<? extends Monster> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        boolean worldSpawn = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        return !worldSpawn || DungeonDimension.isDungeon(level.getLevel()) && pos.getY() < DungeonDimension.SANCTUARY_BOTTOM;
    }

    private static <T extends Monster> EntityType<T> register(
        String name,
        EntityType.Builder<T> builder,
        float height
    ) {
        Identifier id = MmsDungeons.id(name);
        EntityType<T> type = builder
            .sized(WIDTH, height)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }
}
