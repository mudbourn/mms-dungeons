package info.mudbourn.mmsdungeons.client.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class FactionRenderState extends AvatarRenderState {

    public Identifier texture;
    public byte action = FactionMember.ACTION_NONE;
    public float rollProgress;
    public boolean rollAnimated;
    public boolean crawling;
    public boolean flying;
}
