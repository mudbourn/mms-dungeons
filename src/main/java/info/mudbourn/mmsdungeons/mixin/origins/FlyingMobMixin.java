package info.mudbourn.mmsdungeons.mixin.origins;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import info.mudbourn.mmsorigins.power.FlyingMob;
import org.spongepowered.asm.mixin.Mixin;

// Makes a faction human a mob MMS Origins' flight powers can fly, using its pack food and its flying flag.
@Mixin(FactionMember.class)
public abstract class FlyingMobMixin implements FlyingMob {
}
