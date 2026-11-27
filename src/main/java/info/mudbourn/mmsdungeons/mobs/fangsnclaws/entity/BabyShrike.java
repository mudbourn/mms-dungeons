package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

// A shrike chick that grows into a shrike.
public class BabyShrike extends BabyOwlbear {

    public BabyShrike(EntityType<? extends BabyShrike> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends Mob> adultType() {
        return FangsNClawsMobs.SHRIKE;
    }
}
