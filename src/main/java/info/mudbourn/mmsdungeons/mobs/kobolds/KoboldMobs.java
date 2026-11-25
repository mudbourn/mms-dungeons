package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.Kobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldCaptain;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldChild;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldEnchanter;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldEngineer;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldRascal;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldSkeleton;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldWarrior;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldWither;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldZombie;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class KoboldMobs {

    public static EntityType<Kobold> KOBOLD;
    public static EntityType<KoboldWarrior> KOBOLD_WARRIOR;
    public static EntityType<KoboldEnchanter> KOBOLD_ENCHANTER;
    public static EntityType<KoboldRascal> KOBOLD_RASCAL;
    public static EntityType<KoboldEngineer> KOBOLD_ENGINEER;
    public static EntityType<Kobold> KOBOLD_PIRATE;
    public static EntityType<KoboldCaptain> KOBOLD_CAPTAIN;
    public static EntityType<KoboldChild> KOBOLD_CHILD;
    public static EntityType<KoboldZombie> KOBOLD_ZOMBIE;
    public static EntityType<KoboldSkeleton> KOBOLD_SKELETON;
    public static EntityType<KoboldWither> WITHERBOLD;

    private static final double CROWD_RANGE = 48.0;
    private static final int CROWD_LIMIT = 6;

    private KoboldMobs() {
    }

    public static void register() {
        KOBOLD = register("kobold", EntityType.Builder.of(Kobold::new, MobCategory.MONSTER), true);
        KOBOLD_WARRIOR = register("kobold_warrior", EntityType.Builder.of(KoboldWarrior::new, MobCategory.MONSTER), true);
        KOBOLD_ENCHANTER = register("kobold_enchanter", EntityType.Builder.of(KoboldEnchanter::new, MobCategory.MONSTER), true);
        KOBOLD_RASCAL = register("kobold_rascal", EntityType.Builder.of(KoboldRascal::new, MobCategory.MONSTER), true);
        KOBOLD_ENGINEER = register("kobold_engineer", EntityType.Builder.of(KoboldEngineer::new, MobCategory.MONSTER), true);
        KOBOLD_PIRATE = register("kobold_pirate", EntityType.Builder.of(Kobold::new, MobCategory.MONSTER), true);
        KOBOLD_CAPTAIN = register("kobold_captain", EntityType.Builder.of(KoboldCaptain::new, MobCategory.MONSTER), true);
        KOBOLD_CHILD = register(
            "kobold_child",
            EntityType.Builder.of(KoboldChild::new, MobCategory.MISC).sized(0.4f, 0.76f).eyeHeight(0.66f).ridingOffset(-0.225f),
            false
        );
        KOBOLD_ZOMBIE = register("kobold_zombie", EntityType.Builder.of(KoboldZombie::new, MobCategory.MONSTER).notInPeaceful(), true);
        KOBOLD_SKELETON = register(
            "kobold_skeleton",
            EntityType.Builder.of(KoboldSkeleton::new, MobCategory.MONSTER).notInPeaceful(),
            true
        );
        WITHERBOLD = register("witherbold", EntityType.Builder.of(KoboldWither::new, MobCategory.MONSTER).fireImmune(), true);

        registerAttributes();
        registerSpawnRules();
    }

    private static <T extends Mob> EntityType<T> register(String name, EntityType.Builder<T> builder, boolean adultSize) {
        Identifier id = MmsDungeons.id(name);
        EntityType.Builder<T> sized = adultSize ? builder.sized(0.5f, 1.48f).eyeHeight(1.26f).ridingOffset(-0.45f) : builder;
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, sized.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    private static AttributeSupplier.Builder attributes(double health, double damage, double armor, double speed) {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, health)
            .add(Attributes.ATTACK_DAMAGE, damage)
            .add(Attributes.ARMOR, armor)
            .add(Attributes.MOVEMENT_SPEED, speed);
    }

    private static void registerAttributes() {
        AttributeSupplier living = attributes(18.0, 1.0, 2.0, 0.25).build();
        FabricDefaultAttributeRegistry.register(KOBOLD, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_WARRIOR, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_ENCHANTER, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_RASCAL, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_ENGINEER, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_PIRATE, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_CAPTAIN, living);
        FabricDefaultAttributeRegistry.register(KOBOLD_CHILD, living);
        AttributeSupplier zombold = attributes(18.0, 3.0, 2.0, 0.2).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE).build();
        FabricDefaultAttributeRegistry.register(KOBOLD_ZOMBIE, zombold);
        FabricDefaultAttributeRegistry.register(KOBOLD_SKELETON, attributes(18.0, 3.0, 0.0, 0.25).build());
        FabricDefaultAttributeRegistry.register(WITHERBOLD, attributes(32.0, 3.0, 2.0, 0.25).build());
    }

    private static void registerSpawnRules() {
        registerLiving(KOBOLD);
        registerLiving(KOBOLD_WARRIOR);
        registerLiving(KOBOLD_ENCHANTER);
        registerLiving(KOBOLD_RASCAL);
        registerLiving(KOBOLD_ENGINEER);
        registerLiving(KOBOLD_PIRATE);
        registerLiving(KOBOLD_CAPTAIN);
        registerUndead(KOBOLD_ZOMBIE);
        registerUndead(KOBOLD_SKELETON);
        registerUndead(WITHERBOLD);
    }

    private static <T extends AbstractKobold> void registerLiving(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            KoboldMobs::checkLivingSpawn
        );
    }

    private static <T extends Monster> void registerUndead(EntityType<T> type) {
        SpawnPlacements.register(
            type,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            KoboldMobs::checkUndeadSpawn
        );
    }

    // Living kobolds settle the Deep Region above the Periculum Region, a few to an area.
    private static boolean checkLivingSpawn(
        EntityType<? extends AbstractKobold> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (!isWorldSpawn(reason)) {
            return true;
        }
        boolean inDungeon = DungeonDimension.isDungeon(level.getLevel());
        boolean deep = inDungeon && pos.getY() > DungeonDimension.PERICULUM_TOP && pos.getY() < DungeonDimension.SANCTUARY_BOTTOM;
        return deep && level.getEntitiesOfClass(AbstractKobold.class, new AABB(pos).inflate(CROWD_RANGE)).size() < CROWD_LIMIT;
    }

    // The undead kobolds rise only in the Periculum Region.
    private static boolean checkUndeadSpawn(
        EntityType<? extends Monster> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        return !isWorldSpawn(reason) || DungeonDimension.isDungeon(level.getLevel()) && pos.getY() <= DungeonDimension.PERICULUM_TOP;
    }

    private static boolean isWorldSpawn(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
    }
}
