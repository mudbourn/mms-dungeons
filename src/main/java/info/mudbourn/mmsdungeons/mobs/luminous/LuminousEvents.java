package info.mudbourn.mmsdungeons.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class LuminousEvents {

    public static final TagKey<EntityType<?>> CAUSES_FROST = TagKey.create(
        Registries.ENTITY_TYPE,
        MmsDungeons.id("causes_frost")
    );

    private static final int FROST_TICKS = 60;
    private static final float HEMORRHAGE_DAMAGE = 2.0f;

    private LuminousEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(LuminousEvents::onDamage);
    }

    // Frost from icy attackers, and the bleed that makes a hemorrhaging victim take extra damage from any attacker.
    private static boolean onDamage(LivingEntity victim, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null || !(victim.level() instanceof ServerLevel level)) {
            return true;
        }
        if (attacker.typeHolder().is(CAUSES_FROST)) {
            if (!victim.isBlocking()) {
                victim.setTicksFrozen(victim.getTicksFrozen() + FROST_TICKS);
                level.playSound(
                    null,
                    victim.blockPosition(),
                    SoundEvents.PLAYER_HURT_FREEZE,
                    SoundSource.NEUTRAL,
                    0.5f,
                    1.0f
                );
            }
            return true;
        }
        if (victim.hasEffect(LuminousEffects.HEMORRHAGE)) {
            Holder<DamageType> type = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.PLAYER_ATTACK);
            victim.hurtServer(level, new DamageSource(type), HEMORRHAGE_DAMAGE);
        }
        return true;
    }
}
