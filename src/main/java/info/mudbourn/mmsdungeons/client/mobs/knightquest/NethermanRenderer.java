package info.mudbourn.mmsdungeons.client.mobs.knightquest;

import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import info.mudbourn.mmsrendercommon.client.anim.GeoAnimationSpec;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityDefinition;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderState;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.geo.GeoTexture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

// Draws the Netherman as the fire, ice or magic form of its phase, with the switch frames between them.
public class NethermanRenderer extends GeoEntityRenderer<Netherman> {

    private enum Form {
        FIRE("netherman_fire"),
        ICE("netherman_ice"),
        MAGIC("netherman_magic");

        private final String name;

        Form(String name) {
            this.name = name;
        }

        private Identifier geo() {
            return source("geo/" + this.name + ".geo.json");
        }

        private Identifier animations() {
            return source("animations/" + this.name + ".animation.json");
        }

        private Identifier texture() {
            return source("textures/entity/" + this.name + ".png");
        }
    }

    private static final int[] SWITCH_2_STEPS = {50, 60, 70, 80, 90, 100, 110, 120};
    private static final int[] SWITCH_3_STEPS = {50, 65, 70, 85, 100, 115, 130, 145, 160};

    private final Map<Identifier, GeoEntityDefinition> variants = new HashMap<>();
    private GeoEntityDefinition cachedBase;
    private Netherman current;

    public NethermanRenderer(EntityRendererProvider.Context context, Identifier definition) {
        super(context, definition);
    }

    @Override
    public void extractRenderState(Netherman entity, GeoEntityRenderState state, float partialTick) {
        this.current = entity;
        super.extractRenderState(entity, state, partialTick);
        this.current = null;
    }

    @Override
    public GeoEntityDefinition definition() {
        GeoEntityDefinition base = super.definition();
        if (base == null || this.current == null) {
            return base;
        }
        if (base != this.cachedBase) {
            this.variants.clear();
            this.cachedBase = base;
        }
        Form form = form(this.current);
        Identifier texture = texture(this.current, form);
        return this.variants.computeIfAbsent(texture, key -> variant(base, form, key));
    }

    private static GeoEntityDefinition variant(GeoEntityDefinition base, Form form, Identifier texture) {
        GeoAnimationSpec spec = base.animation();
        return new GeoEntityDefinition(
            form.geo(),
            new GeoTexture(texture, 1, 1),
            base.emissiveTexture(),
            base.translucent(),
            base.scale(),
            base.shadowRadius(),
            base.headBone(),
            base.walk(),
            base.attack(),
            base.death(),
            base.heldItems(),
            base.attachments(),
            new GeoAnimationSpec(Optional.of(form.animations()), spec.idle(), spec.states(), spec.driver(), spec.transitionTicks())
        );
    }

    private static Form form(Netherman netherman) {
        if (netherman.isDeadOrDying()) {
            return Form.MAGIC;
        }
        return switch (netherman.getPhase()) {
            case 1 -> Form.FIRE;
            case 2 -> netherman.getSwitch2() >= Netherman.SWITCH_2_TICKS ? Form.ICE : Form.FIRE;
            default -> netherman.getSwitch3() >= Netherman.SWITCH_3_TICKS ? Form.MAGIC : Form.ICE;
        };
    }

    private static Identifier texture(Netherman netherman, Form form) {
        if (netherman.isDeadOrDying()) {
            return Form.MAGIC.texture();
        }
        if (netherman.getPhase() == 2 && netherman.getSwitch2() < Netherman.SWITCH_2_TICKS) {
            return switchFrame(Form.FIRE, Form.ICE, "phase2", SWITCH_2_STEPS, netherman.getSwitch2());
        }
        if (netherman.getPhase() == 3 && netherman.getSwitch3() < Netherman.SWITCH_3_TICKS) {
            return switchFrame(Form.ICE, Form.MAGIC, "phase3", SWITCH_3_STEPS, netherman.getSwitch3());
        }
        return form.texture();
    }

    private static Identifier switchFrame(Form from, Form to, String phase, int[] steps, int counter) {
        if (counter < steps[0]) {
            return from.texture();
        }
        for (int frame = 1; frame < steps.length; frame++) {
            if (counter < steps[frame]) {
                return source("textures/entity/animated/netherman_switch_" + phase + "_" + (frame - 1) + ".png");
            }
        }
        return to.texture();
    }

    private static Identifier source(String path) {
        return Identifier.fromNamespaceAndPath("knightquest", path);
    }
}
