package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

// Swaps the loot of chests, barrels, vaults and trial spawners in deepslate ruins for much richer tables.
public final class DddLoot implements StructureProcessor {

    public static final DddLoot INSTANCE = new DddLoot();

    private static final String CHEST = "mms_dungeons:ddd_deepslate/chest";
    private static final String CHEST_RICH = "mms_dungeons:ddd_deepslate/chest_rich";
    private static final String BARREL = "mms_dungeons:ddd_deepslate/barrel";
    private static final String VAULT = "mms_dungeons:ddd_deepslate/vault";
    private static final String TRIAL = "mms_dungeons:ddd_deepslate/trial_reward";

    private DddLoot() {
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
        if (nbt == null) {
            return info;
        }
        CompoundTag copy = nbt.copy();
        boolean changed = false;
        if (info.state().is(Blocks.VAULT)) {
            changed = swapVault(copy);
        } else if (info.state().is(Blocks.TRIAL_SPAWNER)) {
            changed = swapTrial(copy);
        } else if (copy.contains("LootTable")) {
            copy.putString("LootTable", chestTable(copy.getStringOr("LootTable", "")));
            changed = true;
        }
        return changed ? new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), copy) : info;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return MapCodec.unit(this);
    }

    static String chestTable(String original) {
        if (original.contains("_liv3") || original.contains("_liv4") || original.contains("/tower/")) {
            return CHEST_RICH;
        }
        if (original.contains("dungeon_liv") || original.contains("spider_liv")) {
            return CHEST;
        }
        return BARREL;
    }

    private static boolean swapVault(CompoundTag nbt) {
        CompoundTag config = nbt.getCompoundOrEmpty("config");
        if (!config.contains("loot_table")) {
            return false;
        }
        config.putString("loot_table", VAULT);
        nbt.put("config", config);
        return true;
    }

    private static boolean swapTrial(CompoundTag nbt) {
        boolean changed = false;
        for (String key : new String[] {"normal_config", "ominous_config"}) {
            CompoundTag config = nbt.getCompoundOrEmpty(key);
            ListTag ejected = config.getListOrEmpty("loot_tables_to_eject");
            for (Tag entry : ejected) {
                if (entry instanceof CompoundTag weighted && weighted.getStringOr("data", "").startsWith("ddd:")) {
                    weighted.putString("data", TRIAL);
                    changed = true;
                }
            }
            if (changed) {
                nbt.put(key, config);
            }
        }
        return changed;
    }
}
