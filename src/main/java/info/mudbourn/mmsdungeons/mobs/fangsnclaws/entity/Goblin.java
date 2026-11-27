package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import info.mudbourn.mmsrendercommon.trigger.GeoTriggers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A small thief that darts in after each hit, steals from player inventories, and circles its target in a group.
public class Goblin extends Monster {

    private static final double CHASE_SPEED = 0.45;
    private static final double FLEE_SPEED = 0.58;
    private static final double STEAL_FLEE_SPEED = 0.78;
    private static final double ORBIT_SPEED = 0.65;
    private static final double RUSH_SPEED = 0.55;
    private static final double ORBIT_RADIUS = 4.5;
    private static final double ORBIT_STEP = 0.025;
    private static final double ALLY_RANGE = 12.0;
    private static final double LOOK_AWAY_DOT = 0.3;
    private static final int ORBIT_FLIP_MIN = 80;
    private static final int ORBIT_FLIP_SPREAD = 120;
    private static final int FLEE_TICKS = 30;
    private static final int STEAL_FLEE_TICKS = 65;
    private static final int ATTACK_COOLDOWN = 80;
    private static final int STEAL_COOLDOWN = 160;
    private static final int ATTACK_HIT_TICK = 8;
    private static final int ATTACK_TOTAL_TICKS = 18;
    private static final int STEAL_HIT_TICK = 12;
    private static final int STEAL_TOTAL_TICKS = 24;
    private static final int STEAL_ODDS = 4;
    private static final int STUCK_TICKS = 5;
    private static final double STUCK_MOVE_SQUARED = 0.0025;
    private static final int FIRST_STEAL_SLOT = 9;
    private static final int LAST_STEAL_SLOT = 35;

    private LivingEntity pendingTarget;
    private int attackTick;
    private int stealTick;
    private int fleeTick;
    private int attackCooldown;
    private boolean stealFlee;
    private float orbitAngle;
    private int orbitDirection;
    private int orbitFlipTick;
    private double lastX;
    private double lastZ;
    private int stuckTick;

    public Goblin(EntityType<? extends Goblin> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        this.orbitAngle = (float) (this.random.nextDouble() * Math.PI * 2.0);
        this.orbitDirection = this.random.nextBoolean() ? 1 : -1;
        this.orbitFlipTick = ORBIT_FLIP_MIN + this.random.nextInt(ORBIT_FLIP_SPREAD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 1.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        ItemStack loot = this.getItemBySlot(EquipmentSlot.OFFHAND);
        if (hurt && !loot.isEmpty()) {
            this.spawnAtLocation(level, loot);
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        return hurt;
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() || super.isCurrentlyGlowing();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        this.tickAttack(level);
        this.tickSteal(level);
        this.tickCombat();
    }

    private boolean isActing() {
        return this.attackTick > 0 || this.stealTick > 0;
    }

    private double reach() {
        return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    private void tickAttack(ServerLevel level) {
        if (this.attackTick <= 0) {
            return;
        }
        this.attackTick++;
        if (this.attackTick == ATTACK_HIT_TICK) {
            boolean close = this.pendingTarget != null && this.distanceTo(this.pendingTarget) <= this.reach() + 0.5;
            if (close && this.pendingTarget.isAlive()) {
                super.doHurtTarget(level, this.pendingTarget);
            }
            this.pendingTarget = null;
        }
        if (this.attackTick >= ATTACK_TOTAL_TICKS) {
            this.attackTick = 0;
        }
    }

    private void tickSteal(ServerLevel level) {
        if (this.stealTick <= 0) {
            return;
        }
        this.stealTick++;
        if (this.stealTick == STEAL_HIT_TICK) {
            boolean close = this.pendingTarget != null && this.distanceTo(this.pendingTarget) <= this.reach() + 0.8;
            if (close && this.pendingTarget instanceof Player player && player.isAlive()) {
                this.steal(player);
            }
            this.pendingTarget = null;
        }
        if (this.stealTick >= STEAL_TOTAL_TICKS) {
            this.stealTick = 0;
        }
    }

    private void steal(Player player) {
        List<Integer> filled = new ArrayList<>();
        for (int slot = FIRST_STEAL_SLOT; slot <= LAST_STEAL_SLOT; slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) {
                filled.add(slot);
            }
        }
        if (filled.isEmpty()) {
            return;
        }
        int slot = filled.get(this.random.nextInt(filled.size()));
        this.setItemSlot(EquipmentSlot.OFFHAND, player.getInventory().getItem(slot).copy());
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        this.playSound(FangsNClawsSounds.GOBLIN_CELEBRATE, 1.0f, 0.9f + this.random.nextFloat() * 0.2f);
        this.stealFlee = true;
        this.fleeTick = STEAL_FLEE_TICKS;
        this.attackCooldown = STEAL_COOLDOWN;
    }

    private void startAttack(LivingEntity target) {
        if (this.isActing() || this.attackCooldown > 0) {
            return;
        }
        this.pendingTarget = target;
        if (target instanceof Player && this.random.nextInt(STEAL_ODDS) == 0) {
            GeoTriggers.trigger(this, "steal");
            this.stealTick = 1;
        } else {
            GeoTriggers.trigger(this, "attack");
            this.attackTick = 1;
        }
    }

    private int nearbyAllies() {
        return this.level()
            .getEntitiesOfClass(
                Goblin.class,
                this.getBoundingBox().inflate(ALLY_RANGE),
                ally -> ally != this && ally.isAlive() && ally.getTarget() != null
            )
            .size();
    }

    private boolean isLookingAway(LivingEntity target) {
        Vec3 toSelf = this.position().subtract(target.position()).normalize();
        return target.getLookAngle().dot(toSelf) < LOOK_AWAY_DOT;
    }

    private void tickCombat() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.stuckTick = 0;
            return;
        }
        double moved = Math.pow(this.getX() - this.lastX, 2.0) + Math.pow(this.getZ() - this.lastZ, 2.0);
        if (!this.isActing() && moved < STUCK_MOVE_SQUARED) {
            this.stuckTick++;
            if (this.stuckTick >= STUCK_TICKS && this.onGround()) {
                this.getJumpControl().jump();
                this.stuckTick = 0;
            }
        } else {
            this.stuckTick = 0;
        }
        this.lastX = this.getX();
        this.lastZ = this.getZ();
        if (this.isActing()) {
            return;
        }
        if (this.nearbyAllies() >= 1) {
            this.tickGroup(target);
        } else {
            this.tickSolo(target);
        }
    }

    private void flee(LivingEntity target, double speed, double blend) {
        Vec3 away = this.position().subtract(target.position());
        Vec3 goal = this.position().add(away.length() > 0.01 ? away.normalize().scale(1.5) : new Vec3(1.0, 0.0, 0.0));
        Steering.faceToward(this, goal);
        Steering.walkToward(this, goal, speed, blend);
        if (this.fleeTick == 0) {
            this.stealFlee = false;
        }
    }

    private void tickSolo(LivingEntity target) {
        if (this.fleeTick > 0) {
            this.fleeTick--;
            this.flee(target, this.stealFlee ? STEAL_FLEE_SPEED : FLEE_SPEED, 0.22);
            return;
        }
        Vec3 goal = target.position();
        Steering.faceToward(this, goal);
        if (this.distanceTo(target) <= this.reach() && this.attackCooldown == 0) {
            this.startAttack(target);
            this.fleeTick = FLEE_TICKS;
            this.attackCooldown = ATTACK_COOLDOWN;
        } else if (this.hurtTime == 0) {
            Steering.walkToward(this, goal, CHASE_SPEED, 0.2);
        }
    }

    private void tickGroup(LivingEntity target) {
        if (this.fleeTick > 0 && this.stealFlee) {
            this.fleeTick--;
            this.flee(target, STEAL_FLEE_SPEED, 0.25);
            return;
        }
        if (this.isLookingAway(target) && this.attackCooldown == 0) {
            Steering.faceToward(this, target.position());
            if (this.distanceTo(target) <= this.reach()) {
                this.startAttack(target);
                this.attackCooldown = ATTACK_COOLDOWN;
            } else {
                Steering.walkToward(this, target.position(), RUSH_SPEED, 0.25);
            }
            return;
        }
        if (--this.orbitFlipTick <= 0) {
            this.orbitDirection = -this.orbitDirection;
            this.orbitFlipTick = ORBIT_FLIP_MIN + this.random.nextInt(ORBIT_FLIP_SPREAD);
        }
        this.orbitAngle += (float) (ORBIT_STEP * this.orbitDirection);
        Vec3 ring = new Vec3(
            target.getX() + Math.cos(this.orbitAngle) * ORBIT_RADIUS,
            target.getY(),
            target.getZ() + Math.sin(this.orbitAngle) * ORBIT_RADIUS
        );
        Steering.faceToward(this, target.position());
        Steering.walkToward(this, ring, ORBIT_SPEED, 0.15);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.GOBLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.GOBLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.GOBLIN_DEATH;
    }
}
