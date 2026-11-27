package info.mudbourn.mmsdungeons.mobs.swing;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.mobs.factions.FactionWeapons;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import info.mudbourn.mmsdungeons.net.MobSwingPayload;
import net.bettercombat.BetterCombatMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// Turns the instant melee hit of an armed, player-shaped mob into a Better Combat swing that lands after its upswing through the weapon's hitbox.
public final class ArmedSwings {

    public static final TagKey<EntityType<?>> SWINGERS = TagKey.create(Registries.ENTITY_TYPE, MmsDungeons.id("better_combat_swings"));
    private static final boolean BETTER_COMBAT = FabricLoader.getInstance().isModLoaded("bettercombat");
    private static final double BASE_ATTACK_SPEED = 4.0;
    private static final int MIN_SWING_TICKS = 8;

    // The swing a mob has in flight, held on the mob itself.
    public static final class State {
        private FactionWeapons.@Nullable Swing pending;
        private @Nullable LivingEntity target;
        private int hitDelay;
        private int busyUntil;
        private int lastSwing;
        private int combo;
        private boolean landing;
    }

    // A mob that carries a swing state.
    public interface Swinger {
        State mmsDungeons$swing();
    }

    private ArmedSwings() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(MobSwingPayload.TYPE, MobSwingPayload.CODEC);
    }

    // Called before a mob's own melee hit: null lets the hit through, false replaces it with a swing that lands later.
    public static @Nullable Boolean intercept(Mob mob, ServerLevel level, Entity target) {
        if (!BETTER_COMBAT || !FeaturesConfig.get().betterCombatMobSwings || mob instanceof FactionMember || !mob.typeHolder().is(SWINGERS)) {
            return null;
        }
        State state = ((Swinger) mob).mmsDungeons$swing();
        if (state.landing || !(target instanceof LivingEntity living)) {
            return null;
        }
        ItemStack main = mob.getMainHandItem();
        if (state.pending != null || mob.tickCount < state.busyUntil) {
            return false;
        }
        int length = swingTicks(mob, main);
        if (mob.tickCount - state.lastSwing > length * BetterCombatMod.config.combo_reset_rate) {
            state.combo = 0;
        }
        FactionWeapons.Swing swing = FactionWeapons.swing(main, mob.getOffhandItem(), false, state.combo);
        if (swing == null) {
            return null;
        }
        state.pending = swing;
        state.target = living;
        state.hitDelay = Math.max(1, Math.round(length * swing.upswing()));
        state.busyUntil = mob.tickCount + length;
        state.lastSwing = mob.tickCount;
        state.combo++;
        byte hand = swing.twoHanded() ? FactionMember.SWING_TWO_HANDED : FactionMember.SWING_MAIN_HAND;
        MobSwingPayload payload = new MobSwingPayload(mob.getId(), swing.attack().animation(), length, swing.upswing(), hand);
        for (ServerPlayer player : PlayerLookup.tracking(mob)) {
            ServerPlayNetworking.send(player, payload);
        }
        return false;
    }

    // Lands a swing in flight once its upswing has played.
    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        State state = ((Swinger) mob).mmsDungeons$swing();
        if (state.pending == null || --state.hitDelay > 0) {
            return;
        }
        FactionWeapons.Swing swing = state.pending;
        LivingEntity target = state.target;
        state.pending = null;
        state.target = null;
        if (target == null || !target.isAlive() || target.level() != level || !mob.isAlive()) {
            return;
        }
        state.landing = true;
        try {
            MobStrike.land(level, mob, target, swing, false, entity -> enemy(mob, entity));
        } finally {
            state.landing = false;
        }
    }

    // Who a swing may catch besides its target: players it may attack, and mobs fighting it.
    private static boolean enemy(Mob mob, LivingEntity entity) {
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && mob.canAttack(player);
        }
        return entity instanceof Mob other && other.getTarget() == mob;
    }

    // The swing length a player with this weapon would have: the mob's attack speed, or the weapon's own when the mob has none.
    private static int swingTicks(Mob mob, ItemStack weapon) {
        AttributeInstance attribute = mob.getAttribute(Attributes.ATTACK_SPEED);
        double speed = attribute != null ? attribute.getValue() : BASE_ATTACK_SPEED + weaponSpeed(weapon);
        return Math.max(MIN_SWING_TICKS, Mth.ceil(20.0 / Math.max(0.5, speed)));
    }

    private static double weaponSpeed(ItemStack weapon) {
        double[] total = {0.0};
        weapon.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_SPEED) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
    }
}
