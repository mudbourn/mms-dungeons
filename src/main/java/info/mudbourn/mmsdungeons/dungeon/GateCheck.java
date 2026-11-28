package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.tags.EnchantmentTags;
import info.mudbourn.mmsdungeons.MmsDungeons;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.enchantment.Enchantment;
import net.spell_engine.internals.container.SpellContainerSource;

// The gear a player needs to pass a dungeon portal, and the stronger gear needed to go down into the Deep Region.
// Fighters are judged on their kit's defence and weapon stats, so any armor or weapon counts, modded or not.
// Mages still need class gear: tier-2 or better for the portal, enchanted netherite-tier for the Deep Region.
public final class GateCheck {

    public static final TagKey<Item> GATE_BOWS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_bows"));
    public static final TagKey<Item> GATE_MAGIC_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_magic_armor"));
    public static final TagKey<Item> GATE_MAGIC_WEAPONS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_magic_weapons"));
    // Armor only found past the dungeon, so wearing it proves the player has been through. It fills its slot on either path.
    // Tier-2 class gear and up, enough to pass the portal.
    public static final TagKey<Item> ENTRY_MAGIC_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_entry_magic_armor"));
    public static final TagKey<Item> ENTRY_MAGIC_WEAPONS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_entry_magic_weapons"));
    // Weapons of the same materials, which meet the Deep Region's weapon bar however they are enchanted.
    public static final TagKey<Item> GATE_PROVEN_WEAPONS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_proven_weapons"));
    public static final TagKey<Item> GATE_PROVEN_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_proven_armor"));

    // Fighter path, judged on the stats of the kit rather than item IDs so any armor and weapon counts.
    // Defence is armor plus armor toughness plus the levels of protection-type enchantments, summed over the worn
    // pieces: about 15 for iron, 28 for diamond and 32 for netherite. The weapon score is the best weapon carried:
    // a melee weapon's attack damage bonus plus its Sharpness-type damage, or 6 plus the highest enchantment level
    // for a bow or crossbow.
    private static final double ENTRY_DEFENCE = 15.0;
    private static final double ENTRY_WEAPON = 5.0;
    public static final double DEEP_DEFENCE = 30.0;
    public static final double DEEP_WEAPON = 7.0;
    // Blocks around the player searched for weapons they threw.
    private static final double THROWN_REACH = 64.0;
    private static final double RANGED_BASE = 6.0;
    // A proven armor piece counts for at least a netherite piece in its slot, toughness included.
    private static final Map<EquipmentSlot, Double> PROVEN_FLOOR = Map.of(
        EquipmentSlot.HEAD, 6.0,
        EquipmentSlot.CHEST, 11.0,
        EquipmentSlot.LEGS, 9.0,
        EquipmentSlot.FEET, 6.0
    );
    private static final int MIN_BOOK_SPELLS = 2;
    private static final int MIN_STAFF_ENCHANT_LEVEL = 2;

    private GateCheck() {
    }

    // Whether the player may pass a dungeon portal.
    public static boolean passes(ServerPlayer player) {
        return passesFighter(player, ENTRY_DEFENCE, ENTRY_WEAPON)
            || passesMage(player, ENTRY_MAGIC_ARMOR, ENTRY_MAGIC_WEAPONS, 0);
    }

    // Whether the player may go below the Sanctuary Region into the Deep Region.
    public static boolean passesDeep(ServerPlayer player) {
        return passesFighter(player, DEEP_DEFENCE, DEEP_WEAPON)
            || passesMage(player, GATE_MAGIC_ARMOR, GATE_MAGIC_WEAPONS, MIN_STAFF_ENCHANT_LEVEL);
    }

    private static boolean passesFighter(ServerPlayer player, double defence, double weapon) {
        return defence(player) >= defence && bestWeapon(player) >= weapon;
    }

    // The line a player turned back by the check sees.
    public static Component shortfall(boolean deep) {
        return Component.literal(deep ? "The Deep Region is beyond you for now." : "The gatekeeper notices your effort, and laughs.");
    }

    // The least a proven armor piece counts for in its slot: a netherite piece, toughness included.
    public static double provenFloor(EquipmentSlot slot) {
        return PROVEN_FLOOR.getOrDefault(slot, 0.0);
    }

    public static double defence(LivingEntity wearer) {
        double total = 0.0;
        for (EquipmentSlot slot : PROVEN_FLOOR.keySet()) {
            total += armorScore(wearer.getItemBySlot(slot), slot);
        }
        return total;
    }

    public static double armorScore(ItemStack piece, EquipmentSlot slot) {
        double score = attribute(piece, slot, Attributes.ARMOR) + attribute(piece, slot, Attributes.ARMOR_TOUGHNESS)
            + enchantLevels(piece, EnchantmentTags.ARMOR_EXCLUSIVE);
        return piece.is(GATE_PROVEN_ARMOR) ? Math.max(score, PROVEN_FLOOR.get(slot)) : score;
    }

    public static double bestWeapon(ServerPlayer player) {
        double best = weaponScore(player.getOffhandItem());
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            best = Math.max(best, weaponScore(stack));
        }
        AABB around = player.getBoundingBox().inflate(THROWN_REACH);
        for (AbstractArrow thrown : player.level().getEntitiesOfClass(AbstractArrow.class, around, arrow -> arrow.getOwner() == player)) {
            best = Math.max(best, weaponScore(thrown.getPickupItemStackOrigin()));
        }
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, around, item -> item.getOwner() == player)) {
            best = Math.max(best, weaponScore(item.getItem()));
        }
        return best;
    }

    public static double weaponScore(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        double floor = stack.is(GATE_PROVEN_WEAPONS) ? DEEP_WEAPON : 0.0;
        return Math.max(floor, statScore(stack));
    }

    private static double statScore(ItemStack stack) {
        if (stack.getItem() instanceof ProjectileWeaponItem) {
            return RANGED_BASE + highestEnchantLevel(stack);
        }
        double bonus = attribute(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
        int sharpness = maxEnchantLevel(stack, EnchantmentTags.DAMAGE_EXCLUSIVE);
        return bonus + (sharpness > 0 ? 0.5 * sharpness + 0.5 : 0.0);
    }

    private static boolean passesMage(ServerPlayer player, TagKey<Item> robes, TagKey<Item> staves, int staffEnchantLevel) {
        boolean enchanted = staffEnchantLevel > 0;
        for (EquipmentSlot slot : PROVEN_FLOOR.keySet()) {
            ItemStack piece = player.getItemBySlot(slot);
            boolean robe = (!enchanted || piece.isEnchanted()) && piece.is(robes);
            if (!robe && !piece.is(GATE_PROVEN_ARMOR)) {
                return false;
            }
        }
        return hasStaff(player, staves, staffEnchantLevel) && hasBook(player);
    }

    private static boolean hasStaff(ServerPlayer player, TagKey<Item> staves, int minEnchantLevel) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(staves) && highestEnchantLevel(stack) >= minEnchantLevel) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(staves) && highestEnchantLevel(offhand) >= minEnchantLevel;
    }

    // Whether any equipped spell source other than a staff or wand holds enough spells to count as a book.
    private static boolean hasBook(ServerPlayer player) {
        for (SpellContainerSource.SourcedContainer source : SpellContainerSource.getSpellsOf(player).sources()) {
            boolean isWeapon = source.itemStack().is(ENTRY_MAGIC_WEAPONS);
            if (!isWeapon && source.container().spell_ids().size() >= MIN_BOOK_SPELLS) {
                return true;
            }
        }
        return false;
    }



    private static double attribute(ItemStack stack, EquipmentSlot slot, Holder<net.minecraft.world.entity.ai.attributes.Attribute> wanted) {
        double[] total = {0.0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(wanted) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static int enchantLevels(ItemStack stack, TagKey<Enchantment> tag) {
        int total = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(tag)) {
                total += entry.getIntValue();
            }
        }
        return total;
    }

    private static int maxEnchantLevel(ItemStack stack, TagKey<Enchantment> tag) {
        int highest = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(tag)) {
                highest = Math.max(highest, entry.getIntValue());
            }
        }
        return highest;
    }

    private static int highestEnchantLevel(ItemStack stack) {
        int highest = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            highest = Math.max(highest, entry.getIntValue());
        }
        return highest;
    }
}
