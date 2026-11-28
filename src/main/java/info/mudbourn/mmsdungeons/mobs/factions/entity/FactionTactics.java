package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionClass;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;

// The thinking side of a faction fighter: rolls, crawling through gaps, digging to a hidden target, healing from supplies, flanking and picking targets.
final class FactionTactics {

    private static final float ROLL_SPEED = 0.85f;
    private static final int ROLL_COOLDOWN = 80;
    private static final int ROLL_TURN_TICKS = 2;
    // Within this many blocks of a target it can see, a member fights on its feet and never looks for a gap to crawl.
    private static final double CRAWL_FIGHT_RANGE = 6.0;
    // Ticks after being hurt that a member counts as in danger and won't drop to a crawl.
    private static final int CRAWL_DANGER_TICKS = 60;
    private static final double ROLL_REACH = 3.0;
    private static final float LOW_HEALTH = 0.35f;
    private static final float RETREAT_HEALTH = 0.3f;
    private static final float SUPPLY_HEALTH = 0.5f;
    private static final float EMERGENCY_HEALTH = 0.25f;
    private static final double DRINK_DISTANCE = 3.0;
    private static final double EAT_DISTANCE = 5.0;
    private static final float CONSUME_BACKPEDAL = -0.35f;
    private static final float CONSUME_SIDESTEP = 0.2f;
    private static final double HEALER_RANGE = 24.0;
    private static final int REST_HEAL_TICKS = 20;
    private static final double SAFE_DISTANCE = 8.0;
    private static final int STUCK_TICKS = 40;
    private static final double DIG_RANGE = 16.0;
    private static final float WRONG_TOOL_PENALTY = 100.0f / 30.0f;
    private static final float MAX_HARDNESS = 5.0f;
    private static final int RETHINK_TICKS = 40;
    private static final double TARGET_RANGE = 16.0;
    private static final double FLANK_OFFSET = 3.0;

    private final FactionMember mob;
    private int rollReady = 40;
    private Vec3 rollDirection = Vec3.ZERO;
    private int turnTicks;
    private int unseenTicks;
    private int stuckTicks;
    private @Nullable BlockPos digging;
    private int digProgress;
    private int digNeeded;
    private int flankSide;

    FactionTactics(FactionMember mob) {
        this.mob = mob;
        this.flankSide = mob.getRandom().nextBoolean() ? 1 : -1;
    }

    void reset() {
        this.stopDigging();
        this.unseenTicks = 0;
        this.stuckTicks = 0;
    }

    // Turning into a roll or rolling, when the roll owns the mob's facing.
    boolean rolling() {
        return this.turnTicks > 0 || this.mob.rollTicks() > 0;
    }

    // Runs before the class behavior; true when a tactic took over this tick.
    boolean tick(ServerLevel level, LivingEntity target) {
        if (this.mob.flight().airborne()) {
            return this.mob.consuming() || this.trySupply(target, this.mob.getSensing().hasLineOfSight(target));
        }
        if (this.turnTicks > 0) {
            this.tickTurn();
            return true;
        }
        if (this.mob.rollTicks() > 0) {
            this.tickRoll();
            return true;
        }
        if (this.mob.consuming()) {
            this.backOff(target);
            return true;
        }
        this.tickCrawl(level, target);
        if (this.mob.tickCount % RETHINK_TICKS == 0) {
            this.rethinkTarget(level, target);
        }
        boolean sees = this.mob.getSensing().hasLineOfSight(target);
        this.unseenTicks = sees ? 0 : this.unseenTicks + 1;
        if (this.tickDig(level, target, sees)) {
            return true;
        }
        if (this.tryRoll(target, sees)) {
            return true;
        }
        if (this.trySupply(target, sees)) {
            return true;
        }
        return this.retreat(level, target);
    }

    // A combat roll, mirroring Combat Roll's 8 tick roll: away when hurt, crowded or kiting, toward when closing in.
    private boolean tryRoll(LivingEntity target, boolean sees) {
        if (this.mob.tickCount < this.rollReady || !this.mob.onGround() || this.mob.crawling() || !sees) {
            return false;
        }
        double distance = this.mob.distanceTo(target);
        FactionClass type = this.mob.factionClass();
        boolean hurt = this.mob.getHealth() < this.mob.getMaxHealth() * LOW_HEALTH && distance < 4.0;
        boolean shielded = this.mob.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
        boolean windup = !shielded && target.isSwinging() && distance < 3.5 && this.mob.getRandom().nextFloat() < 0.35f;
        boolean kiting = !type.melee() && distance < 4.5;
        Vec3 away = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        if (hurt || windup || kiting) {
            Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(this.mob.getRandom().nextBoolean() ? 0.6 : -0.6);
            return this.startRoll(away.add(side).normalize());
        }
        boolean closing = type.melee() && distance > 5.0 && distance < 9.0 && this.mob.getRandom().nextFloat() < 0.05f;
        return closing && this.startRoll(away.scale(-1.0));
    }

    // A roll along a flat direction while travelling, when its cooldown is up and the landing is safe.
    boolean travelRoll(Vec3 direction) {
        if (this.mob.tickCount < this.rollReady || !this.mob.onGround() || this.mob.crawling()) {
            return false;
        }
        return this.startRoll(direction.multiply(1.0, 0.0, 1.0).normalize());
    }

    // Turns to face the way it will roll; the roll itself starts once the turn is done.
    private boolean startRoll(Vec3 direction) {
        if (!this.safeLanding(direction)) {
            return false;
        }
        this.rollDirection = direction;
        this.turnTicks = ROLL_TURN_TICKS;
        this.mob.getNavigation().stop();
        this.mob.stopUsingItem();
        this.mob.caster().stop();
        this.mob.setAction(FactionMember.ACTION_NONE);
        this.face(direction);
        this.rollReady = this.mob.tickCount + ROLL_COOLDOWN + this.mob.getRandom().nextInt(40);
        return true;
    }

    private void tickTurn() {
        this.mob.getNavigation().stop();
        this.face(this.rollDirection);
        if (--this.turnTicks > 0) {
            return;
        }
        this.mob.setRollTicks(FactionMember.ROLL_TICKS);
        this.mob.setAction(FactionMember.ACTION_ROLL);
    }

    // Points body, head and eyes along a flat direction.
    private void face(Vec3 direction) {
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0f;
        this.mob.setYRot(yaw);
        this.mob.setYHeadRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setXRot(0.0f);
    }

    // Never rolls off a ledge or into fire, lava or a wall.
    private boolean safeLanding(Vec3 direction) {
        if (!(this.mob.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos land = BlockPos.containing(this.mob.position().add(direction.scale(ROLL_REACH)));
        BlockState feet = stateNow(level, land);
        BlockState head = stateNow(level, land.above());
        BlockState floor = stateNow(level, land.below());
        if (feet == null || head == null || floor == null) {
            return false;
        }
        boolean clear = feet.getCollisionShape(level, land).isEmpty() && head.getCollisionShape(level, land.above()).isEmpty();
        boolean hazard = feet.getFluidState().is(FluidTags.LAVA) || floor.getFluidState().is(FluidTags.LAVA) || feet.is(BlockTags.FIRE);
        return clear && !hazard && floor.isFaceSturdy(level, land.below(), Direction.UP);
    }

    private void tickRoll() {
        int left = this.mob.rollTicks() - 1;
        this.mob.setRollTicks(left);
        this.face(this.rollDirection);
        if (this.mob.onGround()) {
            Vec3 motion = this.rollDirection.scale(ROLL_SPEED * (0.5 + 0.5 * left / FactionMember.ROLL_TICKS));
            this.mob.setDeltaMovement(motion.x, this.mob.getDeltaMovement().y, motion.z);
        }
        if (left == 0) {
            this.mob.setAction(FactionMember.ACTION_NONE);
        }
    }

    // Drops to a crawl only when the path it would take runs through a one-high gap, and stands once nothing low is left ahead.
    private void tickCrawl(ServerLevel level, LivingEntity target) {
        // Crawling is slow and leaves it open, so it is a last resort: never with a target close and in sight, or while it is being hurt.
        boolean hurtRecently = this.mob.tickCount - this.mob.getLastHurtByMobTimestamp() < CRAWL_DANGER_TICKS;
        boolean close = hurtRecently || this.mob.distanceTo(target) < CRAWL_FIGHT_RANGE && this.mob.getSensing().hasLineOfSight(target);
        if (this.mob.crawling()) {
            Path path = this.mob.getNavigation().getPath();
            // Stands to fight a target in reach, and whenever nothing low is left ahead.
            if (this.mob.roomToStand() && (close || path == null || !this.mob.lowGapAhead(path, FactionMember.CRAWL_LOOKAHEAD))) {
                this.mob.setCrawling(false);
            }
            return;
        }
        Path path = this.mob.getNavigation().getPath();
        boolean blocked = path == null || !path.canReach() || this.mob.getNavigation().isStuck();
        this.stuckTicks = blocked && !close ? this.stuckTicks + 1 : 0;
        if (this.stuckTicks < 20 || this.mob.tickCount % 10 != 0) {
            return;
        }
        this.stuckTicks = 0;
        // Jumps a step up the path instead of crawling.
        if (path != null && !path.isDone() && path.getNextNodePos().getY() > this.mob.getBlockY() && this.mob.onGround()) {
            this.mob.getJumpControl().jump();
            return;
        }
        // No way through on its feet: plans the route at crawl height, and crawls only when that route really runs under a block.
        this.mob.setCrawling(true);
        Path low = this.mob.getNavigation().createPath(target, 0);
        if (low != null && low.canReach() && this.mob.lowGapAhead(low, low.getNodeCount())) {
            this.mob.getNavigation().moveTo(low, 1.0);
        } else {
            this.mob.setCrawling(false);
        }
    }

    // Digs through the wall a target hides behind: only the blocks between them, at a pace set by hardness.
    private boolean tickDig(ServerLevel level, LivingEntity target, boolean sees) {
        if (sees || this.unseenTicks < STUCK_TICKS || this.mob.distanceTo(target) > DIG_RANGE) {
            this.stopDigging();
            return false;
        }
        Path path = this.mob.getNavigation().getPath();
        if (path != null && path.canReach() && !this.mob.getNavigation().isStuck()) {
            this.stopDigging();
            return false;
        }
        if (this.digging == null) {
            BlockPos wall = this.wallBetween(level, target);
            if (wall == null || !this.mayBreak(level, wall)) {
                return false;
            }
            this.digging = wall;
            this.digProgress = 0;
            BlockState state = level.getBlockState(wall);
            float speed = this.mob.drawTool(state);
            float hardness = state.getDestroySpeed(level, wall);
            float penalty = state.requiresCorrectToolForDrops() && !this.mob.getMainHandItem().isCorrectToolForDrops(state) ? WRONG_TOOL_PENALTY : 1.0f;
            this.digNeeded = 20 + Math.round(hardness * 30.0f * penalty / speed);
        }
        BlockPos wall = this.digging;
        if (this.mob.distanceToSqr(Vec3.atCenterOf(wall)) > 9.0) {
            this.mob.getNavigation().moveTo(wall.getX() + 0.5, wall.getY(), wall.getZ() + 0.5, 1.0);
            return true;
        }
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(Vec3.atCenterOf(wall));
        if (++this.digProgress % 5 == 0) {
            this.mob.swing(InteractionHand.MAIN_HAND, this.mob.getMainHandItem().getAttackAnimation(), true);
            level.playSound(null, wall, level.getBlockState(wall).getSoundType().getHitSound(), this.mob.getSoundSource(), 0.5f, 0.8f);
        }
        level.destroyBlockProgress(this.mob.getId(), wall, Math.min(9, this.digProgress * 10 / this.digNeeded));
        if (this.digProgress >= this.digNeeded) {
            BlockState state = level.getBlockState(wall);
            float hardness = state.getDestroySpeed(level, wall);
            level.destroyBlock(wall, true, this.mob, 512);
            ItemStack held = this.mob.getMainHandItem();
            if (held.has(DataComponents.TOOL) && hardness != 0.0f) {
                held.hurtAndBreak(1, this.mob, EquipmentSlot.MAINHAND);
            }
            this.stopDigging();
        }
        return true;
    }

    private void stopDigging() {
        if (this.digging != null && this.mob.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(this.mob.getId(), this.digging, -1);
        }
        this.digging = null;
        this.mob.stowTool();
    }

    // The nearest solid block on the line from the mob's feet or head to the target.
    private @Nullable BlockPos wallBetween(ServerLevel level, LivingEntity target) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (double height : new double[] {0.5, 1.5}) {
            Vec3 from = this.mob.position().add(0.0, height, 0.0);
            Vec3 to = target.position().add(0.0, height, 0.0);
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
            if (hit.getType() == HitResult.Type.BLOCK) {
                double distance = hit.getLocation().distanceToSqr(from);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = hit.getBlockPos();
                }
            }
        }
        return best;
    }

    // Mob griefing on, an ordinary block with no block entity, and no claim over it.
    private boolean mayBreak(ServerLevel level, BlockPos pos) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0f || hardness > MAX_HARDNESS || state.hasBlockEntity() || state.isAir()) {
            return false;
        }
        String namespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        if (namespace.equals("mms_dungeons")) {
            return false;
        }
        return OpenPACServerAPI.get(level.getServer()).getServerClaimsManager().get(level.dimension().identifier(), pos) == null;
    }

    // A hurt fighter drinks a potion even close in, and eats only with room to spare, as a player would.
    private boolean trySupply(LivingEntity target, boolean sees) {
        float health = this.mob.getHealth() / this.mob.getMaxHealth();
        if (health >= SUPPLY_HEALTH || this.mob.isUsingItem() && !this.mob.getUseItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return false;
        }
        double distance = this.mob.distanceTo(target);
        boolean drink = this.mob.hasSupply(true) && (health < EMERGENCY_HEALTH || distance >= DRINK_DISTANCE || !sees);
        if (drink && this.mob.startConsuming(true)) {
            this.mob.caster().stop();
            return true;
        }
        boolean eat = this.mob.hasSupply(false) && (distance >= EAT_DISTANCE || !sees);
        if (eat && this.mob.startConsuming(false)) {
            this.mob.caster().stop();
            return true;
        }
        return false;
    }

    // Eating or drinking: backs away from the target at a walk, still facing it.
    private void backOff(LivingEntity target) {
        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        this.mob.getMoveControl().strafe(CONSUME_BACKPEDAL, CONSUME_SIDESTEP * this.flankSide);
    }

    // Badly hurt fighters fall back to a faction priest, or get clear of the target to eat, or rest out of reach once the food is gone.
    private boolean retreat(ServerLevel level, LivingEntity target) {
        if (this.mob.getHealth() >= this.mob.getMaxHealth() * RETREAT_HEALTH || this.mob.factionClass() == FactionClass.PRIEST) {
            return false;
        }
        double distance = this.mob.distanceTo(target);
        if (distance < 2.5) {
            return false;
        }
        FactionMember healer = level.getEntitiesOfClass(
                FactionMember.class,
                this.mob.getBoundingBox().inflate(HEALER_RANGE),
                member -> member != this.mob && member.faction() == this.mob.faction() && member.factionClass() == FactionClass.PRIEST
            )
            .stream()
            .min(Comparator.comparingDouble(this.mob::distanceToSqr))
            .orElse(null);
        if (healer != null) {
            if (this.mob.distanceTo(healer) > 3.0) {
                this.mob.getNavigation().moveTo(healer, 1.0);
            }
            return true;
        }
        boolean food = this.mob.hasSupply(false);
        if (distance < (food ? EAT_DISTANCE : SAFE_DISTANCE)) {
            Vec3 away = this.mob.position().add(this.mob.position().subtract(target.position()).normalize().scale(8.0));
            this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
            return true;
        }
        if (food) {
            return false;
        }
        if (this.mob.tickCount % REST_HEAL_TICKS == 0) {
            this.mob.heal(1.0f);
        }
        return distance < SAFE_DISTANCE * 1.5;
    }

    // Switches to a clearly better player: close, weak, or hitting a crewmate. Followers leave this to their leader.
    private void rethinkTarget(ServerLevel level, LivingEntity current) {
        if (this.mob.liveLeader() != null || this.mob.isNeutral()) {
            return;
        }
        List<Player> players = level.getEntitiesOfClass(
            Player.class,
            this.mob.getBoundingBox().inflate(TARGET_RANGE),
            player -> player.isAlive()
                && !player.isCreative()
                && !player.isSpectator()
                && !this.mob.cowedBy(player)
                && this.mob.getSensing().hasLineOfSight(player)
        );
        Player best = null;
        double bestScore = this.score(level, current);
        for (Player player : players) {
            double score = this.score(level, player);
            if (score < bestScore - 4.0) {
                best = player;
                bestScore = score;
            }
        }
        if (best != null) {
            this.mob.setTarget(best);
        }
    }

    private double score(ServerLevel level, LivingEntity candidate) {
        double score = this.mob.distanceTo(candidate);
        score -= (1.0 - candidate.getHealth() / candidate.getMaxHealth()) * 6.0;
        boolean hittingCrew = !level.getEntitiesOfClass(
            FactionMember.class,
            this.mob.getBoundingBox().inflate(TARGET_RANGE),
            member -> member.faction() == this.mob.faction() && member.getLastHurtByMob() == candidate
        ).isEmpty();
        return hittingCrew ? score - 5.0 : score;
    }

    // Where a melee fighter should run: straight in when alone, around the side when a crewmate already holds the front.
    Vec3 approachPoint(ServerLevel level, LivingEntity target) {
        double distance = this.mob.distanceTo(target);
        AABB around = target.getBoundingBox().inflate(3.0);
        boolean engaged = !level.getEntitiesOfClass(
            FactionMember.class,
            around,
            member -> member != this.mob && member.faction() == this.mob.faction() && member.factionClass().melee()
        ).isEmpty();
        if (!engaged || distance < 4.0) {
            return target.position();
        }
        Vec3 toMob = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 side = new Vec3(-toMob.z, 0.0, toMob.x).scale(FLANK_OFFSET * this.flankSide);
        return target.position().add(side).add(toMob.scale(1.5));
    }

    int flankSide() {
        return this.flankSide;
    }

    private static @Nullable BlockState stateNow(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
