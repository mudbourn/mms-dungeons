package info.mudbourn.mmsdungeons.mobs.orpiment;

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

    public static void register() {
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
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    // Sap settles the Deep Region of the dungeon and nowhere else.
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
}
