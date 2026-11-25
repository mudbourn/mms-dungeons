package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A winged spirit that circles its prey and strikes with swoops, skull barrages, wisp summons and a ground slam.
public class Wraith extends Monster {

    private static final EntityDataAccessor<Boolean> SUMMONING = SynchedEntityData.defineId(Wraith.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FIRING = SynchedEntityData.defineId(Wraith.class, EntityDataSerializers.BOOLEAN);
    private static final Identifier SLAM_RESIST = MmsDungeons.id("wraith_slam_resist");
    private static final int WISP_CLEAR_RANGE = 30;

    private double orbitAngle;
    public AttackPhase attackPhase = AttackPhase.ORBITING;
    public int phaseTimer;
    public int attackCooldown;
    public Vec3 retreatTarget;
    public Vec3 dashDirection;
    private final List<WispSkullProjectile> hangingProjectiles = new ArrayList<>();
    private int universalAttackTimer;
    private AttackType lastAttack;
    public final AnimationState summonState = new AnimationState();
    public final AnimationState idleState = new AnimationState();
    public final AnimationState fireState = new AnimationState();
    public final AnimationState moveState = new AnimationState();
    private boolean wasPhase2;
    private boolean slamResistActive;

    public Wraith(EntityType<? extends Wraith> type, Level level) {
        super(type, level);
        this.xpReward = 22;
        this.moveControl = new WraithMoveControl(this);
        this.setPersistenceRequired();
        this.universalAttackTimer = 60 + this.random.nextInt(80);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.MAX_HEALTH, 170.0)
            .add(Attributes.ARMOR, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUMMONING, false);
        builder.define(FIRING, false);
    }

    public boolean isSummoning() {
        return this.entityData.get(SUMMONING);
    }

    public void setSummoning(boolean value) {
        this.entityData.set(SUMMONING, value);
    }

    public boolean isFiring() {
        return this.entityData.get(FIRING);
    }

    public void setFiring(boolean value) {
        this.entityData.set(FIRING, value);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.universalAttackTimer > 0) {
            this.universalAttackTimer--;
        }
    }

    public boolean isPhase2() {
        return this.getHealth() / this.getMaxHealth() <= 0.25f;
    }

    public AttackType getRandomAttack() {
        List<AttackType> available = new ArrayList<>();
        addWeighted(available, AttackType.SWOOP, 40);
        addWeighted(available, AttackType.BARRAGE, 40);
        addWeighted(available, AttackType.SUMMON, 20);
        if (this.isPhase2()) {
            addWeighted(available, AttackType.GROUND_SLAM, 30);
        }
        if (this.lastAttack != null) {
            available.removeIf(attack -> attack == this.lastAttack);
        }
        return available.isEmpty() ? AttackType.SWOOP : available.get(this.random.nextInt(available.size()));
    }

    private static void addWeighted(List<AttackType> list, AttackType type, int weight) {
        for (int i = 0; i < weight; i++) {
            list.add(type);
        }
    }

    public Player getNearestSurvivalPlayer() {
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : this.level().players()) {
            if (!player.isCreative() && !player.isSpectator() && player.isAlive()) {
                double distance = this.distanceToSqr(player);
                if (distance <= range * range && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = player;
                }
            }
        }
        return nearest;
    }

    public void returnToOrbit(int cooldownMin, int cooldownMax) {
        this.attackPhase = AttackPhase.ORBITING;
        this.phaseTimer = 0;
        this.dashDirection = null;
        int min = this.isPhase2() ? cooldownMin / 2 : cooldownMin;
        int max = this.isPhase2() ? cooldownMax / 2 : cooldownMax;
        this.universalAttackTimer = min + this.random.nextInt(Math.max(1, max - min));
    }

    public void applySlamResistance() {
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        if (!this.slamResistActive && armor != null) {
            armor.addTransientModifier(new AttributeModifier(SLAM_RESIST, 40.0, AttributeModifier.Operation.ADD_VALUE));
            this.slamResistActive = true;
        }
    }

    public void removeSlamResistance() {
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        if (this.slamResistActive && armor != null) {
            armor.removeModifier(SLAM_RESIST);
            this.slamResistActive = false;
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level() instanceof ServerLevel level && this.random.nextFloat() < 0.3f) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 1, 1.0, 1.0, 1.0, 0.001);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            boolean phase2 = this.isPhase2();
            if (phase2 && !this.wasPhase2) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), OrpimentSounds.WRAITH_SUMMON, SoundSource.HOSTILE, 2.0f, 0.5f);
            }
            this.wasPhase2 = phase2;
            return;
        }
        boolean moving = Math.abs(this.getDeltaMovement().x) > 0.05 || Math.abs(this.getDeltaMovement().z) > 0.05;
        boolean shouldMove = moving || this.getTarget() != null || this.attackPhase == AttackPhase.ORBITING;
        boolean busy = this.isSummoning() || this.isFiring();
        if (!shouldMove || busy) {
            this.moveState.stop();
        } else if (!this.moveState.isStarted()) {
            this.moveState.start(this.tickCount);
            this.idleState.stop();
        }
        if (!shouldMove && !busy) {
            if (!this.idleState.isStarted()) {
                this.idleState.start(this.tickCount);
                this.moveState.stop();
            }
        } else if (this.idleState.isStarted()) {
            this.idleState.stop();
        }
        if (this.isSummoning()) {
            if (!this.summonState.isStarted()) {
                this.summonState.start(this.tickCount);
            }
        } else {
            this.summonState.stop();
        }
        if (this.isFiring()) {
            if (!this.fireState.isStarted()) {
                this.fireState.start(this.tickCount);
            }
        } else {
            this.fireState.stop();
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    private void applyWallCollision(Vec3 movement) {
        AABB box = this.getBoundingBox();
        if (this.level().noCollision(this, box.move(movement))) {
            this.setDeltaMovement(movement);
            return;
        }
        Vec3 adjusted = movement;
        if (!this.level().noCollision(this, box.move(new Vec3(movement.x, 0.0, 0.0)))) {
            adjusted = new Vec3(0.0, adjusted.y, adjusted.z);
        }
        if (!this.level().noCollision(this, box.move(new Vec3(0.0, 0.0, movement.z)))) {
            adjusted = new Vec3(adjusted.x, adjusted.y, 0.0);
        }
        if (!this.level().noCollision(this, box.move(new Vec3(0.0, movement.y, 0.0)))) {
            adjusted = new Vec3(adjusted.x, 0.0, adjusted.z);
        }
        this.setDeltaMovement(adjusted);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new UniversalAttackGoal(this));
        this.goalSelector.addGoal(2, new OrbitPlayerGoal(this));
        this.goalSelector.addGoal(3, new AimlessDriftGoal(this));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private static void faceEntity(Wraith wraith, LivingEntity target) {
        double dx = target.getX() - wraith.getX();
        double dz = target.getZ() - wraith.getZ();
        wraith.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        wraith.yHeadRot = wraith.getYRot();
        wraith.yBodyRot = wraith.getYRot();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !this.level().isClientSide()) {
            this.hangingProjectiles.stream().filter(projectile -> projectile != null && projectile.isAlive()).forEach(WispSkullProjectile::discard);
            this.hangingProjectiles.clear();
            if (this.level() instanceof ServerLevel level) {
                level.getEntitiesOfClass(Wisp.class, this.getBoundingBox().inflate(WISP_CLEAR_RANGE)).forEach(Wisp::discard);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Projectile) {
            amount *= 0.8f;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return this.attackPhase != AttackPhase.SLAM_FALL;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return OrpimentSounds.WRAITH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return OrpimentSounds.WRAITH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return OrpimentSounds.WRAITH_DEATH;
    }

    private static class AimlessDriftGoal extends Goal {
        private final Wraith wraith;
        private Vec3 wanderTarget = null;

        public AimlessDriftGoal(Wraith wraith) {
            this.wraith = wraith;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        public boolean canUse() {
            return this.wraith.getNearestSurvivalPlayer() == null && this.wraith.attackPhase == Wraith.AttackPhase.ORBITING;
        }

        public boolean canContinueToUse() {
            return this.wraith.getNearestSurvivalPlayer() == null && this.wraith.attackPhase == Wraith.AttackPhase.ORBITING;
        }

        public void tick() {
            if (!this.wraith.getMoveControl().hasWanted() && this.wraith.random.nextInt(7) == 0) {
                this.findNewWanderTarget();
            }

            if (this.wraith.getTarget() == null) {
                Vec3 motion = this.wraith.getDeltaMovement();
                if (motion.length() > 0.01) {
                    float yaw = -((float)Math.toDegrees(Math.atan2(motion.x, motion.z)));
                    this.wraith.setYRot(yaw);
                    this.wraith.yBodyRot = this.wraith.getYRot();
                }
            }
        }

        private void findNewWanderTarget() {
            BlockPos boundOrigin = this.wraith.blockPosition();

            for (int i = 0; i < 3; i++) {
                BlockPos targetPos = boundOrigin.offset(
                    this.wraith.random.nextInt(15) - 7, this.wraith.random.nextInt(11) - 5, this.wraith.random.nextInt(15) - 7
                );
                if (this.wraith.level().isEmptyBlock(targetPos)) {
                    this.wraith.getMoveControl().setWantedPosition(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 0.35);
                    break;
                }
            }
        }

        public void stop() {
            this.wanderTarget = null;
        }
    }

    public enum AttackPhase {
        ORBITING,
        FREEZE,
        DASH,
        RETREAT,
        ARROW_FREEZE,
        ARROW_HANG,
        ARROW_FIRE,
        WISP_FREEZE,
        WISP_SUMMON,
        SLAM_FALL,
        SLAM_GROUND,
        SLAM_BLAST;
    }

    public enum AttackType {
        SWOOP,
        BARRAGE,
        SUMMON,
        GROUND_SLAM;
    }

    private static class OrbitPlayerGoal extends Goal {
        private final Wraith wraith;
        private static final double ORBIT_RADIUS = 6.0;
        private static final double HEIGHT_OFFSET = 6.0;
        private Player targetPlayer = null;
        private Vec3 orbitCenter = null;

        private double getAngleStep() {
            return this.wraith.isPhase2() ? 0.095 : 0.055;
        }

        private double getLerpFactor() {
            return this.wraith.isPhase2() ? 0.22 : 0.15;
        }

        public OrbitPlayerGoal(Wraith wraith) {
            this.wraith = wraith;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            if (this.wraith.attackPhase != Wraith.AttackPhase.ORBITING) {
                return false;
            } else {
                Player p = this.wraith.getNearestSurvivalPlayer();
                if (p != null) {
                    this.targetPlayer = p;
                    this.orbitCenter = this.targetPlayer.position();
                    return true;
                } else {
                    return false;
                }
            }
        }

        public boolean canContinueToUse() {
            if (this.wraith.attackPhase != Wraith.AttackPhase.ORBITING) {
                return false;
            } else if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
                return false;
            } else if (!this.targetPlayer.isCreative() && !this.targetPlayer.isSpectator()) {
                double range = this.wraith.getAttributeValue(Attributes.FOLLOW_RANGE);
                return this.wraith.distanceToSqr(this.targetPlayer) <= range * range * 2.0;
            } else {
                return false;
            }
        }

        public void stop() {
            this.targetPlayer = null;
            this.orbitCenter = null;
        }

        public void tick() {
            if (this.targetPlayer != null) {
                if (this.orbitCenter == null) {
                    this.orbitCenter = this.targetPlayer.position();
                } else {
                    this.orbitCenter = this.orbitCenter.lerp(this.targetPlayer.position(), 0.1);
                }

                this.wraith.orbitAngle = this.wraith.orbitAngle + this.getAngleStep();
                if (this.wraith.orbitAngle > Math.PI * 2) {
                    this.wraith.orbitAngle -= Math.PI * 2;
                }

                double targetX = this.orbitCenter.x + 6.0 * Math.cos(this.wraith.orbitAngle);
                double targetY = this.orbitCenter.y + 6.0;
                double targetZ = this.orbitCenter.z + 6.0 * Math.sin(this.wraith.orbitAngle);
                Vec3 targetMovement = new Vec3(
                    (targetX - this.wraith.getX()) * this.getLerpFactor(),
                    (targetY - this.wraith.getY()) * this.getLerpFactor(),
                    (targetZ - this.wraith.getZ()) * this.getLerpFactor()
                );
                this.wraith.applyWallCollision(targetMovement);
                double dx = this.targetPlayer.getX() - this.wraith.getX();
                double dz = this.targetPlayer.getZ() - this.wraith.getZ();
                this.wraith.setYRot((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
                this.wraith.yHeadRot = this.wraith.getYRot();
                this.wraith.yBodyRot = this.wraith.getYRot();
            }
        }
    }

    private static class UniversalAttackGoal extends Goal {
        private final Wraith wraith;
        private Wraith.AttackType currentAttack = null;
        private boolean active = false;
        private static final int SWOOP_COOLDOWN_MIN = 120;
        private static final int SWOOP_COOLDOWN_MAX = 220;
        private static final int BARRAGE_COOLDOWN_MIN = 120;
        private static final int BARRAGE_COOLDOWN_MAX = 220;
        private static final int SUMMON_COOLDOWN_MIN = 120;
        private static final int SUMMON_COOLDOWN_MAX = 220;
        private static final int SLAM_COOLDOWN_MIN = 180;
        private static final int SLAM_COOLDOWN_MAX = 300;
        private int attackTimer = 0;
        private int fireIndex = 0;
        private int fireTimer = 0;
        private boolean hasDamagedThisSwing = false;
        private double groundY = 0.0;
        private boolean hasBlastedThisAttack = false;

        public UniversalAttackGoal(Wraith wraith) {
            this.wraith = wraith;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            if (this.wraith.attackPhase != Wraith.AttackPhase.ORBITING) {
                return false;
            } else {
                return this.wraith.universalAttackTimer > 0 ? false : this.wraith.getNearestSurvivalPlayer() != null;
            }
        }

        public boolean canContinueToUse() {
            return this.active;
        }

        public void start() {
            this.active = true;
            this.currentAttack = this.wraith.getRandomAttack();
            this.attackTimer = 0;
            switch (this.currentAttack) {
                case SWOOP:
                    this.startSwoop();
                    break;
                case BARRAGE:
                    this.startBarrage();
                    break;
                case SUMMON:
                    this.startSummon();
                    break;
                case GROUND_SLAM:
                    this.startSlam();
            }
        }

        public void tick() {
            if (this.currentAttack != null) {
                switch (this.currentAttack) {
                    case SWOOP:
                        this.tickSwoop();
                        break;
                    case BARRAGE:
                        this.tickBarrage();
                        break;
                    case SUMMON:
                        this.tickSummon();
                        break;
                    case GROUND_SLAM:
                        this.tickSlam();
                }
            }
        }

        public void stop() {
            if (this.active) {
                this.endAttack();
            }
        }

        private void startSwoop() {
            this.hasDamagedThisSwing = false;
            this.wraith.attackPhase = Wraith.AttackPhase.FREEZE;
            this.wraith.phaseTimer = 10;
            Player player = this.wraith.getNearestSurvivalPlayer();
            if (player != null) {
                this.wraith.retreatTarget = new Vec3(this.wraith.getX(), player.getY() + 6.5, this.wraith.getZ());
            }

            this.wraith
                .level()
                .playSound(
                    null,
                    this.wraith.getX(),
                    this.wraith.getY(),
                    this.wraith.getZ(),
                    OrpimentSounds.WRAITH_SWOOP,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.6F
                );
        }

        private void tickSwoop() {
            this.wraith.phaseTimer--;
            switch (this.wraith.attackPhase) {
                case FREEZE:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    Player p = this.wraith.getNearestSurvivalPlayer();
                    if (p != null) {
                        Wraith.faceEntity(this.wraith, p);
                    }

                    if (this.wraith.level() instanceof ServerLevel serverLevel && this.wraith.tickCount % 3 == 0) {
                        for (int i = 0; i < 2; i++) {
                            double ox = (this.wraith.random.nextDouble() - 0.5) * 1.2;
                            double oz = (this.wraith.random.nextDouble() - 0.5) * 1.2;
                            serverLevel.sendParticles(
                                OrpimentParticles.WRAITH_SKULL,
                                this.wraith.getX() + ox,
                                this.wraith.getY() + 2.5,
                                this.wraith.getZ() + oz,
                                1,
                                0.0,
                                0.1,
                                0.0,
                                0.05
                            );
                        }
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        this.startDashPhase();
                    }
                    break;
                case DASH:
                    if (this.wraith.dashDirection != null) {
                        this.wraith.setDeltaMovement(this.wraith.dashDirection.scale(1.85));
                    }

                    if (!this.hasDamagedThisSwing) {
                        List<Player> hit = this.wraith.level().getEntitiesOfClass(Player.class, this.wraith.getBoundingBox().inflate(0.5));
                        if (!hit.isEmpty()) {
                            this.wraith.doHurtTarget((ServerLevel) this.wraith.level(), hit.get(0));
                            this.hasDamagedThisSwing = true;
                            this.wraith
                                .level()
                                .playSound(
                                    null,
                                    this.wraith.getX(),
                                    this.wraith.getY(),
                                    this.wraith.getZ(),
                                    OrpimentSounds.WRAITH_BITE,
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.2F
                                );
                            this.wraith.phaseTimer = 0;
                        }
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        this.startRetreatPhase();
                    }
                    break;
                case RETREAT:
                    if (this.wraith.retreatTarget != null) {
                        this.wraith
                            .setDeltaMovement(
                                (this.wraith.retreatTarget.x - this.wraith.getX()) * 0.07,
                                (this.wraith.retreatTarget.y - this.wraith.getY()) * 0.07,
                                (this.wraith.retreatTarget.z - this.wraith.getZ()) * 0.07
                            );
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        this.endAttack();
                    }
            }
        }

        private void startDashPhase() {
            Player player = this.wraith.getNearestSurvivalPlayer();
            if (player == null) {
                this.endAttack();
            } else {
                this.wraith.dashDirection = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).subtract(this.wraith.position()).normalize();
                this.wraith.attackPhase = Wraith.AttackPhase.DASH;
                this.wraith.phaseTimer = 18;
            }
        }

        private void startRetreatPhase() {
            this.wraith.setDeltaMovement(Vec3.ZERO);
            this.wraith.attackPhase = Wraith.AttackPhase.RETREAT;
            this.wraith.phaseTimer = 10;
        }

        private void startBarrage() {
            this.cleanupProjectiles();
            this.wraith.setSummoning(true);
            this.fireIndex = 0;
            this.fireTimer = 0;
            this.wraith.hangingProjectiles.clear();
            this.wraith.attackPhase = Wraith.AttackPhase.ARROW_FREEZE;
            this.wraith.phaseTimer = 3;
            this.wraith
                .level()
                .playSound(
                    null,
                    this.wraith.getX(),
                    this.wraith.getY(),
                    this.wraith.getZ(),
                    OrpimentSounds.WRAITH_SWOOP,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.4F
                );
        }

        private void tickBarrage() {
            this.wraith.phaseTimer--;
            switch (this.wraith.attackPhase) {
                case ARROW_FREEZE:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    Player p = this.wraith.getNearestSurvivalPlayer();
                    if (p != null) {
                        Wraith.faceEntity(this.wraith, p);
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        int projectileCount = this.wraith.isPhase2() ? 7 : 5;
                        this.spawnHangingProjectiles(projectileCount);
                        this.wraith.attackPhase = Wraith.AttackPhase.ARROW_HANG;
                        this.wraith.phaseTimer = 4;
                    }
                    break;
                case ARROW_HANG:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    Player hangTarget = this.wraith.getNearestSurvivalPlayer();

                    for (int i = 0; i < this.wraith.hangingProjectiles.size(); i++) {
                        WispSkullProjectile projectile = this.wraith.hangingProjectiles.get(i);
                        if (projectile.isAlive()) {
                            double angle = (Math.PI * 2) / this.wraith.hangingProjectiles.size() * i;
                            double ax = this.wraith.getX() + Math.cos(angle) * 0.8;
                            double ay = this.wraith.getY() + this.wraith.getBbHeight() + 0.8;
                            double az = this.wraith.getZ() + Math.sin(angle) * 0.8;
                            projectile.setPos(ax, ay, az);
                            projectile.setDeltaMovement(Vec3.ZERO);
                            if (hangTarget != null) {
                                Vec3 dir = hangTarget.position().add(0.0, hangTarget.getBbHeight() * 0.5, 0.0).subtract(projectile.position()).normalize();
                                projectile.setYRot((float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) + 100.0));
                                projectile.setXRot((float)(-Math.toDegrees(Math.asin(dir.y))));
                            }
                        }
                    }

                    if (this.wraith.phaseTimer <= 10) {
                        this.wraith.setFiring(true);
                        this.wraith.attackPhase = Wraith.AttackPhase.ARROW_FIRE;
                        this.wraith.phaseTimer = this.wraith.hangingProjectiles.size() * 5 + 10;
                        this.fireIndex = 0;
                        this.fireTimer = 0;
                    }
                    break;
                case ARROW_FIRE:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    this.fireTimer--;
                    if (this.fireTimer <= 0 && this.fireIndex < this.wraith.hangingProjectiles.size()) {
                        WispSkullProjectile projectile = this.wraith.hangingProjectiles.get(this.fireIndex);
                        if (projectile.isAlive()) {
                            Player target = this.wraith.getNearestSurvivalPlayer();
                            if (target != null) {
                                Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(projectile.position()).normalize();
                                double spread = 0.08;
                                projectile.setDeltaMovement(
                                    aim.x + (this.wraith.random.nextDouble() - 0.5) * spread,
                                    aim.y + (this.wraith.random.nextDouble() - 0.5) * spread,
                                    aim.z + (this.wraith.random.nextDouble() - 0.5) * spread
                                );
                                projectile.setYRot((float)(Math.toDegrees(Math.atan2(aim.z, aim.x)) + 100.0));
                                projectile.setXRot((float)(-Math.toDegrees(Math.asin(aim.y))));
                                projectile.setNoGravity(false);
                                this.wraith
                                    .level()
                                    .playSound(
                                        null,
                                        projectile.getX(),
                                        projectile.getY(),
                                        projectile.getZ(),
                                        OrpimentSounds.WRAITH_SHOOT,
                                        SoundSource.HOSTILE,
                                        0.8F,
                                        0.9F + this.wraith.random.nextFloat() * 0.2F
                                    );
                            }
                        }

                        this.fireIndex++;
                        this.fireTimer = 5;
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        this.endAttack();
                    }
            }
        }

        private void spawnHangingProjectiles(int count) {
            Player p = this.wraith.getNearestSurvivalPlayer();

            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2) / count * i;
                double ax = this.wraith.getX() + Math.cos(angle) * 0.8;
                double ay = this.wraith.getY() + this.wraith.getBbHeight() + 0.8;
                double az = this.wraith.getZ() + Math.sin(angle) * 0.8;
                WispSkullProjectile projectile = new WispSkullProjectile(
                    OrpimentMobs.WISP_SKULL_PROJECTILE, this.wraith, this.wraith.level()
                );
                projectile.setPos(ax, ay, az);
                projectile.setDeltaMovement(Vec3.ZERO);
                projectile.setNoGravity(true);
                if (p != null) {
                    Vec3 dir = p.position().add(0.0, p.getBbHeight() * 0.5, 0.0).subtract(new Vec3(ax, ay, az)).normalize();
                    projectile.setYRot((float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) + 100.0));
                    projectile.setXRot((float)(-Math.toDegrees(Math.asin(dir.y))));
                }

                this.wraith.level().addFreshEntity(projectile);
                this.wraith.hangingProjectiles.add(projectile);
            }
        }

        private void startSummon() {
            this.wraith.setSummoning(true);
            this.wraith.attackPhase = Wraith.AttackPhase.WISP_FREEZE;
            this.wraith.phaseTimer = 6;
            this.wraith
                .level()
                .playSound(
                    null,
                    this.wraith.getX(),
                    this.wraith.getY(),
                    this.wraith.getZ(),
                    OrpimentSounds.WRAITH_SWOOP,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.4F
                );
        }

        private void tickSummon() {
            this.wraith.phaseTimer--;
            switch (this.wraith.attackPhase) {
                case WISP_FREEZE:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    Player p = this.wraith.getNearestSurvivalPlayer();
                    if (p != null) {
                        Wraith.faceEntity(this.wraith, p);
                    }

                    if (this.wraith.phaseTimer <= 0) {
                        this.summonWisps();
                        this.wraith.attackPhase = Wraith.AttackPhase.WISP_SUMMON;
                        this.wraith.phaseTimer = 6;
                    }
                    break;
                case WISP_SUMMON:
                    this.wraith.setDeltaMovement(Vec3.ZERO);
                    if (this.wraith.phaseTimer <= 0) {
                        this.endAttack();
                    }
            }
        }

        private void summonWisps() {
            if (this.wraith.level() instanceof ServerLevel serverLevel) {
                int count = 3;

                for (int i = 0; i < count; i++) {
                    Wisp wisp = OrpimentMobs.WISP.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                    if (wisp != null) {
                        double angle = (Math.PI * 2) / count * i;
                        double spawnX = this.wraith.getX() + Math.cos(angle) * 1.5;
                        double spawnY = this.wraith.getY();
                        double spawnZ = this.wraith.getZ() + Math.sin(angle) * 1.5;
                        wisp.snapTo(spawnX, spawnY, spawnZ, this.wraith.getYRot(), 0.0F);
                        wisp.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(wisp.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
                        serverLevel.addFreshEntityWithPassengers(wisp);
                        serverLevel.playSound(
                            null,
                            spawnX,
                            spawnY,
                            spawnZ,
                            OrpimentSounds.WRAITH_SUMMON,
                            SoundSource.HOSTILE,
                            0.6F,
                            1.6F + this.wraith.random.nextFloat() * 0.3F
                        );
                    }
                }
            }
        }

        private void startSlam() {
            this.hasBlastedThisAttack = false;
            this.wraith.attackPhase = Wraith.AttackPhase.SLAM_FALL;
            this.wraith.phaseTimer = 0;
            this.wraith
                .level()
                .playSound(
                    null,
                    this.wraith.getX(),
                    this.wraith.getY(),
                    this.wraith.getZ(),
                    OrpimentSounds.WRAITH_SWOOP,
                    SoundSource.HOSTILE,
                    1.2F,
                    0.3F
                );
        }

        private void tickSlam() {
            switch (this.wraith.attackPhase) {
                case SLAM_FALL:
                    Vec3 vel = this.wraith.getDeltaMovement();
                    this.wraith.setDeltaMovement(vel.x * 0.1, vel.y, vel.z * 0.1);
                    if (this.wraith.tickCount % 5 == 0 && this.wraith.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 3; i++) {
                            double ox = (this.wraith.random.nextDouble() - 0.5) * 1.5;
                            double oz = (this.wraith.random.nextDouble() - 0.5) * 1.5;
                            serverLevel.sendParticles(
                                OrpimentParticles.WRAITH_SKULL,
                                this.wraith.getX() + ox,
                                this.wraith.getY() + 2.5,
                                this.wraith.getZ() + oz,
                                1,
                                0.0,
                                0.1,
                                0.0,
                                0.05
                            );
                        }
                    }

                    if (this.wraith.onGround()) {
                        this.groundY = this.wraith.getY();
                        this.wraith.applySlamResistance();
                        this.wraith.attackPhase = Wraith.AttackPhase.SLAM_GROUND;
                        this.wraith.phaseTimer = 40;
                        this.wraith
                            .level()
                            .playSound(
                                null,
                                this.wraith.getX(),
                                this.wraith.getY(),
                                this.wraith.getZ(),
                                OrpimentSounds.WRAITH_SMASH,
                                SoundSource.HOSTILE,
                                0.7F,
                                1.8F
                            );
                    }
                    break;
                case SLAM_GROUND:
                    this.wraith.setDeltaMovement(0.0, 0.0, 0.0);
                    this.wraith.setPos(this.wraith.getX(), this.groundY, this.wraith.getZ());
                    Player p = this.wraith.getNearestSurvivalPlayer();
                    if (p != null) {
                        Wraith.faceEntity(this.wraith, p);
                    }

                    this.wraith.phaseTimer--;
                    if (this.wraith.phaseTimer <= 0) {
                        this.wraith.attackPhase = Wraith.AttackPhase.SLAM_BLAST;
                    }
                    break;
                case SLAM_BLAST:
                    if (!this.hasBlastedThisAttack) {
                        this.hasBlastedThisAttack = true;
                        this.doBlast();
                    }

                    this.wraith.removeSlamResistance();
                    Player blastTarget = this.wraith.getNearestSurvivalPlayer();
                    double targetY = (blastTarget != null ? blastTarget.getY() : this.wraith.getY()) + 6.5;
                    this.wraith.setDeltaMovement(this.wraith.getDeltaMovement().x * 0.3, (targetY - this.wraith.getY()) * 0.08, this.wraith.getDeltaMovement().z * 0.3);
                    if (this.wraith.getY() >= targetY - 1.5) {
                        this.endAttack();
                    }
            }
        }

        private void doBlast() {
            double BLAST_RADIUS = 6.0;
            double KNOCKBACK_Y = 0.6;
            double KNOCKBACK_XZ = 1.8;
            int POSSESSED_TICKS = 200;
            int POSSESSED_AMP = 0;
            this.wraith
                .level()
                .playSound(
                    null,
                    this.wraith.getX(),
                    this.wraith.getY(),
                    this.wraith.getZ(),
                    OrpimentSounds.WRAITH_SHOOT,
                    SoundSource.HOSTILE,
                    1.5F,
                    0.5F
                );
            if (this.wraith.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.wraith.getX(), this.wraith.getY() + 1.0, this.wraith.getZ(), 3, 0.5, 0.5, 0.5, 0.0);

                for (int i = 0; i < 30; i++) {
                    double ox = (this.wraith.random.nextDouble() - 0.5) * BLAST_RADIUS;
                    double oz = (this.wraith.random.nextDouble() - 0.5) * BLAST_RADIUS;
                    serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME, this.wraith.getX() + ox, this.wraith.getY() + 0.5, this.wraith.getZ() + oz, 1, 0.0, 0.1, 0.0, 0.05
                    );
                }

                for (int i = 0; i < 20; i++) {
                    double ox = (this.wraith.random.nextDouble() - 0.5) * BLAST_RADIUS;
                    double oz = (this.wraith.random.nextDouble() - 0.5) * BLAST_RADIUS;
                    serverLevel.sendParticles(
                        OrpimentParticles.WRAITH_SKULL,
                        this.wraith.getX() + ox,
                        this.wraith.getY() + 1.0,
                        this.wraith.getZ() + oz,
                        1,
                        0.0,
                        0.2,
                        0.0,
                        0.1
                    );
                }
            }

            AABB blastBox = this.wraith.getBoundingBox().inflate(BLAST_RADIUS);

            for (LivingEntity entity : this.wraith.level().getEntitiesOfClass(LivingEntity.class, blastBox)) {
                if (entity != this.wraith) {
                    Vec3 diff = entity.position().subtract(this.wraith.position());
                    double dist = diff.length();
                    if (dist == 0.0) {
                        diff = new Vec3(1.0, 0.0, 0.0);
                    } else {
                        diff = diff.normalize();
                    }

                    double scale = 1.0 - dist / BLAST_RADIUS;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(diff.x * KNOCKBACK_XZ * scale, KNOCKBACK_Y * scale, diff.z * KNOCKBACK_XZ * scale));
                    entity.needsSync = true;
                    entity.addEffect(new MobEffectInstance(OrpimentEffects.POSSESSED, POSSESSED_TICKS, POSSESSED_AMP, false, true));
                }
            }
        }

        private void endAttack() {
            this.active = false;
            this.wraith.setSummoning(false);
            this.wraith.setFiring(false);
            this.cleanupProjectiles();
            int cooldownMin;
            int cooldownMax;
            switch (this.currentAttack) {
                case SWOOP:
                    cooldownMin = 120;
                    cooldownMax = 220;
                    break;
                case BARRAGE:
                    cooldownMin = 120;
                    cooldownMax = 220;
                    break;
                case SUMMON:
                    cooldownMin = 120;
                    cooldownMax = 220;
                    break;
                case GROUND_SLAM:
                    cooldownMin = 180;
                    cooldownMax = 300;
                    break;
                default:
                    cooldownMin = 120;
                    cooldownMax = 220;
            }

            this.wraith.lastAttack = this.currentAttack;
            this.wraith.returnToOrbit(cooldownMin, cooldownMax);
        }

        private void cleanupProjectiles() {
            for (WispSkullProjectile p : this.wraith.hangingProjectiles) {
                if (p != null && p.isAlive()) {
                    p.discard();
                }
            }

            this.wraith.hangingProjectiles.clear();
        }
    }

    private static class WraithMoveControl extends MoveControl {
        public WraithMoveControl(Wraith wraith) {
            super(wraith);
        }

        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                Vec3 vec3 = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
                double d0 = vec3.length();
                if (d0 < this.mob.getBoundingBox().getSize()) {
                    this.operation = Operation.WAIT;
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(0.5));
                } else {
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(vec3.scale(this.speedModifier * 0.05 / d0)));
                    if (this.mob.getTarget() == null) {
                        Vec3 motion = this.mob.getDeltaMovement();
                        this.mob.setYRot(-((float)Math.toDegrees(Math.atan2(motion.x, motion.z))));
                        this.mob.yBodyRot = this.mob.getYRot();
                    } else {
                        double dx = this.mob.getTarget().getX() - this.mob.getX();
                        double dz = this.mob.getTarget().getZ() - this.mob.getZ();
                        this.mob.setYRot(-((float)Math.toDegrees(Math.atan2(dx, dz))));
                        this.mob.yBodyRot = this.mob.getYRot();
                    }
                }
            }
        }
    }
}

