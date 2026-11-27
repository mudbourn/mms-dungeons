package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

// A hyena pup that grows into a hyena.
public class BabyHyena extends BabyWildWolf {

    public BabyHyena(EntityType<? extends BabyHyena> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends Mob> adultType() {
        return FangsNClawsMobs.HYENA;
    }
}
