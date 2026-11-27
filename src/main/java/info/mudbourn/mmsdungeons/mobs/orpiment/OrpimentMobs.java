package info.mudbourn.mmsdungeons.mobs.orpiment;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Craw;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Sap;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.TarClumpProjectile;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.TarCube;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Wisp;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.WispSkullProjectile;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Wraith;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

public final class OrpimentMobs {

    public static EntityType<TarCube> TAR_CUBE;
    public static EntityType<Sap> SAP;
    public static EntityType<Wraith> WRAITH;
    public static EntityType<Wisp> WISP;
    public static EntityType<Craw> CRAW;
    public static EntityType<TarClumpProjectile> TAR_CLUMP_PROJECTILE;
    public static EntityType<WispSkullProjectile> WISP_SKULL_PROJECTILE;

    private OrpimentMobs() {
    }

    private static final ResourceKey<DamageType> SOUL = ResourceKey.create(Registries.DAMAGE_TYPE, MmsDungeons.id("soul"));

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(OrpimentMobs::soulDamage);
        OrpimentDataSerializers.register();
        TAR_CUBE = register("tar_cube", EntityType.Builder.of(TarCube::new, MobCategory.MONSTER).sized(0.52f, 0.52f).clientTrackingRange(8));
        SAP = register("sap", EntityType.Builder.of(Sap::new, MobCategory.MONSTER).sized(0.52f, 0.52f).clientTrackingRange(8));
        WRAITH = register("wraith", EntityType.Builder.of(Wraith::new, MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(10));
        WISP = register("wisp", EntityType.Builder.of(Wisp::new, MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(8));
        CRAW = register("craw", EntityType.Builder.of(Craw::new, MobCategory.MONSTER).sized(0.9f, 1.4f).clientTrackingRange(8));
        TAR_CLUMP_PROJECTILE = register(
            "tar_clump_projectile",
            EntityType.Builder.<TarClumpProjectile>of(TarClumpProjectile::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(1)
        );
        WISP_SKULL_PROJECTILE = register(
            "wisp_skull_projectile",
            EntityType.Builder.<WispSkullProjectile>of(WispSkullProjectile::new, MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(1)
        );
        FabricDefaultAttributeRegistry.register(TAR_CUBE, TarCube.createAttributes());
        FabricDefaultAttributeRegistry.register(SAP, Sap.createAttributes());
        FabricDefaultAttributeRegistry.register(WRAITH, Wraith.createAttributes());
        FabricDefaultAttributeRegistry.register(WISP, Wisp.createAttributes());
        FabricDefaultAttributeRegistry.register(CRAW, Craw.createAttributes());
        SpawnPlacements.register(SAP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrpimentMobs::checkSapSpawn);
        SpawnPlacements.register(TAR_CUBE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrpimentMobs::checkDungeonSpawn);
        SpawnPlacements.register(WRAITH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrpimentMobs::checkDungeonSpawn);
        SpawnPlacements.register(WISP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrpimentMobs::checkDungeonSpawn);
        SpawnPlacements.register(CRAW, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrpimentMobs::checkDungeonSpawn);
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    // Sap settles the Deep and Periculum Regions of the dungeon and nowhere else.
    private static boolean checkSapSpawn(
        EntityType<Sap> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        boolean worldSpawn = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        return !worldSpawn || DungeonDimension.isDungeon(level.getLevel()) && pos.getY() < DungeonDimension.SANCTUARY_BOTTOM;
    }

    // Tar Cubes, Wraiths, Wisps and Craws rise from the world only in the Deep and Periculum Regions; spawners, eggs and commands work anywhere.
    // Wraiths, wisps and their skulls tear at the soul: their hits are re-dealt as soul damage, which ignores armor.
    private static boolean soulDamage(LivingEntity victim, DamageSource source, float amount) {
        boolean spectral = source.getEntity() instanceof Wraith || source.getEntity() instanceof Wisp
            || source.getDirectEntity() instanceof WispSkullProjectile;
        if (!spectral || source.is(SOUL) || !(victim.level() instanceof ServerLevel level)) {
            return true;
        }
        DamageSource soul = new DamageSource(
            level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SOUL),
            source.getDirectEntity(),
            source.getEntity()
        );
        victim.hurtServer(level, soul, amount);
        return false;
    }

    private static boolean checkDungeonSpawn(
        EntityType<? extends Mob> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        boolean worldSpawn = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        return !worldSpawn || DungeonDimension.isDungeon(level.getLevel()) && pos.getY() < DungeonDimension.SANCTUARY_BOTTOM;
    }
}
