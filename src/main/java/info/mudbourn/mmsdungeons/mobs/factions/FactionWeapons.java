package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import java.util.ArrayList;
import java.util.List;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// Reads a held weapon's reach, handedness, poses and attacks from Better Combat, with plain fallbacks when it is absent.
public final class FactionWeapons {

    private static final boolean BETTER_COMBAT = FabricLoader.getInstance().isModLoaded("bettercombat");
    private static final double DEFAULT_REACH = 2.5;
    private static final double INTERACTION_RANGE = 3.0;

    // One Better Combat attack: the attack itself, its reach, and the share of the swing that passes before the hit lands.
    public record Swing(WeaponAttributes.Attack attack, double range, float upswing, boolean twoHanded) {
    }

    private FactionWeapons() {
    }

    // The Better Combat attack for this step of the combo with the weapon in the given hand, or null without one.
    public static @Nullable Swing swing(ItemStack stack, ItemStack other, boolean offHand, int combo) {
        if (!BETTER_COMBAT) {
            return null;
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null || attributes.attacks() == null) {
            return null;
        }
        List<WeaponAttributes.Attack> usable = new ArrayList<>();
        for (WeaponAttributes.Attack attack : attributes.attacks()) {
            if (attack.animation() != null && !attack.animation().isEmpty() && allowed(attack, stack, other, offHand)) {
                usable.add(attack);
            }
        }
        if (usable.isEmpty()) {
            return null;
        }
        WeaponAttributes.Attack pick = usable.get(Math.floorMod(combo, usable.size()));
        double range = PlayerAttackHelper.combineAttackRange(attributes, INTERACTION_RANGE) * rangeMultiplier(pick);
        float upswing = (float) Mth.clamp(pick.upswing(), 0.0, 1.0) * BetterCombatMod.config.getUpswingMultiplier();
        return new Swing(pick, range, upswing, attributes.isTwoHanded());
    }

    private static double rangeMultiplier(WeaponAttributes.Attack attack) {
        return attack.rangeMultiplier() > 0.0f ? attack.rangeMultiplier() : 1.0;
    }

    private static boolean allowed(WeaponAttributes.Attack attack, ItemStack stack, ItemStack other, boolean offHand) {
        if (attack.conditions() == null) {
            return true;
        }
        boolean shield = other.has(DataComponents.BLOCKS_ATTACKS);
        boolean dual = !other.isEmpty() && !shield;
        for (WeaponAttributes.Condition condition : attack.conditions()) {
            boolean met = switch (condition) {
                case NOT_MOUNTED -> true;
                case MAIN_HAND_ONLY -> !offHand;
                case OFF_HAND_ONLY -> offHand;
                case MOUNTED -> false;
                case NOT_DUAL_WIELDING -> !dual;
                case DUAL_WIELDING_ANY -> dual;
                case DUAL_WIELDING_SAME, DUAL_WIELDING_SAME_CATEGORY -> dual && other.is(stack.getItem());
                case NO_OFFHAND_ITEM -> other.isEmpty();
                case OFF_HAND_SHIELD -> shield;
            };
            if (!met) {
                return false;
            }
        }
        return true;
    }

    // How far the weapon reaches, as Better Combat measures it for a player.
    public static double reach(ItemStack stack) {
        if (BETTER_COMBAT) {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
            if (attributes != null) {
                return PlayerAttackHelper.combineAttackRange(attributes, INTERACTION_RANGE);
            }
        }
        return DEFAULT_REACH;
    }

    public static boolean heavy(ItemStack stack) {
        if (BETTER_COMBAT) {
            WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
            if (attributes != null) {
                return attributes.isTwoHanded();
            }
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("hammer") || path.contains("claymore");
    }

    // The Better Combat idle pose for a weapon held in the given hand, or an empty string without one.
    public static String pose(ItemStack stack, boolean offHand) {
        if (!BETTER_COMBAT || stack.isEmpty()) {
            return "";
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null) {
            return "";
        }
        String pose = offHand ? attributes.offHandPose() : attributes.pose();
        return pose == null ? "" : pose;
    }

    // The arm pose for the given swing of the combo when Better Combat has no animation to play.
    public static byte style(int combo) {
        return combo % 2 == 0 ? FactionMember.ACTION_SLASH : FactionMember.ACTION_SLASH_LEFT;
    }
}
