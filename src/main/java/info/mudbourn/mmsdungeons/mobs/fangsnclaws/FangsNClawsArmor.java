package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorType;

// What the Fangs 'n Claws armor sets do beyond their stats.
public final class FangsNClawsArmor {

    private static final int CURE_INTERVAL_TICKS = 20;
    private static final int FULL_FUR_SET = 4;

    private FangsNClawsArmor() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean cure = server.getTickCount() % CURE_INTERVAL_TICKS == 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (cure) {
                    cureScorpion(player);
                }
                warmInPowderSnow(player);
            }
        });
    }

    public static boolean hasFurHelmet(LivingEntity wearer) {
        return FangsNClawsGear.FUR.wears(wearer, ArmorType.HELMET);
    }

    public static boolean wearsFurBoots(LivingEntity wearer) {
        return FangsNClawsGear.FUR.wears(wearer, ArmorType.BOOTS);
    }

    // Whether a worn Scorpion piece makes the wearer immune to an effect.
    public static boolean blocksEffect(LivingEntity wearer, Holder<MobEffect> effect) {
        return effect.is(MobEffects.NAUSEA) && FangsNClawsGear.SCORPION.wears(wearer, ArmorType.HELMET)
            || effect.is(MobEffects.POISON) && FangsNClawsGear.SCORPION.wears(wearer, ArmorType.CHESTPLATE)
            || effect.is(FangsNClawsEffects.VENOM) && FangsNClawsGear.SCORPION.wears(wearer, ArmorType.LEGGINGS)
            || effect.is(MobEffects.HUNGER) && FangsNClawsGear.SCORPION.wears(wearer, ArmorType.BOOTS);
    }

    private static void cureScorpion(LivingEntity wearer) {
        if (FangsNClawsGear.SCORPION.wears(wearer, ArmorType.HELMET)) {
            wearer.removeEffect(MobEffects.NAUSEA);
        }
        if (FangsNClawsGear.SCORPION.wears(wearer, ArmorType.CHESTPLATE)) {
            wearer.removeEffect(MobEffects.POISON);
        }
        if (FangsNClawsGear.SCORPION.wears(wearer, ArmorType.LEGGINGS)) {
            FangsNClawsEffects.cureVenom(wearer);
        }
        if (FangsNClawsGear.SCORPION.wears(wearer, ArmorType.BOOTS)) {
            wearer.removeEffect(MobEffects.HUNGER);
        }
    }

    private static void warmInPowderSnow(LivingEntity wearer) {
        if (!wearer.isInPowderSnow) {
            return;
        }
        int pieces = FangsNClawsGear.FUR.count(wearer);
        if (pieces >= FULL_FUR_SET) {
            wearer.setTicksFrozen(0);
        } else if (pieces > 0 && wearer.getRandom().nextInt(FULL_FUR_SET) < pieces) {
            wearer.setTicksFrozen(Math.max(0, wearer.getTicksFrozen() - 1));
        }
    }
}
