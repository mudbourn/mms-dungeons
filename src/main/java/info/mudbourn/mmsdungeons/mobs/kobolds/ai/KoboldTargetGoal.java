package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.relations.FeudTargetGoal;
import net.minecraft.world.entity.player.Player;

// Goes after what the relations file makes kobolds hostile to, and after players of the Phantom origin, who count as phantoms.
public class KoboldTargetGoal extends FeudTargetGoal {

    private static final int INTERVAL = 12;

    public KoboldTargetGoal(AbstractKobold kobold) {
        super(kobold, INTERVAL, target -> target instanceof Player player && !player.isCreative() && !player.isSpectator() && PhantomOrigin.is(player));
    }

    @Override
    protected double getFollowDistance() {
        return this.mob.getVehicle() != null ? super.getFollowDistance() * 3.0 : super.getFollowDistance();
    }
}
