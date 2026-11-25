package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.resources.Identifier;

// Armor shaped for the kobold body, whose head and limbs are smaller than a player's. The pieces pivot where the
// kobold's own parts do, and the helmet is sized up to sit over the snout and the back of the skull.
final class KoboldArmor {

    static final ArmorModelSet<ModelLayerLocation> LAYERS = layers();

    private static final Set<String> HEAD = Set.of("head");
    private static final Set<String> CHEST = Set.of("body", "left_arm", "right_arm");
    private static final Set<String> LEGS = Set.of("left_leg", "right_leg", "body");
    private static final Set<String> FEET = Set.of("left_leg", "right_leg");

    private KoboldArmor() {
    }

    static ArmorModelSet<LayerDefinition> createSet() {
        MeshDefinition head = outer();
        head.getRoot().retainPartsAndChildren(HEAD);
        MeshDefinition chest = outer();
        chest.getRoot().retainExactParts(CHEST);
        MeshDefinition legs = inner();
        legs.getRoot().retainExactParts(LEGS);
        MeshDefinition feet = outer();
        feet.getRoot().retainExactParts(FEET);
        return new ArmorModelSet<>(head, chest, legs, feet).map(mesh -> LayerDefinition.create(mesh, 64, 32));
    }

    private static ArmorModelSet<ModelLayerLocation> layers() {
        Identifier id = MmsDungeons.id("kobold_armor");
        return new ArmorModelSet<>(
            new ModelLayerLocation(id, "helmet"),
            new ModelLayerLocation(id, "chestplate"),
            new ModelLayerLocation(id, "leggings"),
            new ModelLayerLocation(id, "boots")
        );
    }

    // Outer layer (helmet, chestplate, boots): stands 1px off the kobold on every side, like zombie and skeleton
    // armor does. Each box keeps the standard armor texture size and is grown or shrunk per axis to fit, so the
    // chestplate covers the 6x10x4 torso and the boots end flush with the feet.
    private static MeshDefinition outer() {
        return mesh(
            0.5f,
            0.75f,
            new CubeDeformation(0.0f, -0.5f, 1.0f),
            new CubeDeformation(0.5f, -0.5f, 0.5f),
            new CubeDeformation(0.5f, -1.0f, 0.5f)
        );
    }

    // Inner layer (leggings): half a pixel off the legs, with a waist that sits inside the chestplate.
    private static MeshDefinition inner() {
        return mesh(
            0.4f,
            0.6f,
            new CubeDeformation(-0.5f, -0.75f, 0.5f),
            new CubeDeformation(0.0f, -0.5f, 0.0f),
            new CubeDeformation(0.0f, -1.0f, 0.0f)
        );
    }

    private static MeshDefinition mesh(float head, float hat, CubeDeformation body, CubeDeformation arms, CubeDeformation legs) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -7.5f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(head)),
            PartPose.offset(0.0f, 4.0f, -0.5f)
        );
        root.getChild("head").addOrReplaceChild(
            "hat",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -7.5f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(hat)),
            PartPose.ZERO
        );
        // The torso runs 10px down from the neck at y 4.
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, -1.0f, -2.0f, 8.0f, 12.0f, 4.0f, body),
            PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        // Legs hang 10px from the hip at y 14 down to the ground.
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, -1.0f, -2.0f, 4.0f, 12.0f, 4.0f, legs),
            PartPose.offset(1.5f, 14.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 12.0f, 4.0f, legs),
            PartPose.offset(-1.5f, 14.0f, 0.0f)
        );
        // Arms hang from the shoulder at y 5; the pad rises a little above the shoulder.
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-0.5f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, arms),
            PartPose.offset(3.0f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.5f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, arms),
            PartPose.offset(-3.0f, 5.0f, 0.0f)
        );
        return mesh;
    }
}
