package info.mudbourn.mmsdungeons.client.mobs.factions;

import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Hands a downed faction member to AbsolutRevive's own downed pose, so it lies on
 * its back the way a downed player does.
 *
 * <p>AbsolutRevive poses any living render state that carries its unconscious and
 * collapse-progress keys, but only fills them in for players. This fills them in
 * for faction members. The keys are read by reflection from the mod's
 * {@code RenderStateExtensions}, its own class rather than a Minecraft mapping, so
 * there is no compile dependency; without the mod, {@link #available()} is false
 * and members keep their crawl.
 */
final class AbsolutReviveRender {

    private static final String EXTENSIONS = "goetic.mods.absolutrevive.client.RenderStateExtensions";

    private static final @Nullable RenderStateDataKey<Boolean> UNCONSCIOUS = key("UNCONSCIOUS");
    private static final @Nullable RenderStateDataKey<Float> COLLAPSE_PROGRESS = key("COLLAPSE_PROGRESS");

    private AbsolutReviveRender() {
    }

    static boolean available() {
        return UNCONSCIOUS != null && COLLAPSE_PROGRESS != null;
    }

    // Marks the state downed with the fall eased in by collapse, or clears it.
    static void stamp(LivingEntityRenderState state, boolean downed, float collapse) {
        if (!available() || !(state instanceof FabricRenderState fabric)) {
            return;
        }
        fabric.setData(UNCONSCIOUS, downed);
        fabric.setData(COLLAPSE_PROGRESS, downed ? collapse : 0.0f);
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable RenderStateDataKey<T> key(String name) {
        try {
            Field field = Class.forName(EXTENSIONS).getField(name);
            return (RenderStateDataKey<T>) field.get(null);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            return null;
        }
    }
}
