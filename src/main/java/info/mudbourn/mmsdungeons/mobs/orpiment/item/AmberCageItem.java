package info.mudbourn.mmsdungeons.mobs.orpiment.item;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.WispSkullProjectile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Summons five wisp skulls that circle the holder, frozen in place, and fires them one by one.
public class AmberCageItem extends Item {

    private static final int SKULLS = 5;
    private static final int FIRE_INTERVAL = 10;
    private static final int COOLDOWN = 400;
    private static final double ORBIT_STEP = 0.031415926535897934;
    private static final String ACTIVE = "AmberCageActive";
    private static final String SKULL_IDS = "AmberCageSkulls";
    private static final String FIRE_INDEX = "AmberCageFireIndex";
    private static final String LAST_FIRE = "AmberCageLastFire";

    public AmberCageItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(ACTIVE, false);
    }

    private static CompoundTag read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static List<Integer> skullIds(CompoundTag tag) {
        List<Integer> ids = new ArrayList<>();
        for (int id : tag.getIntArray(SKULL_IDS).orElse(new int[0])) {
            ids.add(id);
        }
        return ids;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) {
            return;
        }
        CompoundTag tag = read(stack);
        List<Integer> ids = skullIds(tag);
        if (!isActive(stack)) {
            if (!ids.isEmpty()) {
                ids.forEach(id -> discard(level, id));
                CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.remove(SKULL_IDS));
            }
            return;
        }
        double angle = player.tickCount * ORBIT_STEP;
        int fired = tag.getIntOr(FIRE_INDEX, 0);
        for (int i = fired; i < ids.size(); i++) {
            if (level.getEntity(ids.get(i)) instanceof WispSkullProjectile skull && skull.isAlive()) {
                double skullAngle = angle + Math.PI * 2 / ids.size() * i;
                skull.setPos(player.getX() + 2.5 * Math.cos(skullAngle), player.getY() + 6.0, player.getZ() + 2.5 * Math.sin(skullAngle));
                skull.setDeltaMovement(Vec3.ZERO);
                skull.setNoGravity(true);
            }
        }
    }

    private static void discard(ServerLevel level, int id) {
        if (level.getEntity(id) instanceof WispSkullProjectile skull) {
            skull.discard();
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (isActive(stack)) {
            this.fireNext(server, player, stack);
        } else if (stack.getDamageValue() < stack.getMaxDamage()) {
            this.start(server, player, stack, hand);
        } else {
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    private void start(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < SKULLS; i++) {
            double angle = Math.PI * 2 / SKULLS * i;
            WispSkullProjectile skull = new WispSkullProjectile(OrpimentMobs.WISP_SKULL_PROJECTILE, level);
            skull.setPos(player.getX() + Math.cos(angle) * 2.5, player.getY() + 6.0, player.getZ() + Math.sin(angle) * 2.5);
            skull.setNoGravity(true);
            skull.pickup = AbstractArrow.Pickup.DISALLOWED;
            skull.setSilent(true);
            level.addFreshEntity(skull);
            ids.add(skull.getId());
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(ACTIVE, true);
            tag.putIntArray(SKULL_IDS, ids.stream().mapToInt(Integer::intValue).toArray());
            tag.putInt(FIRE_INDEX, 0);
            tag.putInt(LAST_FIRE, player.tickCount);
        });
        stack.hurtAndBreak(1, player, hand);
        for (int i = 0; i < 20; i++) {
            level.sendParticles(
                OrpimentParticles.WRAITH_SKULL,
                player.getX() + (player.getRandom().nextDouble() - 0.5) * 5.0,
                player.getY() + player.getRandom().nextDouble() * 3.0,
                player.getZ() + (player.getRandom().nextDouble() - 0.5) * 5.0,
                1,
                0.0,
                0.1,
                0.0,
                0.05
            );
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), OrpimentSounds.WRAITH_SUMMON, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void fireNext(ServerLevel level, Player player, ItemStack stack) {
        CompoundTag tag = read(stack);
        int index = tag.getIntOr(FIRE_INDEX, 0);
        List<Integer> ids = skullIds(tag);
        if (player.tickCount - tag.getIntOr(LAST_FIRE, 0) < FIRE_INTERVAL || index >= ids.size()) {
            return;
        }
        if (level.getEntity(ids.get(index)) instanceof WispSkullProjectile skull && skull.isAlive()) {
            skull.teleportTo(player.getX(), player.getEyeY() - 0.5, player.getZ());
            Vec3 look = player.getLookAngle();
            skull.shoot(look.x, look.y, look.z, 1.0f, 0.0f);
            skull.setNoGravity(false);
            skull.setBaseDamage(30.0);
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                OrpimentSounds.WRAITH_SHOOT,
                SoundSource.PLAYERS,
                0.8f,
                0.9f + player.getRandom().nextFloat() * 0.2f
            );
        }
        int next = index + 1;
        if (next >= ids.size()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.putBoolean(ACTIVE, false);
                data.remove(SKULL_IDS);
                data.putInt(FIRE_INDEX, 0);
            });
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.putInt(FIRE_INDEX, next);
                data.putInt(LAST_FIRE, player.tickCount);
            });
        }
    }
}
