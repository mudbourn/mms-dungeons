package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;

public class KoboldSkullModel extends SkullModel {

    public KoboldSkullModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.5f, -7.0f, -3.5f, 7.0f, 7.0f, 7.0f)
                .texOffs(22, 0)
                .addBox(-2.5f, -3.0f, -6.5f, 5.0f, 3.0f, 3.0f)
                .texOffs(1, 3)
                .addBox(-0.5f, -3.85f, -5.5f, 1.0f, 1.0f, 2.0f),
            PartPose.ZERO
        );
        head.addOrReplaceChild(
            "left_horn",
            CubeListBuilder.create().texOffs(36, 0).addBox(-0.5f, -4.0f, -1.0f, 2.0f, 5.0f, 2.0f),
            PartPose.offsetAndRotation(1.5f, -7.0f, 2.0f, -0.6109f, 0.3054f, 0.1745f)
        );
        head.addOrReplaceChild(
            "right_horn",
            CubeListBuilder.create().texOffs(45, 0).addBox(-1.5f, -4.0f, -1.0f, 2.0f, 5.0f, 2.0f),
            PartPose.offsetAndRotation(-1.5f, -7.0f, 2.0f, -0.6109f, -0.3054f, -0.1745f)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }
}
