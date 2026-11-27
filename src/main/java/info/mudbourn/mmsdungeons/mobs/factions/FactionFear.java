package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.GateCheck;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Scores a creature's kit with the gear check's measures, so a faction human can tell a stronger player from a weaker one.
public final class FactionFear {

    private static final int HOTBAR = 9;
    static final TagKey<Item> ELITE_ARMOR = TagKey.create(Registries.ITEM, MmsDungeons.id("fear_elite_armor"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private FactionFear() {
    }

    // Whether the player's kit beats the human's, which makes the human back off until attacked.
    public static boolean outgears(LivingEntity human, Player player) {
        return score(player) > score(human);
    }

    private static double score(LivingEntity wearer) {
        return GateCheck.defence(wearer) + elite(wearer) + weapon(wearer);
    }

    // Elite armor, such as Aerial Hell's phoenix set, counts for twice a netherite piece in its slot.
    private static double elite(LivingEntity wearer) {
        double bonus = 0.0;
        for (EquipmentSlot slot : ARMOR) {
            if (wearer.getItemBySlot(slot).is(ELITE_ARMOR)) {
                bonus += GateCheck.provenFloor(slot);
            }
        }
        return bonus;
    }

    private static double weapon(LivingEntity wearer) {
        double best = Math.max(GateCheck.weaponScore(wearer.getMainHandItem()), GateCheck.weaponScore(wearer.getOffhandItem()));
        if (wearer instanceof Player player) {
            for (int slot = 0; slot < HOTBAR; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                best = Math.max(best, GateCheck.weaponScore(stack));
            }
        }
        return best;
    }
}
