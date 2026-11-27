package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

// A fast, tall wolf-man that howls at its prey, claws, then bites, and shrugs off blows that are not silver.
public class Werewolf extends StrikeMob {

    public static final TagKey<Item> BANE = TagKey.create(Registries.ITEM, MmsDungeons.id("werewolf_bane"));
    public static final int VARIANT_COUNT = 3;

    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(Werewolf.class, EntityDataSerializers.INT);

    private static final double CHASE_SPEED = 1.6;
    private static final double STROLL_SPEED = 0.8;
    private static final float LOOK_RANGE = 8.0f;
    private static final float SILVER_AVOID_RANGE = 25.0f;
    private static final float UNSILVERED_FACTOR = 0.5f;
    private static final int CLAW_HIT_TICK = 15;
    private static final int CLAW_TOTAL_TICKS = 20;
    private static final int CLAW_COOLDOWN = 15;
    private static final int BITE_HIT_TICK = 12;
    private static final int BITE_TOTAL_TICKS = 22;
    private static final int BITE_COOLDOWN = 100;
    private static final int BITE_COMBAT_DELAY = 40;
    private static final float BITE_DAMAGE = 6.0f;
    private static final int HOWL_TICKS = 60;
    private static final int HOWL_COOLDOWN = 600;

    private int howlTicks;
    private int howlCooldown;
    private int combatTicks;
    private int biteCooldown;
    private boolean biting;

    public Werewolf(EntityType<? extends Werewolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 28.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Mth.clamp(variant, 0, VARIANT_COUNT - 1));
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || this.howlTicks > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, SilverSkeleton.class, SILVER_AVOID_RANGE, 2.0, 2.0));
        this.goalSelector.addGoal(2, new StrikeGoal(this, CHASE_SPEED));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, LOOK_RANGE));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void startAttack(LivingEntity target) {
        this.biting = this.biteCooldown == 0 && this.combatTicks >= BITE_COMBAT_DELAY;
        if (this.biting) {
            this.biteCooldown = BITE_COOLDOWN;
            this.beginStrike(target, "attack_bite", BITE_HIT_TICK, BITE_TOTAL_TICKS, CLAW_COOLDOWN);
        } else {
            this.beginStrike(target, "attack", CLAW_HIT_TICK, CLAW_TOTAL_TICKS, CLAW_COOLDOWN);
        }
    }

    @Override
    protected void landStrike(ServerLevel level, LivingEntity target) {
        if (!this.biting) {
            super.landStrike(level, target);
            return;
        }
        this.playSound(FangsNClawsSounds.WEREWOLF_BITE, 1.0f, 0.9f + this.random.nextFloat() * 0.2f);
        target.hurtServer(level, this.damageSources().mobAttack(this), BITE_DAMAGE);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean silvered = source.getDirectEntity() instanceof LivingEntity attacker
            && attacker.getMainHandItem().is(BANE);
        float scaled = !silvered && source.getEntity() != null ? amount * UNSILVERED_FACTOR : amount;
        return super.hurtServer(level, source, scaled);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.howlCooldown > 0) {
            this.howlCooldown--;
        }
        if (this.howlTicks > 0) {
            this.howlTicks--;
        }
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.combatTicks = 0;
            return;
        }
        this.combatTicks++;
        if (this.howlCooldown == 0 && !this.isBusy() && this.onGround()) {
            GeoTriggers.trigger(this, "howl");
            this.playSound(FangsNClawsSounds.WEREWOLF_HOWL, 1.2f, 0.9f + this.random.nextFloat() * 0.2f);
            this.howlTicks = HOWL_TICKS;
            this.howlCooldown = HOWL_COOLDOWN;
            this.getNavigation().stop();
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        @Nullable SpawnGroupData groupData
    ) {
        float roll = this.random.nextFloat();
        this.setVariant(roll < 0.5f ? 0 : roll < 0.85f ? 1 : 2);
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.WEREWOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.WEREWOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.WEREWOLF_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Variant", 0));
    }
}
