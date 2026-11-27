package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;

// Turns an ogre slowly while it walks and quickly while it runs.
public class OgreMoveControl extends MoveControl {

    private static final float WALK_TURN = 0.12f;
    private static final float RUN_TURN = 0.35f;

    private final Ogre ogre;

    public OgreMoveControl(Ogre ogre) {
        super(ogre);
        this.ogre = ogre;
    }

    @Override
    protected float rotlerp(float angle, float target, float maxStep) {
        return angle + Mth.wrapDegrees(target - angle) * (this.ogre.isRunning() ? RUN_TURN : WALK_TURN);
    }
}
