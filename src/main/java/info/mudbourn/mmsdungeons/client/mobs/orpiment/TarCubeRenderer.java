package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.TarCube;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;

public class TarCubeRenderer extends CubeRenderer<TarCube, SlimeRenderState> {

    public TarCubeRenderer(EntityRendererProvider.Context context) {
        super(
            context,
            OrpimentClient.TAR_CUBE,
            OrpimentClient.CUBE_OUTER,
            MmsDungeons.id("textures/entity/orpiment/tar_cube_new.png"),
            MmsDungeons.id("textures/entity/orpiment/tar_cube_eyes.png")
        );
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }
}
