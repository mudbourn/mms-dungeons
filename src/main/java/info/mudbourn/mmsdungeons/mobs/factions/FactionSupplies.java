package info.mudbourn.mmsdungeons.mobs.factions;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

// The food and healing potions a faction member carries into a fight, better kit for stronger factions and tiers.
public final class FactionSupplies {

    private static final Item[] BANDIT_FOOD = {Items.BREAD, Items.COOKED_PORKCHOP, Items.COOKED_MUTTON};
    private static final Item[] MERCENARY_FOOD = {Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.BAKED_POTATO};
    private static final Item[] GUILDSMAN_FOOD = {Items.COOKED_BEEF, Items.GOLDEN_CARROT};
    private static final float BANDIT_POTION_CHANCE = 0.35f;

    private FactionSupplies() {
    }

    public static List<ItemStack> roll(Faction faction, int tier, RandomSource random) {
        List<ItemStack> supplies = new ArrayList<>();
        switch (faction) {
            case BANDIT -> {
                supplies.add(food(BANDIT_FOOD, random, 1 + random.nextInt(2)));
                if (random.nextFloat() < BANDIT_POTION_CHANCE) {
                    supplies.add(potion(Potions.HEALING));
                }
            }
            case MERCENARY -> {
                supplies.add(food(MERCENARY_FOOD, random, 2));
                supplies.add(potion(Potions.HEALING));
                if (tier >= 3) {
                    supplies.add(new ItemStack(Items.GOLDEN_APPLE));
                }
            }
            case GUILDSMAN -> {
                supplies.add(food(GUILDSMAN_FOOD, random, 2));
                supplies.add(potion(tier >= 3 ? Potions.STRONG_HEALING : Potions.HEALING));
                supplies.add(potion(Potions.HEALING));
                if (tier >= 2) {
                    supplies.add(new ItemStack(Items.GOLDEN_APPLE));
                }
            }
        }
        return supplies;
    }

    // A drink: anything that heals the moment it is consumed, such as a healing potion or a golden apple.
    public static boolean quick(ItemStack stack) {
        return stack.has(DataComponents.POTION_CONTENTS) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private static ItemStack food(Item[] choices, RandomSource random, int count) {
        return new ItemStack(choices[random.nextInt(choices.length)], count);
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }
}
