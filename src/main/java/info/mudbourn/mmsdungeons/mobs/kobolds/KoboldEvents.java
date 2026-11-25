package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mixin.features.MobAccessor;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.KoboldRascal;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class KoboldEvents {

    private static final ResourceKey<Enchantment> PROSPECTOR = ResourceKey.create(Registries.ENCHANTMENT, MmsDungeons.id("prospector"));
    private static final double EMERALD_CHANCE = 0.05;
    private static final double EMERALD_CHANCE_LUCKY = 0.25;

    private KoboldEvents() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(KoboldEvents::onLoad);
        ServerLivingEntityEvents.AFTER_DEATH.register(KoboldEvents::onDeath);
    }

    // Raiders and zombies go after kobolds, though zombies leave a Rascal alone until it hurts them.
    private static void onLoad(Entity entity, ServerLevel level) {
        if (entity instanceof Raider raider) {
            GoalSelector targets = ((MobAccessor) raider).mmsDungeons$getTargetSelector();
            targets.addGoal(3, new NearestAttackableTargetGoal<>(raider, AbstractKobold.class, false));
        } else if (entity instanceof Zombie zombie && !(zombie instanceof ZombifiedPiglin)) {
            ((MobAccessor) zombie).mmsDungeons$getTargetSelector().addGoal(
                3,
                new NearestAttackableTargetGoal<>(
                    zombie,
                    AbstractKobold.class,
                    false,
                    (target, targetLevel) -> !(target instanceof KoboldRascal) || zombie.getLastHurtByMob() == target
                )
            );
        }
    }

    // Prospector gives a small chance of emeralds, one per level, from enemies the wielder kills.
    private static void onDeath(LivingEntity victim, DamageSource source) {
        boolean enemy = victim instanceof Enemy;
        if (!enemy || !(source.getEntity() instanceof LivingEntity killer) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(PROSPECTOR),
            killer.getMainHandItem()
        );
        double chance = killer.hasEffect(MobEffects.LUCK) ? EMERALD_CHANCE_LUCKY : EMERALD_CHANCE;
        if (enchantLevel > 0 && level.getRandom().nextDouble() <= chance) {
            for (int i = 0; i < enchantLevel; i++) {
                level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.EMERALD)));
            }
        }
    }
}
