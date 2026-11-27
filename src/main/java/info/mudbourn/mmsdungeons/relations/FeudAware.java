package info.mudbourn.mmsdungeons.relations;

import net.minecraft.world.entity.LivingEntity;

// A mob that adds its own feud goal and may veto targets, so the loader leaves it alone.
public interface FeudAware {

    default boolean mayStartFeud(LivingEntity target) {
        return true;
    }
}
