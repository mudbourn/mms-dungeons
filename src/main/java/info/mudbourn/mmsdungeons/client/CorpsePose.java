package info.mudbourn.mmsdungeons.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

// Carries a corpse marker's synced facing onto its render state, NaN for anything that is not a corpse.
public final class CorpsePose {

    public static final RenderStateDataKey<Float> FACING = RenderStateDataKey.create(() -> "mms_dungeons corpse facing");

    private CorpsePose() {
    }

    public static boolean isCorpse(LivingEntityRenderState state) {
        return !Float.isNaN(facing(state));
    }

    public static float facing(LivingEntityRenderState state) {
        return state.getDataOrDefault(FACING, Float.NaN);
    }
}
