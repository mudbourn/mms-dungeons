package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

// Arms zombies and skeletons that spawn in the dungeon with better gear the deeper they are.
public final class DepthGear {

    private enum Material {
        IRON(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD),
        DIAMOND(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD),
        NETHERITE(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_SWORD);

        final Item[] armor;
        final Item sword;

        Material(Item head, Item chest, Item legs, Item feet, Item sword) {
            this.armor = new Item[] {head, chest, legs, feet};
            this.sword = sword;
        }
    }

    // One depth band: its main armor, how many pieces of it, what fills the other slots, and whether everything is enchanted.
    private record Tier(Material main, int mainPieces, Material filler, boolean fillerEnchanted, boolean enchanted) {
    }

    private static final List<Tier> TIERS = List.of(
        new Tier(Material.IRON, 0, null, false, false),
        new Tier(Material.IRON, 4, null, false, false),
        new Tier(Material.IRON, 4, null, false, true),
        new Tier(Material.DIAMOND, 2, Material.IRON, true, false),
        new Tier(Material.DIAMOND, 4, null, false, false),
        new Tier(Material.DIAMOND, 4, null, false, true),
        new Tier(Material.NETHERITE, 2, Material.DIAMOND, true, false),
        new Tier(Material.NETHERITE, 4, null, false, false),
        new Tier(Material.NETHERITE, 4, null, false, true)
    );
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };
    private static final String GEARED_TAG = "mms_dungeons_geared";
    private static final int TOP = 127;
    private static final int SPAN = 256;
    private static final float DROP_CHANCE = 0.02f;
    private static final int ENCHANT_LEVEL = 30;

    private DepthGear() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DepthGear::onLoad);
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        boolean armable = entity instanceof Zombie || entity instanceof AbstractSkeleton;
        if (!armable || !DungeonDimension.isDungeon(level) || !entity.addTag(GEARED_TAG)) {
            return;
        }
        Mob mob = (Mob) entity;
        int index = Math.clamp((long) (TOP - mob.getBlockY()) * TIERS.size() / SPAN, 0, TIERS.size() - 1);
        equip(mob, level, TIERS.get(index), index);
    }

    private static void equip(Mob mob, ServerLevel level, Tier tier, int index) {
        RandomSource random = mob.getRandom();
        List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(slots, new Random(random.nextLong()));
        int mainPieces = index == 0 ? 1 + random.nextInt(2) : tier.mainPieces();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            boolean isMain = i < mainPieces;
            Material material = isMain ? tier.main() : tier.filler();
            if (material == null) {
                continue;
            }
            boolean enchanted = isMain ? tier.enchanted() : tier.fillerEnchanted();
            give(mob, level, ARMOR_SLOTS[slot], new ItemStack(material.armor[slot]), enchanted);
        }
        if (index == 0) {
            return;
        }
        if (mob instanceof AbstractSkeleton) {
            ItemStack bow = mob.getMainHandItem();
            if (tier.enchanted() && !bow.isEmpty()) {
                give(mob, level, EquipmentSlot.MAINHAND, bow.copy(), true);
            }
        } else if (mob.getMainHandItem().isEmpty()) {
            give(mob, level, EquipmentSlot.MAINHAND, new ItemStack(tier.main().sword), tier.enchanted());
        }
    }

    private static void give(Mob mob, ServerLevel level, EquipmentSlot slot, ItemStack stack, boolean enchanted) {
        if (enchanted) {
            Optional<HolderSet.Named<Enchantment>> pool = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentTags.IN_ENCHANTING_TABLE);
            stack = EnchantmentHelper.enchantItem(mob.getRandom(), stack, ENCHANT_LEVEL, level.registryAccess(), pool);
        }
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, DROP_CHANCE);
    }
}
