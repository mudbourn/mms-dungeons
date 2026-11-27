package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionSlots;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionWaterskin;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

// The food, healing potions and weather gear a faction member carries, better kit for stronger factions and tiers.
public final class FactionSupplies {

    private static final Item[] BANDIT_FOOD = {Items.BREAD, Items.COOKED_PORKCHOP, Items.COOKED_MUTTON};
    private static final Item[] MERCENARY_FOOD = {Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.BAKED_POTATO};
    private static final Item[] GUILDSMAN_FOOD = {Items.COOKED_BEEF, Items.GOLDEN_CARROT};
    private static final float BANDIT_POTION_CHANCE = 0.35f;
    private static final Identifier WATER_SKIN = Identifier.fromNamespaceAndPath("scorchful", "water_skin");
    // Base temperatures of the hot (desert, badlands, savanna) and cold (snowy, taiga, peaks) biomes.
    private static final float HOT_BIOME = 1.5f;
    private static final float COLD_BIOME = 0.25f;
    private static final float HOT_BUCKET_CHANCE = 0.4f;
    private static final int NETHER_SKINS = 2;
    private static final int NETHER_CAULDRONS = 2;

    public static final boolean SKINS = FabricLoader.getInstance().isModLoaded("scorchful");

    private FactionSupplies() {
    }

    public static List<ItemStack> roll(Faction faction, int tier, RandomSource random, ServerLevelAccessor level, BlockPos pos) {
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
        supplies.addAll(climateKit(level, pos, random));
        return supplies;
    }

    // Gear for the place it spawns: skins and cauldron stock in the Nether, a skin and maybe a bucket in hot biomes, a lantern in cold ones.
    private static List<ItemStack> climateKit(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        List<ItemStack> kit = new ArrayList<>();
        if (level.getLevel().dimension() == Level.NETHER) {
            if (SKINS) {
                addSkins(kit, NETHER_SKINS);
                kit.add(new ItemStack(Items.CAULDRON, NETHER_CAULDRONS));
            }
            return kit;
        }
        float temperature = level.getBiome(pos).value().getBaseTemperature();
        if (temperature >= HOT_BIOME && SKINS) {
            addSkins(kit, 1);
            if (random.nextFloat() < HOT_BUCKET_CHANCE) {
                kit.add(new ItemStack(Items.WATER_BUCKET));
            }
        } else if (temperature <= COLD_BIOME && FactionSlots.belt() != null) {
            kit.add(new ItemStack(Items.LANTERN));
        }
        return kit;
    }

    private static void addSkins(List<ItemStack> kit, int count) {
        for (int i = 0; i < count; i++) {
            kit.add(FactionWaterskin.filled());
        }
    }

    // A Scorchful waterskin, full or not.
    public static boolean skin(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(WATER_SKIN);
    }

    public static boolean lantern(ItemStack stack) {
        return stack.is(Items.LANTERN);
    }

    // Food that is eaten for its nutrition, not a quick heal.
    public static boolean meal(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && !quick(stack);
    }

    // A drink: anything that heals the moment it is consumed, such as a healing potion or a golden apple.
    public static boolean quick(ItemStack stack) {
        return !water(stack) && (stack.has(DataComponents.POTION_CONTENTS) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE));
    }

    // A plain water bottle, which a floran drinks for sun.
    public static boolean water(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER);
    }

    public static ItemStack water(int count) {
        ItemStack bottle = potion(Potions.WATER);
        bottle.setCount(count);
        return bottle;
    }

    private static ItemStack food(Item[] choices, RandomSource random, int count) {
        return new ItemStack(choices[random.nextInt(choices.length)], count);
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }
}
