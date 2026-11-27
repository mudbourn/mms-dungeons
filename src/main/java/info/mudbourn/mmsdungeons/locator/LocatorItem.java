package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

// Opens the tuning screen with sneak and use, switches the device on and off with use, and cycles its focus.
public final class LocatorItem extends Item {

    private static final int TOGGLE_COOLDOWN = 10;
    private static final String SET = "LocatorSet";
    private static final String FOCUS = "LocatorFocus";
    private static final String PICK_KIND = "Kind";
    private static final String PICK_ID = "Id";
    private static final String SELECTED = "LocatorSelected";
    private static final String SELECTED_KIND = "LocatorSelectedKind";
    private static final String SELECTED_ID = "LocatorSelectedId";
    private static final String FOUND_KIND = "LocatorFoundKind";
    private static final String FOUND_ID = "LocatorFoundId";
    private static final String ON = "LocatorOn";
    private static final String X = "LocatorX";
    private static final String Y = "LocatorY";
    private static final String Z = "LocatorZ";

    public LocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        // In the off hand the device only listens: clicks never switch, tune or cycle it.
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            LocatorNetworking.open(player);
        } else if (isOn(stack)) {
            LocatorSessions.stop(player);
            setOn(stack, false);
            LocatorSounds.play(player, LocatorSounds.TOGGLE);
            player.getCooldowns().addCooldown(stack, TOGGLE_COOLDOWN);
        } else {
            turnOn(serverLevel, player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player) || !isOn(stack) || LocatorSessions.has(player)) {
            return;
        }
        if (level.dimension() != DungeonDimension.LEVEL) {
            LocatorSessions.startStatic(player, focused(stack));
            return;
        }
        BlockPos found = foundPos(stack);
        LocatorTarget target = found(stack);
        if (found == null || target == null) {
            setOn(stack, false);
            return;
        }
        LocatorSessions.resumeLocked(player, target, found);
    }

    static void select(ServerPlayer player, ItemStack stack, List<LocatorTarget> targets) {
        LocatorSessions.stop(player);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag list = new ListTag();
            for (LocatorTarget target : targets) {
                CompoundTag entry = new CompoundTag();
                entry.putString(PICK_KIND, target.kind().name());
                entry.putString(PICK_ID, target.id().toString());
                list.add(entry);
            }
            tag.put(SET, list);
            tag.remove(SELECTED);
            tag.remove(SELECTED_KIND);
            tag.remove(SELECTED_ID);
            tag.putInt(FOCUS, -1);
            tag.putBoolean(ON, false);
            clearFound(tag);
        });
    }

    static void cycle(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Locators.LOCATOR) || !isOn(stack) || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        List<LocatorTarget> targets = selection(stack);
        if (targets.size() < 2) {
            player.sendSystemMessage(targets.isEmpty() ? Component.translatable("locator.mms_dungeons.no_target") : targets.get(0).label(), true);
            return;
        }
        int next = focus(stack) + 1 >= targets.size() ? -1 : focus(stack) + 1;
        LocatorSessions.stop(player);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(FOCUS, next);
            clearFound(tag);
        });
        LocatorSounds.play(player, LocatorSounds.TOGGLE);
        player.getCooldowns().addCooldown(stack, TOGGLE_COOLDOWN);
        player.sendSystemMessage(focusLabel(stack), true);
        if (player.level() instanceof ServerLevel level && level.dimension() == DungeonDimension.LEVEL) {
            LocatorSessions.startSearch(player, level, focused(stack));
        } else {
            LocatorSessions.startStatic(player, focused(stack));
        }
    }

    public static boolean isOn(ItemStack stack) {
        return tag(stack).getBooleanOr(ON, false);
    }

    static void setOn(ItemStack stack, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(ON, on);
            if (!on) {
                clearFound(tag);
            }
        });
    }

    static void setFound(ItemStack stack, LocatorTarget target, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(FOUND_KIND, target.kind().name());
            tag.putString(FOUND_ID, target.id().toString());
            tag.putInt(X, pos.getX());
            tag.putInt(Y, pos.getY());
            tag.putInt(Z, pos.getZ());
        });
    }

    static InteractionHand activeHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Locators.LOCATOR) && isOn(stack)) {
                return hand;
            }
        }
        return null;
    }

    static void turnOffAll(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Locators.LOCATOR) && isOn(stack)) {
                setOn(stack, false);
            }
        }
    }

    // Drops the found position from every switched-on locator, keeping them on.
    static void clearLocks(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Locators.LOCATOR) && isOn(stack)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, LocatorItem::clearFound);
            }
        }
    }

    private static void turnOn(ServerLevel level, ServerPlayer player, ItemStack stack) {
        List<LocatorTarget> targets = focused(stack);
        LocatorSounds.play(player, LocatorSounds.TOGGLE);
        player.getCooldowns().addCooldown(stack, TOGGLE_COOLDOWN);
        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("locator.mms_dungeons.no_target"), true);
        } else if (level.dimension() != DungeonDimension.LEVEL) {
            setOn(stack, true);
            player.sendSystemMessage(Component.translatable("locator.mms_dungeons.no_signal"), true);
            LocatorSessions.startStatic(player, targets);
        } else {
            setOn(stack, true);
            LocatorSessions.startSearch(player, level, targets);
        }
    }

    static List<LocatorTarget> selection(ItemStack stack) {
        CompoundTag tag = tag(stack);
        List<LocatorTarget> out = new ArrayList<>();
        if (tag.contains(SET)) {
            for (Tag entry : tag.getListOrEmpty(SET)) {
                if (entry instanceof CompoundTag compound) {
                    LocatorTarget target = named(compound, PICK_KIND, PICK_ID);
                    if (target != null) {
                        out.add(target);
                    }
                }
            }
            return out;
        }
        LocatorTarget legacy = named(tag, SELECTED_KIND, SELECTED_ID);
        if (legacy == null && tag.contains(SELECTED)) {
            legacy = Locators.TARGETS.get(Math.floorMod(tag.getIntOr(SELECTED, 0), Locators.TARGETS.size()));
        }
        if (legacy != null) {
            out.add(legacy);
        }
        return out;
    }

    static int focus(ItemStack stack) {
        int focus = tag(stack).getIntOr(FOCUS, -1);
        return focus >= selection(stack).size() ? -1 : focus;
    }

    static List<LocatorTarget> focused(ItemStack stack) {
        List<LocatorTarget> targets = selection(stack);
        int focus = focus(stack);
        return focus < 0 ? targets : List.of(targets.get(focus));
    }

    static Component focusLabel(ItemStack stack) {
        int focus = focus(stack);
        return focus < 0 ? Component.translatable("locator.mms_dungeons.nearest") : selection(stack).get(focus).label();
    }

    private static LocatorTarget found(ItemStack stack) {
        return named(tag(stack), FOUND_KIND, FOUND_ID);
    }

    private static BlockPos foundPos(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains(X) ? new BlockPos(tag.getIntOr(X, 0), tag.getIntOr(Y, 0), tag.getIntOr(Z, 0)) : null;
    }

    private static LocatorTarget named(CompoundTag tag, String kindKey, String idKey) {
        LocatorTarget.Kind kind = LocatorTarget.Kind.byName(tag.getStringOr(kindKey, ""));
        Identifier id = Identifier.tryParse(tag.getStringOr(idKey, ""));
        return kind == null || id == null ? null : LocatorTarget.single(kind, id);
    }

    private static void clearFound(CompoundTag tag) {
        tag.remove(FOUND_KIND);
        tag.remove(FOUND_ID);
        tag.remove(X);
        tag.remove(Y);
        tag.remove(Z);
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
