package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KoboldChild extends AbstractKobold {

    private static final int CHILD_AGE = -24000;
    private static final float CAPTAIN_CHANCE = 0.06f;
    private static final float ENGINEER_CHANCE = 0.05f;
    private static final float ENCHANTER_CHANCE = 0.1f;

    public KoboldChild(EntityType<KoboldChild> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    // No baby shrink on the hitbox.
    @Override
    public float getAgeScale() {
        return 1.0f;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.2));
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.hasCustomName()) {
            this.setAge(CHILD_AGE);
        } else if (this.getAge() >= 0 && this.level() instanceof ServerLevel level) {
            BlockPos pos = this.blockPosition();
            this.discard();
            this.growUp(level, pos);
        }
    }

    private void growUp(ServerLevel level, BlockPos pos) {
        float roll = this.random.nextFloat();
        EntityType<? extends AbstractKobold> grown;
        if (level.getBiome(pos).is(BiomeTags.IS_JUNGLE)) {
            grown = roll < CAPTAIN_CHANCE ? KoboldMobs.KOBOLD_CAPTAIN : KoboldMobs.KOBOLD_PIRATE;
        } else if (this.random.nextFloat() > 1.0f - ENGINEER_CHANCE) {
            grown = KoboldMobs.KOBOLD_ENGINEER;
        } else if (roll < ENCHANTER_CHANCE) {
            grown = KoboldMobs.KOBOLD_ENCHANTER;
        } else {
            grown = KoboldMobs.KOBOLD;
        }
        grown.spawn(level, pos, EntitySpawnReason.BREEDING);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.AMETHYST_SHARD)) {
            held.consume(1, player);
            this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}
