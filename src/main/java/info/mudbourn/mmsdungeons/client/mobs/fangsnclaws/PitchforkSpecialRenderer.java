package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

// The in-hand 3D pitchfork, drawn like a trident.
public final class PitchforkSpecialRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier TYPE = MmsDungeons.id("pitchfork");

    private final PitchforkModel model;
    private final boolean hellfire;

    private PitchforkSpecialRenderer(PitchforkModel model, boolean hellfire) {
        this.model = model;
        this.hellfire = hellfire;
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        int overlay,
        boolean foil,
        int outline
    ) {
        long ticks = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        this.model.submit(poseStack, collector, this.hellfire, foil, ticks, light, outline);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        this.model.model().root().getExtentsForGui(new PoseStack(), output);
    }

    public record Unbaked(boolean hellfire) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.BOOL.optionalFieldOf("hellfire", false).forGetter(Unbaked::hellfire)
            ).apply(instance, Unbaked::new)
        );

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new PitchforkSpecialRenderer(new PitchforkModel(context.entityModelSet()), this.hellfire);
        }
    }
}
