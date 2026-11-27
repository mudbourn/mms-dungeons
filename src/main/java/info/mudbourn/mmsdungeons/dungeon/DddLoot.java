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

// Swaps the loot of chests, barrels, vaults and trial spawners in Deep and Sanctuary labyrinths for the tables of their tier.
public final class DddLoot implements StructureProcessor {

    public static final DddLoot DEEP = new DddLoot("ddd_deepslate");
    public static final DddLoot SANCTUARY = new DddLoot("ddd_sanctuary");

    private final String chest;
    private final String chestRich;
    private final String barrel;
    private final String vault;
    private final String trial;

    private DddLoot(String folder) {
        String base = "mms_dungeons:" + folder + "/";
        this.chest = base + "chest";
        this.chestRich = base + "chest_rich";
        this.barrel = base + "barrel";
        this.vault = base + "vault";
        this.trial = base + "trial_reward";
    }

    public static DddLoot of(DddRuin ruin) {
        return ruin.sanctuary() ? SANCTUARY : DEEP;
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

    String chestTable(String original) {
        if (original.contains("_liv3") || original.contains("_liv4") || original.contains("/tower/")) {
            return chestRich;
        }
        if (original.contains("dungeon_liv") || original.contains("spider_liv")) {
            return chest;
        }
        return barrel;
    }

    private boolean swapVault(CompoundTag nbt) {
        CompoundTag config = nbt.getCompoundOrEmpty("config");
        if (!config.contains("loot_table")) {
            return false;
        }
        config.putString("loot_table", vault);
        nbt.put("config", config);
        return true;
    }

    private boolean swapTrial(CompoundTag nbt) {
        boolean changed = false;
        for (String key : new String[] {"normal_config", "ominous_config"}) {
            CompoundTag config = nbt.getCompoundOrEmpty(key);
            ListTag ejected = config.getListOrEmpty("loot_tables_to_eject");
            for (Tag entry : ejected) {
                if (entry instanceof CompoundTag weighted && weighted.getStringOr("data", "").startsWith("ddd:")) {
                    weighted.putString("data", trial);
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
