package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// The ogre of the deep caves, drawn in its own colors and dropping diamonds.
public class CaveOgre extends Ogre {

    public CaveOgre(EntityType<? extends CaveOgre> type, Level level) {
        super(type, level);
    }
}
