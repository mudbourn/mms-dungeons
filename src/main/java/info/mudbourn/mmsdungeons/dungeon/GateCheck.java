package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.spell_engine.internals.container.SpellContainerSource;

// The gear a player needs to pass a dungeon portal.
public final class GateCheck {

    public static final TagKey<Item> GATE_BOWS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_bows"));
    public static final TagKey<Item> GATE_MAGIC_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_magic_armor"));
    public static final TagKey<Item> GATE_MAGIC_WEAPONS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_magic_weapons"));
    // Armor only found past the dungeon, so wearing it proves the player has been through. It fills its slot on either path.
    public static final TagKey<Item> GATE_PROVEN_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_proven_armor"));

    // Diamond armor points per slot, the floor for the fighter path.
    private static final Map<EquipmentSlot, Double> DIAMOND_ARMOR = Map.of(
        EquipmentSlot.HEAD, 3.0,
        EquipmentSlot.CHEST, 8.0,
        EquipmentSlot.LEGS, 6.0,
        EquipmentSlot.FEET, 3.0
    );
    // Attack damage a diamond sword adds over the bare hand.
    private static final double DIAMOND_ATTACK = 6.0;
    private static final int MIN_BOOK_SPELLS = 2;
    private static final int MIN_STAFF_ENCHANT_LEVEL = 2;

    private GateCheck() {
    }

    public static boolean passes(ServerPlayer player) {
        return passesFighter(player) || passesMage(player);
    }

    private static boolean passesFighter(ServerPlayer player) {
        for (Map.Entry<EquipmentSlot, Double> slot : DIAMOND_ARMOR.entrySet()) {
            ItemStack piece = player.getItemBySlot(slot.getKey());
            boolean strong = piece.isEnchanted() && armorPoints(piece, slot.getKey()) >= slot.getValue();
            if (!strong && !piece.is(GATE_PROVEN_ARMOR)) {
                return false;
            }
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEnchanted() && (stack.is(GATE_BOWS) || attackBonus(stack) >= DIAMOND_ATTACK)) {
                return true;
            }
        }
        return player.getOffhandItem().isEnchanted() && player.getOffhandItem().is(GATE_BOWS);
    }

    private static boolean passesMage(ServerPlayer player) {
        for (EquipmentSlot slot : DIAMOND_ARMOR.keySet()) {
            ItemStack piece = player.getItemBySlot(slot);
            boolean robe = piece.isEnchanted() && piece.is(GATE_MAGIC_ARMOR);
            if (!robe && !piece.is(GATE_PROVEN_ARMOR)) {
                return false;
            }
        }
        return hasStaff(player) && hasBook(player);
    }

    private static boolean hasStaff(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(GATE_MAGIC_WEAPONS) && highestEnchantLevel(stack) >= MIN_STAFF_ENCHANT_LEVEL) {
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(GATE_MAGIC_WEAPONS) && highestEnchantLevel(offhand) >= MIN_STAFF_ENCHANT_LEVEL;
    }

    // Whether any equipped spell source other than a staff or wand holds enough spells to count as a book.
    private static boolean hasBook(ServerPlayer player) {
        for (SpellContainerSource.SourcedContainer source : SpellContainerSource.getSpellsOf(player).sources()) {
            boolean isWeapon = source.itemStack().is(GATE_MAGIC_WEAPONS);
            if (!isWeapon && source.container().spell_ids().size() >= MIN_BOOK_SPELLS) {
                return true;
            }
        }
        return false;
    }

    private static double armorPoints(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0.0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ARMOR)) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static double attackBonus(ItemStack stack) {
        double[] total = {0.0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }

    private static int highestEnchantLevel(ItemStack stack) {
        int highest = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            highest = Math.max(highest, entry.getIntValue());
        }
        return highest;
    }
}
