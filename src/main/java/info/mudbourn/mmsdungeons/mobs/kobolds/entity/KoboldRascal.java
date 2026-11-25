package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldItems;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldSounds;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldTags;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldMeleeGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldPotionGoal;
import info.mudbourn.mmsdungeons.mobs.kobolds.ai.KoboldRevengeGoal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public class KoboldRascal extends AbstractKobold {

    private static final List<Item> HAUL = List.of(
        Items.COAL,
        Items.AMETHYST_SHARD,
        Items.EMERALD,
        Items.RAW_COPPER,
        Items.RAW_IRON,
        Items.RAW_GOLD,
        Items.LAPIS_LAZULI,
        Items.REDSTONE
    );
    private static final int INVISIBLE_TICKS = 24000;
    private static final int HIDING_TICKS_LEFT = 9600;
    private static final float PICKAXE_CHANCE = 0.15f;
    private static final float DIAMOND_CHANCE = 0.02f;
    private static final double CALL_RANGE = 128.0;

    public KoboldRascal(EntityType<KoboldRascal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new KoboldRevengeGoal(this));
        this.goalSelector.addGoal(1, new KoboldPotionGoal(this));
        this.goalSelector.addGoal(2, new KoboldMeleeGoal(this, 1.2, false));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        MobEffectInstance hiding = this.getEffect(MobEffects.INVISIBILITY);
        if (hiding != null && !hiding.endsWithin(HIDING_TICKS_LEFT) && this.level() instanceof ServerLevel level) {
            player.swing(hand, player.getItemInHand(hand).getAttackAnimation(), false);
            this.removeEffect(MobEffects.INVISIBILITY);
            this.swingForAttack(InteractionHand.MAIN_HAND);
            this.playSound(KoboldSounds.TRADE, 1.0f, 1.0f);
            ItemEntity reward = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), this.reward(level));
            reward.setPickUpDelay(10);
            level.addFreshEntity(reward);
        }
        for (AbstractKobold kobold : this.level().getEntitiesOfClass(AbstractKobold.class, this.getBoundingBox().inflate(CALL_RANGE))) {
            if (!kobold.is(this)) {
                this.getNavigation().moveTo(kobold, 1.2);
            }
        }
        return super.mobInteract(player, hand);
    }

    private ItemStack reward(ServerLevel level) {
        if (this.random.nextFloat() < PICKAXE_CHANCE) {
            ItemStack pickaxe = new ItemStack(KoboldItems.KOBOLD_IRON_PICKAXE);
            pickaxe.setDamageValue(Mth.nextInt(this.random, 52, 716));
            int power = Mth.nextInt(this.random, 18, 32);
            EnchantmentHelper.enchantItem(this.random, pickaxe, power, level.registryAccess(), Optional.empty());
            return pickaxe;
        }
        Map<Item, Integer> haul = new LinkedHashMap<>();
        int pulls = Mth.nextInt(this.random, 7, 38);
        for (int i = 0; i < pulls; i++) {
            Item item = this.random.nextFloat() >= 1.0f - DIAMOND_CHANCE ? Items.DIAMOND : HAUL.get(this.random.nextInt(HAUL.size()));
            haul.merge(item, 1, Integer::sum);
        }
        List<ItemStackTemplate> stacks = new ArrayList<>();
        haul.forEach((item, count) -> stacks.add(ItemStackTemplate.fromNonEmptyStack(new ItemStack(item, count))));
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(stacks));
        return bundle;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBLE_TICKS, 0));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack drop, ItemStack held, EquipmentSlot slot) {
        return (drop.is(ItemTags.SWORDS) || drop.is(KoboldTags.ARMOR)) && super.canReplaceCurrentItem(drop, held, slot);
    }
}
