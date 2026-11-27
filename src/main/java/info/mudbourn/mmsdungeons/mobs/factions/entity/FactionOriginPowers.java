package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsorigins.power.CritChancePower;
import info.mudbourn.mmsorigins.power.KeyGesturePower;
import info.mudbourn.mmsorigins.power.PosePower;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.ActionOnItemUsePower;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PreventItemUsePower;
import io.github.apace100.apoli.power.VariableIntPower;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginRegistry;
import net.minecraft.resources.Identifier;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// A faction member's origin as Origins grants it to a player: the origin's real powers through Apoli, and the power keys its AI presses. Only loaded with Origins and MMS Origins.
final class FactionOriginPowers {

    static final String PRIMARY = "key.origins.primary_active";
    static final String SECONDARY = "key.origins.secondary_active";
    static final String SNEAK = "key.sneak";
    private static final Identifier SOURCE = MmsDungeons.id("faction_origin");

    // Foods tried in turn when a rolled food breaks the origin's diet: meat, then plants, then waste.
    private static final Item[] DIET_FOODS = {
        Items.COOKED_BEEF,
        Items.BREAD,
        Items.BAKED_POTATO,
        Items.ROTTEN_FLESH
    };

    private FactionOriginPowers() {
    }

    // Swaps in the powers of the origin, as choosing it does for a player.
    static void grant(LivingEntity mob, FactionOrigin origin) {
        PowerHolderComponent holder = PowerHolderComponent.KEY.get(mob);
        holder.removeAllPowersFromSource(SOURCE);
        Origin found = OriginRegistry.get().get(origin.originId());
        if (found != null) {
            for (PowerType<?> type : found.getPowerTypes()) {
                holder.addPower(type, SOURCE);
            }
        }
        holder.sync();
    }

    static boolean granted(LivingEntity mob) {
        return !PowerHolderComponent.KEY.get(mob).getPowersFromSource(SOURCE).isEmpty();
    }

    // One tick of a key held down: continuous powers and gestures see every tick, the rest only the press.
    static void key(LivingEntity mob, String key, boolean pressed) {
        for (Power power : PowerHolderComponent.KEY.get(mob).getPowers()) {
            if (power instanceof Active active && key.equals(active.getKey().key)
                && (pressed || active.getKey().continuous || power instanceof KeyGesturePower)) {
                active.onUse();
            }
        }
    }

    // A resource power's value, or -1 when the member has no such power.
    static int resource(LivingEntity mob, Identifier id) {
        VariableIntPower power = variable(mob, id);
        return power == null ? -1 : power.getValue();
    }

    static int max(LivingEntity mob, Identifier id) {
        VariableIntPower power = variable(mob, id);
        return power == null ? 0 : power.getMax();
    }

    static void spend(LivingEntity mob, Identifier id, int amount) {
        VariableIntPower power = variable(mob, id);
        if (power != null) {
            power.setValue(Math.max(power.getMin(), power.getValue() - amount));
            PowerHolderComponent.syncPower(mob, power.getType());
        }
    }

    static void set(LivingEntity mob, Identifier id, int value) {
        VariableIntPower power = variable(mob, id);
        if (power != null) {
            power.setValue(Math.clamp(value, power.getMin(), power.getMax()));
            PowerHolderComponent.syncPower(mob, power.getType());
        }
    }

    // Whether the origin's restrictions keep it from using the item, as they keep a player of the origin.
    static boolean forbids(LivingEntity mob, ItemStack stack) {
        for (PreventItemUsePower power : PowerHolderComponent.KEY.get(mob).getPowers(PreventItemUsePower.class)) {
            if (power.isActive() && power.doesPrevent(stack)) {
                return true;
            }
        }
        return false;
    }

    // Swaps each food the diet forbids for one it allows, or drops it when none is allowed, and empties a hand holding a forbidden item.
    static void obey(LivingEntity mob, List<ItemStack> supplies) {
        for (int i = supplies.size() - 1; i >= 0; i--) {
            ItemStack stack = supplies.get(i);
            if (!stack.has(DataComponents.FOOD) || !forbids(mob, stack)) {
                continue;
            }
            ItemStack allowed = ItemStack.EMPTY;
            for (Item food : DIET_FOODS) {
                ItemStack candidate = new ItemStack(food, stack.getCount());
                if (!forbids(mob, candidate)) {
                    allowed = candidate;
                    break;
                }
            }
            if (allowed.isEmpty()) {
                supplies.remove(i);
            } else {
                supplies.set(i, allowed);
            }
        }
        for (EquipmentSlot hand : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            if (forbids(mob, mob.getItemBySlot(hand))) {
                mob.setItemSlot(hand, ItemStack.EMPTY);
            }
        }
    }

    // Runs the origin's powers for an item it finished eating or drinking, such as an enderian's hurt from a potion.
    static void finishUsing(LivingEntity mob, ItemStack used, ItemStack result) {
        ActionOnItemUsePower.executeActions(mob, used, result, ActionOnItemUsePower.TriggerType.FINISH, ActionOnItemUsePower.PriorityPhase.ALL);
    }

    // The pose an active pose power forces, or null when none does.
    static @Nullable Pose forcedPose(LivingEntity mob) {
        for (PosePower power : PowerHolderComponent.KEY.get(mob).getPowers(PosePower.class)) {
            if (power.isActive()) {
                return power.getPose();
            }
        }
        return null;
    }

    // Whether a power, such as a toggle, is on and its conditions hold.
    static boolean active(LivingEntity mob, Identifier id) {
        for (Power power : PowerHolderComponent.KEY.get(mob).getPowers()) {
            if (power.getType().getIdentifier().equals(id)) {
                return power.isActive();
            }
        }
        return false;
    }

    // The origin's critical hit chance by its crit chance powers, as Critical Strike gives it to a player.
    static float critChance(LivingEntity mob) {
        return CritChancePower.totalChance(mob);
    }

    private static @Nullable VariableIntPower variable(LivingEntity mob, Identifier id) {
        for (VariableIntPower power : PowerHolderComponent.KEY.get(mob).getPowers(VariableIntPower.class)) {
            if (power.getType().getIdentifier().equals(id)) {
                return power;
            }
        }
        return null;
    }
}
