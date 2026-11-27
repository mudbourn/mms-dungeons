package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Swaps DDD's "The Ugly One" trial spawner for one strong or two weaker bosses themed by the room's biome.
public final class DddTrialBoss implements StructureProcessor {

    public static final DddTrialBoss INSTANCE = new DddTrialBoss();

    private static final String UGLY_ONE = "The Ugly One";

    private static final List<Boss> FALLBACK = List.of(
        new Boss("mutantmonsters:mutant_zombie", 1, 0),
        new Boss("mutantmonsters:mutant_skeleton", 2, 75)
    );

    private static final List<Boss> GUARDIANS = List.of(
        new Boss("mms_dungeons:eld_knight", 1, 0),
        new Boss("mms_dungeons:cave_ogre", 2, 80)
    );

    private static final List<Boss> OVERGROWN = List.of(new Boss("mms_dungeons:owlbear", 2, 60));

    private static final List<Boss> NETHER = List.of(
        new Boss("mms_dungeons:hell_ogre", 1, 0),
        new Boss("mutantmonsters:mutant_creeper", 2, 60)
    );

    private static final Map<String, List<Boss>> BY_BIOME = Map.ofEntries(
        Map.entry("minecraft:lush_caves", OVERGROWN),
        Map.entry("undergroundworlds:spider_cave", OVERGROWN),
        Map.entry("minecraft:nether_wastes", NETHER),
        Map.entry("minecraft:crimson_forest", NETHER),
        Map.entry("minecraft:warped_forest", NETHER),
        Map.entry("minecraft:soul_sand_valley", NETHER),
        Map.entry("minecraft:basalt_deltas", NETHER),
        Map.entry("mms_dungeons:amber_caves", List.of(new Boss("mms_dungeons:wraith", 2, 80))),
        Map.entry("undergroundworlds:underground_tundra", List.of(new Boss("frostiful:frostologer", 1, 0))),
        Map.entry("undergroundworlds:mushroom_cave", List.of(new Boss("mms_dungeons:swampman", 2, 80))),
        Map.entry("undergroundworlds:glowshroom_cave", List.of(new Boss("mms_dungeons:swampman", 2, 80))),
        Map.entry("minecraft:deep_dark", List.of(new Boss("mutantmonsters:mutant_enderman", 1, 0))),
        Map.entry("minecraft:dripstone_caves", GUARDIANS),
        Map.entry("minecraft:sulfur_caves", GUARDIANS),
        Map.entry("undergroundworlds:underground_desert", GUARDIANS),
        Map.entry("undergroundworlds:underground_jungle", GUARDIANS),
        Map.entry("alexscaves:magnetic_caves", GUARDIANS),
        Map.entry("alexscaves:primordial_caves", GUARDIANS),
        Map.entry("alexscaves:toxic_caves", GUARDIANS),
        Map.entry("alexscaves:abyssal_chasm", GUARDIANS),
        Map.entry("alexscaves:forlorn_hollows", GUARDIANS),
        Map.entry("alexscaves:candy_cavity", GUARDIANS),
        Map.entry("mms_dungeons:aerial_hell_cavern", GUARDIANS)
    );

    private DddTrialBoss() {
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
        LevelReader level,
        BlockPos targetPosition,
        BlockPos referencePos,
        BlockPos templateRelativePos,
        StructureTemplate.StructureBlockInfo info,
        StructurePlaceSettings settings
    ) {
        CompoundTag nbt = info.nbt();
        if (nbt == null || !info.state().is(Blocks.TRIAL_SPAWNER) || !summonsUglyOne(nbt)) {
            return info;
        }
        String biome = level.getBiome(targetPosition).unwrapKey().map(key -> key.identifier().toString()).orElse("");
        Boss boss = pick(BY_BIOME.getOrDefault(biome, FALLBACK), targetPosition);
        CompoundTag copy = nbt.copy();
        copy.remove("spawn_data");
        for (String key : new String[] {"normal_config", "ominous_config"}) {
            if (copy.contains(key)) {
                copy.put(key, boss.config(copy.getCompoundOrEmpty(key)));
            }
        }
        return new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), copy);
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return MapCodec.unit(this);
    }

    private static boolean summonsUglyOne(CompoundTag nbt) {
        for (Tag entry : nbt.getCompoundOrEmpty("normal_config").getListOrEmpty("spawn_potentials")) {
            if (entry instanceof CompoundTag weighted) {
                String name = weighted.getCompoundOrEmpty("data").getCompoundOrEmpty("entity").get("CustomName") instanceof Tag tag ? tag.toString() : "";
                if (name.contains(UGLY_ONE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Boss pick(List<Boss> options, BlockPos pos) {
        List<Boss> present = options.stream().filter(Boss::exists).toList();
        if (present.isEmpty()) {
            present = FALLBACK.stream().filter(Boss::exists).toList();
        }
        if (present.isEmpty()) {
            return new Boss("minecraft:ravager", 1, 0);
        }
        return present.get(Math.floorMod((int) Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()), present.size()));
    }

    private record Boss(String id, int count, float health) {

        boolean exists() {
            Identifier key = Identifier.tryParse(id);
            return key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key);
        }

        CompoundTag config(CompoundTag original) {
            CompoundTag config = original.copy();
            CompoundTag entity = new CompoundTag();
            entity.putString("id", id);
            entity.putBoolean("PersistenceRequired", true);
            if (health > 0) {
                CompoundTag maxHealth = new CompoundTag();
                maxHealth.putString("id", "minecraft:max_health");
                maxHealth.putDouble("base", health);
                ListTag attributes = new ListTag();
                attributes.add(maxHealth);
                entity.put("attributes", attributes);
                entity.putFloat("Health", health);
            }
            CompoundTag data = new CompoundTag();
            data.put("entity", entity);
            CompoundTag weighted = new CompoundTag();
            weighted.put("data", data);
            weighted.putInt("weight", 1);
            ListTag potentials = new ListTag();
            potentials.add(weighted);
            config.put("spawn_potentials", potentials);
            config.putFloat("total_mobs", count);
            config.putFloat("simultaneous_mobs", count);
            config.putFloat("total_mobs_added_per_player", 0.0f);
            config.putFloat("simultaneous_mobs_added_per_player", 0.0f);
            return config;
        }
    }
}
