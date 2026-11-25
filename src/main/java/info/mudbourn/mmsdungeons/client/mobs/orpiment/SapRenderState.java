package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class SapRenderState extends SlimeRenderState {

    public final List<ItemStackRenderState> items = new ArrayList<>();
    public long seed;
}
