package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

// In a fight the body turns with the head, as a player's does, so a member always faces what it is looking at.
final class FactionBodyControl extends BodyRotationControl {

    private final Mob mob;

    FactionBodyControl(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void clientTick() {
        if (this.mob.isAggressive()) {
            this.mob.setYBodyRot(this.mob.getYHeadRot());
            return;
        }
        super.clientTick();
    }
}
