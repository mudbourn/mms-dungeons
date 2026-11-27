package info.mudbourn.mmsdungeons.mobs.luminous.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

// A zombie that usually spawns holding a stone, iron or golden pickaxe or shovel.
public class MinerZombie extends Zombie {

    private static final Item[] TOOLS = {
        Items.STONE_PICKAXE,
        Items.IRON_PICKAXE,
        Items.GOLDEN_PICKAXE,
        Items.STONE_SHOVEL,
        Items.IRON_SHOVEL,
        Items.GOLDEN_SHOVEL
    };
    private static final int TOOL_CHANCE_IN_TEN = 9;

    public MinerZombie(EntityType<? extends MinerZombie> type, Level level) {
        super(type, level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (this.random.nextInt(10) < TOOL_CHANCE_IN_TEN) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(TOOLS[this.random.nextInt(TOOLS.length)]));
        }
        return data;
    }
}
