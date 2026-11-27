package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;

// Where the Fangs 'n Claws mobs may spawn in the dungeon, and which cave and Nether biomes list them.
public final class FangsNClawsSpawns {

    // A stretch of the dungeon's height that a natural spawn must fall inside.
    public enum Region {
        ANY,
        BELOW_SANCTUARY
    }

    private static final TagKey<Biome> NETHER_INFESTATION =
        TagKey.create(Registries.BIOME, MmsDungeons.id("nether_infestation"));
    private static final TagKey<Biome> DUNGEON_GRASSY =
        TagKey.create(Registries.BIOME, MmsDungeons.id("dungeon_grassy"));
    private static final double PACK_RANGE = 24.0;
    private static final int GROUP_LIMIT = 3;

    private FangsNClawsSpawns() {
    }

    public static void register() {
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES, Biomes.SULFUR_CAVES),
            MobCategory.MONSTER,
            FangsNClawsMobs.CAVE_OGRE,
            15,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(NETHER_INFESTATION),
            MobCategory.MONSTER,
            FangsNClawsMobs.HELL_OGRE,
            6,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES),
            MobCategory.MONSTER,
            FangsNClawsMobs.IMP,
            10,
            1,
            3
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES),
            MobCategory.MONSTER,
            FangsNClawsMobs.FIRE_GHOST,
            12,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(DUNGEON_GRASSY),
            MobCategory.MONSTER,
            FangsNClawsMobs.WILD_WOLF,
            20,
            2,
            4
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(DUNGEON_GRASSY),
            MobCategory.MONSTER,
            FangsNClawsMobs.ZOMBIE_HORSE,
            10,
            1,
            2
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(DUNGEON_GRASSY),
            MobCategory.MONSTER,
            FangsNClawsMobs.SKELETON_HORSE,
            10,
            1,
            2
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(DUNGEON_GRASSY),
            MobCategory.MONSTER,
            FangsNClawsMobs.OWLBEAR,
            8,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(DUNGEON_GRASSY),
            MobCategory.MONSTER,
            FangsNClawsMobs.GOLEM,
            6,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.SULFUR_CAVES),
            MobCategory.MONSTER,
            FangsNClawsMobs.DESERT_SCORPION,
            15,
            1,
            3
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.DRIPSTONE_CAVES),
            MobCategory.MONSTER,
            FangsNClawsMobs.SCORPION,
            12,
            1,
            2
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.DRIPSTONE_CAVES, Biomes.SULFUR_CAVES, Biomes.LUSH_CAVES),
            MobCategory.MONSTER,
            FangsNClawsMobs.WEREWOLF,
            8,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(NETHER_INFESTATION),
            MobCategory.MONSTER,
            FangsNClawsMobs.NETHER_SCORPION,
            12,
            1,
            2
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES),
            MobCategory.MONSTER,
            FangsNClawsMobs.NIGHTMARE_HORSE,
            6,
            1,
            1
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.SOUL_SAND_VALLEY),
            MobCategory.MONSTER,
            FangsNClawsMobs.GHOST,
            15,
            1,
            1
        );
    }

    // The spawn rule for a natural spawn, which only counts inside the dungeon.
    static <T extends Monster> boolean canSpawn(
        EntityType<T> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random,
        Region region
    ) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        if (!DungeonDimension.isDungeon(level.getLevel()) || !within(region, pos.getY())) {
            return false;
        }
        boolean packed = type == FangsNClawsMobs.DART_GOBLIN || type == FangsNClawsMobs.IMP;
        AABB around = new AABB(pos).inflate(PACK_RANGE, 8.0, PACK_RANGE);
        if (!packed) {
            return true;
        }
        int neighbors = level.getEntitiesOfClass(Monster.class, around, entity -> entity.getType() == type).size();
        return neighbors < GROUP_LIMIT;
    }

    private static boolean within(Region region, int y) {
        return switch (region) {
            case ANY -> true;
            case BELOW_SANCTUARY -> y < DungeonDimension.SANCTUARY_BOTTOM;
        };
    }
}
