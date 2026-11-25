package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Sap;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SapRenderer extends CubeRenderer<Sap, SapRenderState> {

    public SapRenderer(EntityRendererProvider.Context context) {
        super(
            context,
            OrpimentClient.SAP,
            OrpimentClient.CUBE_OUTER,
            MmsDungeons.id("textures/entity/orpiment/sap.png"),
            MmsDungeons.id("textures/entity/orpiment/sap_eyes.png")
        );
        this.addLayer(new SapItemsLayer(this));
    }

    @Override
    public SapRenderState createRenderState() {
        return new SapRenderState();
    }

    @Override
    public void extractRenderState(Sap sap, SapRenderState state, float partialTick) {
        super.extractRenderState(sap, state, partialTick);
        state.seed = sap.getUUID().getLeastSignificantBits() ^ sap.getUUID().getMostSignificantBits();
        List<ItemStack> carried = sap.getCarried();
        state.items.clear();
        for (ItemStack stack : carried) {
            ItemStackRenderState item = new ItemStackRenderState();
            if (!stack.isEmpty()) {
                this.itemModelResolver.updateForNonLiving(item, stack, ItemDisplayContext.FIXED, sap);
            }
            state.items.add(item);
        }
    }
}
