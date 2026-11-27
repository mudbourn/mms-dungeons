package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.tags.EnchantmentTags;
import info.mudbourn.mmsdungeons.MmsDungeons;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
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
    // The share of the defence bar each slot is expected to carry, iron-shaped for the portal and netherite-shaped for the Deep Region.
    private static final Map<EquipmentSlot, Double> ENTRY_SHARE = Map.of(
        EquipmentSlot.HEAD, 2.0,
        EquipmentSlot.CHEST, 6.0,
        EquipmentSlot.LEGS, 5.0,
        EquipmentSlot.FEET, 2.0
    );
    private static final Map<EquipmentSlot, String> SLOT_NAMES = Map.of(
        EquipmentSlot.HEAD, "helmet",
        EquipmentSlot.CHEST, "chestplate",
        EquipmentSlot.LEGS, "leggings",
        EquipmentSlot.FEET, "boots"
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

    // Tells a player in plain words what gear the check wants and which part of their kit falls short.
    public static Component shortfall(ServerPlayer player, boolean deep) {
        Map<EquipmentSlot, Double> share = deep ? PROVEN_FLOOR : ENTRY_SHARE;
        double need = deep ? DEEP_DEFENCE : ENTRY_DEFENCE;
        double shareTotal = share.values().stream().mapToDouble(Double::doubleValue).sum();
        List<String> weakPieces = new ArrayList<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (armorScore(player.getItemBySlot(slot), slot) < need * share.get(slot) / shareTotal) {
                weakPieces.add(SLOT_NAMES.get(slot));
            }
        }
        boolean armorShort = defence(player) < need;
        boolean weaponShort = bestWeapon(player) < (deep ? DEEP_WEAPON : ENTRY_WEAPON);
        StringBuilder text = new StringBuilder(deep ? "The Deep Region is beyond you for now. " : "The gatekeeper notices your effort, and laughs. ");
        text.append(deep
            ? "You need netherite armor, or enchanted diamond armor, and a netherite weapon, or an enchanted diamond one. "
            : "You need at least iron armor and an iron weapon, or better. ");
        if (armorShort) {
            text.append(weakPieces.isEmpty()
                ? "Your armor is not strong enough. "
                : "Your " + String.join(", ", weakPieces) + (weakPieces.size() == 1 ? " falls" : " fall") + " short. ");
        }
        if (weaponShort) {
            text.append("Your best weapon is not strong enough. ");
        }
        text.append(deep
            ? "A mage needs a full set of enchanted netherite-tier robes, an enchanted staff or wand and a spellbook."
            : "A mage needs a full set of tier 2 robes, a staff or wand and a spellbook.");
        return Component.literal(text.toString());
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
