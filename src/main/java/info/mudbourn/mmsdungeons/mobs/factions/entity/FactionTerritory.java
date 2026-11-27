package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// How human groups treat one another by dungeon region: in the Deep they give other groups a wide berth, and in
// the Periculum they hunt down groups of rival factions.
final class FactionTerritory {

    private static final double DEEP_BERTH = 32.0;
    private static final int DEEP_MOVE_AWAY = 20;
    private static final int DEEP_MOVE_HEIGHT = 6;
    private static final double HUNT_RANGE = 64.0;
    private static final int CHECK_TICKS = 40;

    private FactionTerritory() {
    }

    // The nearest living human of another crew around the mob, rivals only when asked, or null.
    private static @Nullable FactionMember nearestOther(FactionMember mob, double range, boolean rivalsOnly) {
        AABB around = mob.getBoundingBox().inflate(range);
        FactionMember nearest = null;
        double best = range * range;
        for (FactionMember other : mob.level().getEntitiesOfClass(FactionMember.class, around, other -> other != mob && other.isAlive())) {
            boolean stranger = !other.crew().equals(mob.crew());
            boolean rival = other.faction() != mob.faction();
            double distance = mob.distanceToSqr(other);
            if (stranger && (rival || !rivalsOnly) && distance < best) {
                nearest = other;
                best = distance;
            }
        }
        return nearest;
    }

    // In the Deep, walks away from any other group that comes within its berth while it has no fight.
    static final class KeepApartGoal extends Goal {

        private final FactionMember mob;
        private int check;

        KeepApartGoal(FactionMember mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || this.mob.getTarget() != null || this.mob.downed()
                || !DungeonDimension.isDeep(this.mob.level(), this.mob.blockPosition())) {
                return false;
            }
            this.check = CHECK_TICKS;
            FactionMember other = nearestOther(this.mob, DEEP_BERTH, false);
            if (other == null) {
                return false;
            }
            Vec3 away = DefaultRandomPos.getPosAway(this.mob, DEEP_MOVE_AWAY, DEEP_MOVE_HEIGHT, other.position());
            return away != null && this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return this.mob.getTarget() == null && !this.mob.downed() && !this.mob.getNavigation().isDone();
        }
    }

    // In the Periculum, picks the nearest human of a rival faction as its quarry, sight or not.
    static final class HuntRivalsGoal extends TargetGoal {

        private final FactionMember hunter;
        private int check;

        HuntRivalsGoal(FactionMember mob) {
            super(mob, false);
            this.hunter = mob;
        }

        @Override
        public boolean canUse() {
            if (--this.check > 0 || this.hunter.getTarget() != null || this.hunter.downed()
                || !DungeonDimension.isPericulum(this.hunter.level(), this.hunter.blockPosition())) {
                return false;
            }
            this.check = CHECK_TICKS;
            this.targetMob = nearestOther(this.hunter, HUNT_RANGE, true);
            return this.targetMob != null;
        }

        @Override
        public void start() {
            this.hunter.setTarget(this.targetMob);
            super.start();
        }

        @Override
        protected double getFollowDistance() {
            return HUNT_RANGE;
        }
    }
}
