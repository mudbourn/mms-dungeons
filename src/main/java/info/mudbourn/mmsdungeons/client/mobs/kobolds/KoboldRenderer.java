package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKoboldSkeleton;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

// Draws a kobold-bodied mob with its skin, glowing eyes and, when asked for, held items and armor.
public class KoboldRenderer<T extends Mob, M extends KoboldModel<KoboldRenderState>> extends MobRenderer<T, KoboldRenderState, M> {

    private final float scale;
    private final BiConsumer<? super T, KoboldRenderState> look;

    public KoboldRenderer(EntityRendererProvider.Context context, M model, float scale, BiConsumer<? super T, KoboldRenderState> look) {
        super(context, model, 0.36f);
        this.scale = scale;
        this.look = look;
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                state -> state.eyes,
                (state, age) -> state.eyes == null ? 0.0f : 1.0f,
                model,
                RenderTypes::eyes,
                true
            )
        );
    }

    public KoboldRenderer<T, M> holdItems(boolean onlyWhenAggressive) {
        this.addLayer(new ItemInHandLayer<>(this) {
            @Override
            public void submit(
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int light,
                KoboldRenderState state,
                float yRot,
                float xRot
            ) {
                if (!onlyWhenAggressive || state.isAggressive) {
                    super.submit(poseStack, collector, light, state, yRot, xRot);
                }
            }
        });
        return this;
    }

    public KoboldRenderer<T, M> dress(
        EntityRendererProvider.Context context,
        Function<ModelPart, KoboldModel<KoboldRenderState>> armorModel
    ) {
        ArmorModelSet<KoboldModel<KoboldRenderState>> armor = ArmorModelSet.bake(KoboldArmor.LAYERS, context.getModelSet(), armorModel);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
        return this;
    }

    @Override
    public KoboldRenderState createRenderState() {
        return new KoboldRenderState();
    }

    @Override
    public Identifier getTextureLocation(KoboldRenderState state) {
        return state.texture;
    }

    @Override
    public void extractRenderState(T entity, KoboldRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
        state.isAggressive = entity.isAggressive();
        state.isBlocking = entity.isBlocking();
        if (entity instanceof AbstractKobold kobold) {
            state.isCharging = kobold.isCharging();
        } else if (entity instanceof AbstractKoboldSkeleton skeleton) {
            state.isCharging = skeleton.isCharging();
        }
        this.look.accept(entity, state);
    }

    @Override
    protected boolean isShaking(KoboldRenderState state) {
        return state.isConverting;
    }

    @Override
    protected void scale(KoboldRenderState state, PoseStack poseStack) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }
}
