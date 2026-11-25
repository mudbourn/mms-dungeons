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

// Armor shaped for the kobold body, whose head and limbs are smaller than a player's.
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

    private static MeshDefinition outer() {
        CubeListBuilder body = CubeListBuilder.create()
            .texOffs(16, 16)
            .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 11.0f, 4.0f, new CubeDeformation(0.05f));
        return mesh(-0.3f, -0.25f, -0.35f, body);
    }

    private static MeshDefinition inner() {
        CubeListBuilder body = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 0.0f, -2.0f, 1.0f, 1.0f, 1.0f);
        return mesh(-0.4f, -0.35f, -0.45f, body);
    }

    private static MeshDefinition mesh(float head, float hat, float limb, CubeListBuilder body) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -7.5f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(head)),
            PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        root.getChild("head").addOrReplaceChild(
            "hat",
            CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -7.5f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(hat)),
            PartPose.offset(0.0f, 4.0f, 0.0f)
        );
        root.addOrReplaceChild("body", body, PartPose.offset(0.0f, 4.0f, 0.0f));
        CubeDeformation limbs = new CubeDeformation(limb);
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0f, -1.55f, -2.0f, 4.0f, 12.0f, 4.0f, limbs),
            PartPose.offset(1.5f, 15.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, -1.55f, -2.0f, 4.0f, 12.0f, 4.0f, limbs),
            PartPose.offset(-1.5f, 15.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(40, 16).mirror().addBox(0.0f, -1.55f, -2.0f, 4.0f, 12.0f, 4.0f, limbs),
            PartPose.offset(4.5f, 5.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-4.0f, -1.55f, -2.0f, 4.0f, 12.0f, 4.0f, limbs),
            PartPose.offset(-4.5f, 5.0f, 0.0f)
        );
        return mesh;
    }
}
