package info.mudbourn.mmsdungeons.corpse;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

// Lootable bodies of slain mobs: a downed mannequin holding the loot, open to anyone, gone once emptied or after a while.
public final class MobCorpses {

    private static final String TAG = "mms_dungeons_mob_corpse";
    private static final int SCAN_INTERVAL = 100;
    private static final long LIFETIME_TICKS = 12000L;

    private static AttachmentType<List<ItemStack>> LOOT;
    private static AttachmentType<Long> EXPIRES;

    private MobCorpses() {
    }

    public static void register() {
        LOOT = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.CODEC.listOf())
            .buildAndRegister(MmsDungeons.id("mob_corpse_loot"));
        EXPIRES = AttachmentRegistry.<Long>builder()
            .persistent(Codec.LONG)
            .buildAndRegister(MmsDungeons.id("mob_corpse_expires"));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!isCorpse(entity)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer looter && hand == InteractionHand.MAIN_HAND) {
                loot(looter, entity);
            }
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % SCAN_INTERVAL == 0) {
                server.getAllLevels().forEach(MobCorpses::sweep);
            }
        });
    }

    public static boolean isCorpse(Entity entity) {
        return entity instanceof Mannequin && entity.entityTags().contains(TAG);
    }

    // Lays a body at the spot wearing the given skin texture and armor, head to feet, at the given size, holding the loot. Nothing is made when there is no loot.
    public static void leave(ServerLevel level, Vec3 at, float facing, String texture, Component name, List<ItemStack> worn, float scale, List<ItemStack> loot) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        if (items.isEmpty()) {
            return;
        }
        CompoundTag profile = new CompoundTag();
        profile.putString("texture", texture);
        Mannequin body = CorpseMarkers.body(level, at, facing, profile, name, CollisionContext.empty());
        body.addTag(TAG);
        CorpseMarkers.dress(body, worn, scale);
        body.setAttached(LOOT, items);
        body.setAttached(EXPIRES, level.getGameTime() + LIFETIME_TICKS);
        level.addFreshEntity(body);
    }

    private static void loot(ServerPlayer player, Entity body) {
        List<ItemStack> items = body.getAttachedOrElse(LOOT, List.of());
        body.discard();
        for (ItemStack stack : items) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.spawnAtLocation((ServerLevel) player.level(), stack);
            }
        }
    }

    private static void sweep(ServerLevel level) {
        for (Mannequin body : level.getEntities(EntityTypes.MANNEQUIN, MobCorpses::isCorpse)) {
            if (level.getGameTime() >= body.getAttachedOrElse(EXPIRES, 0L)) {
                body.discard();
            }
        }
    }
}
