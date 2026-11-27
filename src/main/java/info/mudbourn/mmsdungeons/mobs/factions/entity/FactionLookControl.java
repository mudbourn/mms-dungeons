package info.mudbourn.mmsdungeons.mobs.factions.entity;

import net.minecraft.world.entity.ai.control.LookControl;

// Leaves the facing of a gliding member to its glide, which steers by yaw and pitch.
final class FactionLookControl extends LookControl {

    private final FactionMember member;

    FactionLookControl(FactionMember member) {
        super(member);
        this.member = member;
    }

    @Override
    public void tick() {
        if (this.member.flight().gliding()) {
            return;
        }
        super.tick();
    }
}
