package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.dungeon.BossBarAggro;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A colossal worm that lies burrowed until a player nears, then claws, bites, spits acid and calls up arms.
public class PurpleWorm extends Monster {

    public static final int HIDDEN = 0;
    public static final int REVEALING = 1;
    public static final int REVEALED = 2;
    public static final int HIDING = 3;

    private enum Attack {
        CLAW(25, 0),
        BITE(25, 60),
        SPLIT(25, 30),
        MULTISPLIT(25, 150),
        BREATH(120, 300),
        SUMMON(120, 400),
        VOLLEY(65, 100);

        private final int total;
        private final int cooldown;

        Attack(int total, int cooldown) {
            this.total = total;
            this.cooldown = cooldown;
        }
    }

    private static final EntityDataAccessor<Integer> HIDE_STATE =
        SynchedEntityData.defineId(PurpleWorm.class, EntityDataSerializers.INT);

    private static final double[] PART_CENTERS = {3.3, 5.2, 7.1, 8.9, 10.8, 12.0};
    private static final double[] PART_FORWARD = {0.0, 0.0, 0.0, 0.0, 0.0, 0.8};
    private static final double PART_HEIGHT = 2.0;
    private static final double HEAD_HEIGHT = 11.5;
    private static final double HEAD_FORWARD = 2.0;
    private static final double REVEAL_RANGE = 20.0;
    private static final int HIDE_DELAY_TICKS = 200;
    private static final int REVEAL_TICKS = 10;
    private static final int HIDE_TICKS = 15;
    private static final float HIDE_HEAL_PER_TICK = 0.5f;
    private static final float TURN_SPEED = 4.0f;
    private static final float BITE_TURN_SPEED = 8.0f;
    private static final float BREATH_TURN_SPEED = 3.0f;
    private static final float FIRE_FACING_LIMIT = 35.0f;
    private static final int GLOBAL_COOLDOWN = 20;
    private static final double CLAW_RANGE = 5.0;
    private static final int CLAW_SOUND_TICK = 12;
    private static final int CLAW_HIT_TICK = 15;
    private static final double CLAW_KNOCKBACK = 3.0;
    private static final double BITE_MIN_RANGE = 4.0;
    private static final double BITE_MAX_RANGE = 10.0;
    private static final int BITE_SOUND_TICK = 8;
    private static final int BITE_DAMAGE_START = 6;
    private static final int BITE_DAMAGE_END = 22;
    private static final int BITE_TRACK_END = 20;
    private static final double BITE_CONE = 0.8;
    private static final float BITE_DAMAGE_FACTOR = 1.4f;
    private static final double BITE_KNOCKBACK = 1.0;
    private static final int SPLIT_FIRE_TICK = 12;
    private static final double SPLIT_MAX_RANGE = 30.0;
    private static final double SPLIT_SPEED = 1.8;
    private static final double SPLIT_GRAVITY = 0.03;
    private static final double SPLIT_DRAG = 0.99;
    private static final double MULTISPLIT_MIN_RANGE = 6.0;
    private static final double MULTISPLIT_MAX_RANGE = 26.0;
    private static final float MULTISPLIT_SPREAD = 12.0f;
    private static final int MULTISPLIT_SHOTS = 5;
    private static final float MULTISPLIT_SCALE = 2.0f;
    private static final double BREATH_MIN_RANGE = 5.0;
    private static final double BREATH_MAX_RANGE = 16.0;
    private static final double BREATH_REACH = 14.0;
    private static final double BREATH_CONE = 0.75;
    private static final int BREATH_START_TICK = 50;
    private static final int BREATH_END_TICK = 100;
    private static final int BREATH_STEP_TICKS = 5;
    private static final int[] BREATH_CLOUD_TICKS = {60, 75, 90};
    private static final float BREATH_DAMAGE = 5.0f;
    private static final int BREATH_ACID_TICKS = 100;
    private static final float BREATH_CLOUD_WIDTH = 3.0f;
    private static final int BREATH_CLOUD_LIFETIME = 160;
    private static final int ACID_COLOR = 0xFF76F154;
    private static final double SUMMON_MAX_RANGE = 30.0;
    private static final int[] SUMMON_TICKS = {25, 45, 65};
    private static final int SUMMON_TRIES = 8;
    private static final double SUMMON_MIN_RADIUS = 1.0;
    private static final double SUMMON_RADIUS_SPREAD = 2.0;
    private static final int SUMMON_ARM_LIMIT = 8;
    private static final double VOLLEY_MIN_RANGE = 8.0;
    private static final double VOLLEY_MAX_RANGE = 30.0;
    private static final int[] VOLLEY_TICKS = {15, 22, 29, 36, 43, 50};
    private static final double VOLLEY_FAR_OFFSET = 6.0;
    private static final double VOLLEY_NEAR_OFFSET = 6.0;
    private static final double VOLLEY_SIDE_SPREAD = 3.0;
    private static final int VOLLEY_FLIGHT_TICKS = 40;
    private static final double MIN_CLEARANCE = 1.0;

    private final ServerBossEvent bossEvent;
    private final Set<UUID> fought = new HashSet<>();
    private final int[] cooldowns = new int[Attack.values().length];
    private final Set<UUID> bitten = new HashSet<>();
    private PurpleWormPart[] parts;
    private Attack attack;
    private int attackTick;
    private LivingEntity attackTarget;
    private Vec3 volleyAim = Vec3.ZERO;
    private int globalCooldown;
    private int hideTicks;
    private int awayTicks;

    public PurpleWorm(EntityType<? extends PurpleWorm> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    // Whether an entity belongs to a Purple Worm and its acid, and so is spared by them.
    public static boolean isKin(Entity entity) {
        return entity instanceof PurpleWorm
            || entity instanceof PurpleWormPart
            || entity instanceof PurpleWormArm
            || entity instanceof AcidCloud;
    }

    public int getHideState() {
        return this.entityData.get(HIDE_STATE);
    }

    public boolean isBodyShown() {
        return this.getHideState() != HIDDEN;
    }

    private void setHideState(int state) {
        this.entityData.set(HIDE_STATE, state);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDE_STATE, HIDDEN);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            @Override
            public boolean canUse() {
                return PurpleWorm.this.getHideState() == REVEALED && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(0.0, motion.y, 0.0);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.syncParts(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        BossBarAggro.sync(this.bossEvent, this, this.fought, player -> true);
        this.bossEvent.setVisible(this.isBodyShown());
        for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
                this.cooldowns[i]--;
            }
        }
        switch (this.getHideState()) {
            case HIDDEN -> this.tickHidden(level);
            case REVEALING -> this.tickRevealing();
            case REVEALED -> this.tickRevealed(level);
            case HIDING -> this.tickHiding();
        }
    }

    private Player nearbyPlayer(ServerLevel level) {
        Player near = level.getNearestPlayer(this, REVEAL_RANGE);
        return near == null || near.isSpectator() ? null : near;
    }

    private void tickHidden(ServerLevel level) {
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.heal(HIDE_HEAL_PER_TICK);
        if (this.nearbyPlayer(level) != null) {
            this.setHideState(REVEALING);
            this.hideTicks = REVEAL_TICKS;
            this.awayTicks = 0;
            GeoTriggers.trigger(this, "reveal");
            this.playSound(FangsNClawsSounds.PURPLE_WORM_EMERGE, 3.0f, 1.0f);
        }
    }

    private void tickRevealing() {
        if (--this.hideTicks <= 0) {
            this.setHideState(REVEALED);
        }
    }

    private void tickHiding() {
        if (--this.hideTicks <= 0) {
            this.setHideState(HIDDEN);
        }
    }

    private void tickRevealed(ServerLevel level) {
        LivingEntity target = this.getTarget();
        boolean live = target != null && target.isAlive();
        this.awayTicks = this.nearbyPlayer(level) == null && !live ? this.awayTicks + 1 : 0;
        if (this.attack != null) {
            this.tickAttack(level);
            return;
        }
        if (this.awayTicks >= HIDE_DELAY_TICKS) {
            this.startHide();
            return;
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (!live) {
            return;
        }
        this.faceToward(target.position(), TURN_SPEED);
        if (this.globalCooldown == 0) {
            this.chooseAttack(level, target);
        }
    }

    private void startHide() {
        this.setHideState(HIDING);
        this.hideTicks = HIDE_TICKS;
        this.setTarget(null);
        GeoTriggers.trigger(this, "hide");
        this.playSound(FangsNClawsSounds.PURPLE_WORM_HIDE, 3.0f, 1.0f);
    }

    private double flatDistance(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private float yawTo(Vec3 point) {
        return (float) (Mth.atan2(-(point.x - this.getX()), point.z - this.getZ()) * 180.0 / Math.PI);
    }

    private boolean isFacing(Vec3 point) {
        return Math.abs(Mth.wrapDegrees(this.yawTo(point) - this.getYRot())) <= FIRE_FACING_LIMIT;
    }

    private void faceToward(Vec3 point, float speed) {
        float yaw = Mth.approachDegrees(this.getYRot(), this.yawTo(point), speed);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private Vec3 forward() {
        float radians = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
    }

    private boolean onRight(Vec3 point) {
        Vec3 forward = this.forward();
        double dx = point.x - this.getX();
        double dz = point.z - this.getZ();
        return dx * -forward.z + dz * forward.x > 0.0;
    }

    private Vec3 headPosition(ServerLevel level) {
        Vec3 base = this.position().add(0.0, 1.0, 0.0);
        double height = HEAD_HEIGHT;
        BlockHitResult ceiling = level.clip(
            new ClipContext(base, base.add(0.0, height, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        );
        if (ceiling.getType() == HitResult.Type.BLOCK) {
            height = Math.max(ceiling.getLocation().y - base.y - 0.6, MIN_CLEARANCE);
        }
        return base.add(this.forward().scale(HEAD_FORWARD)).add(0.0, height, 0.0);
    }

    private void chooseAttack(ServerLevel level, LivingEntity target) {
        double distance = this.flatDistance(target);
        boolean facing = this.isFacing(target.position());
        List<Attack> options = new ArrayList<>();
        if (distance <= CLAW_RANGE) {
            options.add(Attack.CLAW);
        }
        if (this.ready(Attack.BITE) && distance >= BITE_MIN_RANGE && distance <= BITE_MAX_RANGE && facing) {
            options.add(Attack.BITE);
        }
        if (this.ready(Attack.SPLIT) && distance <= SPLIT_MAX_RANGE && facing) {
            options.add(Attack.SPLIT);
        }
        if (this.ready(Attack.MULTISPLIT) && distance >= MULTISPLIT_MIN_RANGE
            && distance <= MULTISPLIT_MAX_RANGE && facing) {
            options.add(Attack.MULTISPLIT);
        }
        if (this.ready(Attack.BREATH) && distance >= BREATH_MIN_RANGE && distance <= BREATH_MAX_RANGE && facing) {
            options.add(Attack.BREATH);
        }
        if (this.ready(Attack.SUMMON) && distance <= SUMMON_MAX_RANGE) {
            options.add(Attack.SUMMON);
        }
        if (this.ready(Attack.VOLLEY) && distance >= VOLLEY_MIN_RANGE && distance <= VOLLEY_MAX_RANGE) {
            options.add(Attack.VOLLEY);
        }
        if (!options.isEmpty()) {
            this.startAttack(options.get(this.random.nextInt(options.size())), target);
        }
    }

    private boolean ready(Attack kind) {
        return this.cooldowns[kind.ordinal()] == 0;
    }

    private void startAttack(Attack kind, LivingEntity target) {
        this.attack = kind;
        this.attackTick = 0;
        this.attackTarget = target;
        this.cooldowns[kind.ordinal()] = kind.cooldown;
        this.bitten.clear();
        this.volleyAim = target.position();
        this.getNavigation().stop();
        String animation = switch (kind) {
            case CLAW -> this.onRight(target.position()) ? "attack_claw_right" : "attack_claw_left";
            case BITE -> "attack_bite";
            case SPLIT -> "attack_split";
            case MULTISPLIT -> "attack_multisplit";
            case BREATH -> "attack_breath";
            case SUMMON -> "attack_summon_claws";
            case VOLLEY -> "attack_split_volley";
        };
        GeoTriggers.trigger(this, animation);
        if (kind == Attack.BREATH) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_BREATH, 3.0f, 1.0f);
        } else if (kind == Attack.VOLLEY) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_SPLIT_VOLLEY, 3.0f, 1.0f);
        }
    }

    private void tickAttack(ServerLevel level) {
        this.attackTick++;
        LivingEntity target = this.attackTarget;
        boolean live = target != null && target.isAlive();
        switch (this.attack) {
            case CLAW -> this.tickClaw(level, target, live);
            case BITE -> this.tickBite(level, target, live);
            case SPLIT -> this.tickSplit(level, target, live);
            case MULTISPLIT -> this.tickMultisplit(level, target, live);
            case BREATH -> this.tickBreath(level, target, live);
            case SUMMON -> this.tickSummon(level, target, live);
            case VOLLEY -> this.tickVolley(level);
        }
        if (this.attackTick >= this.attack.total) {
            this.attack = null;
            this.attackTarget = null;
            this.globalCooldown = GLOBAL_COOLDOWN;
        }
    }

    private void tickClaw(ServerLevel level, LivingEntity target, boolean live) {
        if (live && this.attackTick <= CLAW_HIT_TICK) {
            this.faceToward(target.position(), TURN_SPEED);
        }
        if (this.attackTick == CLAW_SOUND_TICK) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_CLAW, 3.0f, 1.0f);
        }
        if (this.attackTick == CLAW_HIT_TICK) {
            Vec3 forward = this.forward();
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity victim : this.victims(level, CLAW_RANGE)) {
                Vec3 offset = victim.position().subtract(this.position());
                if (offset.x * forward.x + offset.z * forward.z > 0.0
                    && victim.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                    this.knock(victim, CLAW_KNOCKBACK);
                }
            }
        }
    }

    private void tickBite(ServerLevel level, LivingEntity target, boolean live) {
        if (live && this.attackTick <= BITE_TRACK_END) {
            this.faceToward(target.position(), BITE_TURN_SPEED);
        }
        if (this.attackTick == BITE_SOUND_TICK) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_BITE, 3.0f, 1.0f);
        }
        if (this.attackTick < BITE_DAMAGE_START || this.attackTick > BITE_DAMAGE_END) {
            return;
        }
        Vec3 forward = this.forward();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * BITE_DAMAGE_FACTOR;
        for (LivingEntity victim : this.victims(level, BITE_MAX_RANGE)) {
            Vec3 offset = victim.position().subtract(this.position());
            double flat = Math.max(Math.sqrt(offset.x * offset.x + offset.z * offset.z), 1.0E-4);
            boolean ahead = (offset.x * forward.x + offset.z * forward.z) / flat >= BITE_CONE;
            if (ahead && this.bitten.add(victim.getUUID())
                && victim.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                this.knock(victim, BITE_KNOCKBACK);
            }
        }
    }

    private void tickSplit(ServerLevel level, LivingEntity target, boolean live) {
        if (live && this.attackTick < SPLIT_FIRE_TICK) {
            this.faceToward(target.position(), TURN_SPEED);
        }
        if (this.attackTick == SPLIT_FIRE_TICK && live) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_SPLIT, 3.0f, 1.0f);
            this.fireSplit(level, target.getEyePosition(), 1.0f, 0.0f);
        }
    }

    private void tickMultisplit(ServerLevel level, LivingEntity target, boolean live) {
        if (live && this.attackTick < SPLIT_FIRE_TICK) {
            this.faceToward(target.position(), TURN_SPEED);
        }
        if (this.attackTick == SPLIT_FIRE_TICK && live) {
            this.playSound(FangsNClawsSounds.PURPLE_WORM_SPLIT, 3.0f, 0.8f);
            for (int i = 0; i < MULTISPLIT_SHOTS; i++) {
                float spread = (i - (MULTISPLIT_SHOTS - 1) / 2.0f) * MULTISPLIT_SPREAD;
                this.fireSplit(level, target.getEyePosition(), MULTISPLIT_SCALE, spread);
            }
        }
    }

    private void fireSplit(ServerLevel level, Vec3 aim, float scale, float spreadDegrees) {
        Vec3 origin = this.headPosition(level);
        double flat = Math.sqrt((aim.x - origin.x) * (aim.x - origin.x) + (aim.z - origin.z) * (aim.z - origin.z));
        double flight = flat / SPLIT_SPEED;
        Vec3 lifted = aim.add(0.0, 0.5 * SPLIT_GRAVITY * flight * flight, 0.0);
        Vec3 direction = lifted.subtract(origin).normalize().yRot(-spreadDegrees * Mth.DEG_TO_RAD);
        AcidSplit split = new AcidSplit(level, this, scale);
        split.setPos(origin);
        split.shoot(direction.x, direction.y, direction.z, (float) SPLIT_SPEED, 0.0f);
        level.addFreshEntity(split);
    }

    private void tickBreath(ServerLevel level, LivingEntity target, boolean live) {
        if (live && this.attackTick < BREATH_END_TICK) {
            this.faceToward(target.position(), BREATH_TURN_SPEED);
        }
        if (this.attackTick < BREATH_START_TICK || this.attackTick > BREATH_END_TICK) {
            return;
        }
        Vec3 head = this.headPosition(level);
        Vec3 forward = this.forward();
        level.sendParticles(
            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ACID_COLOR),
            head.x + forward.x * 2.0,
            head.y - 1.0,
            head.z + forward.z * 2.0,
            12,
            1.0,
            0.6,
            1.0,
            0.0
        );
        if (this.attackTick % BREATH_STEP_TICKS == 0) {
            for (LivingEntity victim : this.victims(level, BREATH_REACH)) {
                Vec3 offset = victim.position().subtract(this.position());
                double flat = Math.max(Math.sqrt(offset.x * offset.x + offset.z * offset.z), 1.0E-4);
                if ((offset.x * forward.x + offset.z * forward.z) / flat >= BREATH_CONE
                    && victim.hurtServer(level, this.damageSources().mobAttack(this), BREATH_DAMAGE)) {
                    victim.addEffect(new MobEffectInstance(FangsNClawsEffects.CORRODED, BREATH_ACID_TICKS, 0));
                }
            }
        }
        for (int cloudTick : BREATH_CLOUD_TICKS) {
            if (cloudTick == this.attackTick && live) {
                this.dropCloud(level, target.position());
            }
        }
    }

    private void dropCloud(ServerLevel level, Vec3 at) {
        AcidCloud cloud = new AcidCloud(FangsNClawsMobs.ACID_CLOUD, level);
        cloud.setPos(at);
        cloud.setSize(BREATH_CLOUD_WIDTH, 1.0f);
        cloud.setLifetime(BREATH_CLOUD_LIFETIME);
        level.addFreshEntity(cloud);
    }

    private void tickSummon(ServerLevel level, LivingEntity target, boolean live) {
        for (int summonTick : SUMMON_TICKS) {
            if (summonTick == this.attackTick && live) {
                this.raiseArm(level, target);
            }
        }
    }

    private void raiseArm(ServerLevel level, LivingEntity target) {
        int arms = level.getEntitiesOfClass(
            PurpleWormArm.class,
            this.getBoundingBox().inflate(SUMMON_MAX_RANGE)
        ).size();
        if (arms >= SUMMON_ARM_LIMIT) {
            return;
        }
        for (int i = 0; i < SUMMON_TRIES; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = SUMMON_MIN_RADIUS + this.random.nextDouble() * SUMMON_RADIUS_SPREAD;
            BlockPos spot = BlockPos.containing(
                target.getX() + Math.cos(angle) * radius,
                target.getY(),
                target.getZ() + Math.sin(angle) * radius
            );
            if (level.getBlockState(spot.below()).isSolid() && level.getBlockState(spot).isAir()) {
                PurpleWormArm arm = new PurpleWormArm(FangsNClawsMobs.PURPLE_WORM_ARM, level);
                arm.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                level.addFreshEntity(arm);
                arm.emerge();
                return;
            }
        }
    }

    private void tickVolley(ServerLevel level) {
        for (int i = 0; i < VOLLEY_TICKS.length; i++) {
            if (VOLLEY_TICKS[i] == this.attackTick) {
                this.lobSplit(level, i);
            }
        }
    }

    private void lobSplit(ServerLevel level, int index) {
        Vec3 origin = this.headPosition(level);
        Vec3 toward = new Vec3(this.volleyAim.x - this.getX(), 0.0, this.volleyAim.z - this.getZ()).normalize();
        Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
        double along = index % 2 == 0 ? VOLLEY_FAR_OFFSET : -VOLLEY_NEAR_OFFSET;
        double across = (this.random.nextDouble() * 2.0 - 1.0) * VOLLEY_SIDE_SPREAD;
        Vec3 landing = this.volleyAim.add(toward.scale(along)).add(side.scale(across));
        AcidSplit split = new AcidSplit(level, this, 1.0f);
        split.setPos(origin);
        split.setDeltaMovement(lob(origin, landing, VOLLEY_FLIGHT_TICKS));
        level.addFreshEntity(split);
    }

    private static Vec3 lob(Vec3 from, Vec3 to, int ticks) {
        Vec3 velocity = to.subtract(from).scale(1.0 / ticks).add(0.0, 0.5 * SPLIT_GRAVITY * ticks, 0.0);
        for (int pass = 0; pass < 4; pass++) {
            Vec3 position = from;
            Vec3 step = velocity;
            for (int tick = 0; tick < ticks; tick++) {
                position = position.add(step);
                step = step.scale(SPLIT_DRAG).add(0.0, -SPLIT_GRAVITY, 0.0);
            }
            velocity = velocity.add(to.subtract(position).scale(1.0 / ticks));
        }
        return velocity;
    }

    private List<LivingEntity> victims(ServerLevel level, double range) {
        return level.getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(range, 4.0, range),
            entity -> !isKin(entity) && entity.isAlive() && this.flatDistance(entity) <= range
        );
    }

    private void knock(LivingEntity victim, double strength) {
        Vec3 away = victim.position().subtract(this.position());
        double flat = Math.max(Math.sqrt(away.x * away.x + away.z * away.z), 1.0E-4);
        victim.push(away.x / flat * strength, 0.5, away.z / flat * strength);
        victim.needsSync = true;
    }

    private void syncParts(ServerLevel level) {
        if (this.parts == null) {
            this.parts = new PurpleWormPart[PART_CENTERS.length];
        }
        Vec3 forward = this.forward();
        for (int i = 0; i < this.parts.length; i++) {
            PurpleWormPart part = this.parts[i];
            if (part == null || part.isRemoved()) {
                part = new PurpleWormPart(FangsNClawsMobs.PURPLE_WORM_PART, level);
                part.attach(this);
                this.parts[i] = part;
                level.addFreshEntity(part);
            }
            part.setPos(
                this.getX() + forward.x * PART_FORWARD[i],
                this.getY() + PART_CENTERS[i] - PART_HEIGHT / 2.0,
                this.getZ() + forward.z * PART_FORWARD[i]
            );
        }
    }

    private void discardParts() {
        if (this.parts == null) {
            return;
        }
        for (PurpleWormPart part : this.parts) {
            if (part != null) {
                part.discard();
            }
        }
        this.parts = null;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) {
            return true;
        }
        return this.getHideState() == HIDDEN
            && !source.is(DamageTypes.GENERIC_KILL)
            && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
            || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean isPickable() {
        return this.isBodyShown() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return this.isBodyShown() && super.canBeCollidedWith(other);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.discardParts();
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getHideState() == REVEALED ? FangsNClawsSounds.PURPLE_WORM_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.PURPLE_WORM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.PURPLE_WORM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0f;
    }
}
