package info.mudbourn.mmsdungeons.mobs.factions.entity;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jspecify.annotations.Nullable;

// Notices a mob closing in to attack: one targeting this member, within 4 blocks and in its line of sight, becomes its target unless it is already fighting something close.
final class FactionThreatSense extends TargetGoal {

    private static final double RANGE = 4.0;
    private static final int SCAN_TICKS = 5;
    private static final double ENGAGED_RANGE = 8.0;

    private final FactionMember member;
    private @Nullable Mob threat;

    FactionThreatSense(FactionMember member) {
        super(member, true);
        this.member = member;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.member.downed() || this.member.tickCount % SCAN_TICKS != 0) {
            return false;
        }
        this.threat = this.member.level().getEntitiesOfClass(Mob.class, this.member.getBoundingBox().inflate(RANGE), this::threatens)
            .stream()
            .min(Comparator.comparingDouble(this.member::distanceToSqr))
            .orElse(null);
        return this.threat != null && this.threat != this.member.getTarget() && !this.engaged();
    }

    @Override
    public void start() {
        this.member.setTarget(this.threat);
        super.start();
    }

    // Already fighting something close and in sight, it finishes that fight rather than turning on each newcomer.
    private boolean engaged() {
        LivingEntity target = this.member.getTarget();
        return target != null && target.isAlive() && this.member.distanceTo(target) <= ENGAGED_RANGE && this.member.getSensing().hasLineOfSight(target);
    }

    private boolean threatens(Mob mob) {
        return mob != this.member
            && mob.isAlive()
            && mob.getTarget() == this.member
            && this.member.distanceTo(mob) <= RANGE
            && this.member.getSensing().hasLineOfSight(mob);
    }
}
