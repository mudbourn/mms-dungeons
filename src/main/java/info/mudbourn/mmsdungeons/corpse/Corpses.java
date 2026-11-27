package info.mudbourn.mmsdungeons.corpse;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

// Forces keepInventory, turns Periculum deaths into saved corpses with a world marker, lets owners loot their body, and registers the recovery cross and the mob corpses.
public final class Corpses {

    private static final int SETTLE_INTERVAL = 20;
    private static final double SUMMON_DISTANCE = 2.0;

    public static Item RECOVERY_CROSS;

    private static CorpseData data;

    private Corpses() {
    }

    public static void register() {
        CorpseMarkers.register();
        MobCorpses.register();
        Identifier id = MmsDungeons.id("recovery_cross");
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        RECOVERY_CROSS = Registry.register(
            BuiltInRegistries.ITEM,
            id,
            new RecoveryCrossItem(new Item.Properties().stacksTo(1).setId(key)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(output -> output.accept(RECOVERY_CROSS));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            data = server.overworld().getDataStorage().computeIfAbsent(CorpseData.TYPE);
            server.getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> RecoveryCrossItem.registerCommand(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> data = null);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % SETTLE_INTERVAL == 0) {
                server.getAllLevels().forEach(CorpseMarkers::settle);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && DungeonDimension.isPericulum(player.level(), player.blockPosition())) {
                bury(player);
            }
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof Mannequin) || !entity.entityTags().contains(CorpseMarkers.TAG)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer owner && hand == InteractionHand.MAIN_HAND) {
                loot(owner, entity);
            }
            return InteractionResult.SUCCESS;
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Mannequin && entity.entityTags().contains(CorpseMarkers.TAG)) {
                UUID owner = CorpseMarkers.ownerOf(entity);
                CorpseData.Corpse record = data == null ? null : data.byMarker(entity.getUUID());
                if (record == null || owner == null || !owner.equals(record.owner)) {
                    entity.discard();
                }
            }
        });
    }

    // Hands the owner this body's items, dropping what does not fit, and removes the emptied body.
    private static void loot(ServerPlayer player, Entity marker) {
        UUID owner = CorpseMarkers.ownerOf(marker);
        if (!player.getUUID().equals(owner)) {
            player.sendSystemMessage(Component.literal("This is not your body."), true);
            return;
        }
        CorpseData.Corpse corpse = data == null ? null : data.byMarker(marker.getUUID());
        if (corpse == null || !owner.equals(corpse.owner)) {
            return;
        }
        data.remove(corpse);
        marker.discard();
        for (ItemStack stack : corpse.items) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.spawnAtLocation((ServerLevel) player.level(), stack);
            }
        }
        player.sendSystemMessage(Component.literal("Recovered belongings"), true);
    }

    // Whether the player may move this entity: anything but a corpse that belongs to someone else.
    public static boolean mayHandle(Player player, Entity entity) {
        if (MobCorpses.isCorpse(entity)) {
            return false;
        }
        if (!(entity instanceof Mannequin) || !entity.entityTags().contains(CorpseMarkers.TAG)) {
            return true;
        }
        return player.getUUID().equals(CorpseMarkers.ownerOf(entity));
    }

    // Moves the player's whole inventory into their corpse record and stands a marker at the death spot.
    private static void bury(ServerPlayer player) {
        if (data == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<ItemStack> taken = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (taken.isEmpty()) {
            return;
        }
        CorpseData.Corpse corpse = data.add(
            player.getUUID(),
            taken,
            level.dimension().identifier(),
            player.blockPosition(),
            System.currentTimeMillis());
        Mannequin marker = CorpseMarkers.create(player, level, player.position(), player.yBodyRot);
        corpse.marker = marker.getUUID();
        level.addFreshEntity(marker);
    }

    // The player's corpses, newest first.
    static List<CorpseData.Corpse> corpsesOf(UUID owner) {
        return data == null ? List.of() : data.of(owner);
    }

    // Moves a corpse's body and marker to stand in front of the player, in whatever dimension they are in.
    static void summon(ServerPlayer player, CorpseData.Corpse corpse) {
        ServerLevel level = (ServerLevel) player.level();
        CorpseMarkers.remove(level.getServer(), corpse);
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 at = player.position().add(look.x * SUMMON_DISTANCE, 0.0, look.z * SUMMON_DISTANCE);
        Mannequin marker = CorpseMarkers.create(player, level, at, player.getYRot());
        corpse.marker = marker.getUUID();
        corpse.dimension = level.dimension().identifier();
        corpse.pos = marker.blockPosition();
        data.touch();
        level.addFreshEntity(marker);
    }

    static CorpseData.Corpse corpse(UUID id) {
        return data == null ? null : data.byId(id);
    }
}
