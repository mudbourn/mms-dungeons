package info.mudbourn.mmsdungeons.corpse;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.server.MinecraftServer;
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

// Forces keepInventory, turns Periculum deaths into saved corpses with a world marker, lets owners loot their body, and registers the recovery cross.
public final class Corpses {

    private static final int SETTLE_INTERVAL = 20;

    public static Item RECOVERY_CROSS;

    private static CorpseData data;

    private Corpses() {
    }

    public static void register() {
        CorpseMarkers.register();
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
                if (data == null || owner == null || !data.ownsMarker(owner, entity.getUUID())) {
                    entity.discard();
                }
            }
        });
    }

    // Hands the owner their corpse's items, dropping what does not fit at the body.
    private static void loot(ServerPlayer player, Entity marker) {
        UUID owner = CorpseMarkers.ownerOf(marker);
        if (!player.getUUID().equals(owner)) {
            player.sendSystemMessage(Component.literal("This is not your body."), true);
            return;
        }
        if (data == null || !data.ownsMarker(owner, marker.getUUID())) {
            return;
        }
        CorpseData.Corpse corpse = take(player.level().getServer(), owner);
        if (corpse == null) {
            return;
        }
        for (ItemStack stack : corpse.items) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.spawnAtLocation((ServerLevel) player.level(), stack);
            }
        }
        player.sendSystemMessage(Component.literal("Recovered belongings"), true);
    }

    // Whether the player may move this entity: anything but a corpse that belongs to someone else.
    public static boolean mayHandle(Player player, Entity entity) {
        if (!(entity instanceof Mannequin) || !entity.entityTags().contains(CorpseMarkers.TAG)) {
            return true;
        }
        return player.getUUID().equals(CorpseMarkers.ownerOf(entity));
    }

    static CorpseData data() {
        return data;
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
        CorpseData.Corpse corpse = data.getOrCreate(player.getUUID(), level.dimension().identifier());
        CorpseMarkers.remove(level.getServer(), corpse);
        corpse.items.addAll(taken);
        Mannequin marker = CorpseMarkers.create(player, level);
        corpse.marker = marker.getUUID();
        corpse.dimension = level.dimension().identifier();
        level.addFreshEntity(marker);
    }

    // Removes and returns the player's corpse record and its marker, or null when they have none.
    static CorpseData.Corpse take(MinecraftServer server, UUID owner) {
        if (data == null) {
            return null;
        }
        CorpseData.Corpse corpse = data.take(owner);
        if (corpse != null) {
            CorpseMarkers.remove(server, corpse);
        }
        return corpse;
    }

    // Puts a taken record's items back when recovery could not place them all.
    static void restore(UUID owner, CorpseData.Corpse corpse) {
        data.getOrCreate(owner, corpse.dimension).items.addAll(corpse.items);
    }
}
