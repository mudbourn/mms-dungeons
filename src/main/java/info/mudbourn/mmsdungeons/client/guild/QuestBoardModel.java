package info.mudbourn.mmsdungeons.client.guild;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

// The quest board mesh: a roofed notice board.
public class QuestBoardModel extends Model<EntityRenderState> {

    public QuestBoardModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "Toit",
            CubeListBuilder.create()
                .texOffs(0, 11)
                .addBox(-22.0F, -2.0F, -1.0F, 42.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-20.0F, -1.0F, -4.0F, 38.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(0, 17)
                .addBox(-20.0F, 0.0F, 5.0F, 38.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 22)
                .addBox(-20.0F, 1.0F, 8.0F, 38.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 32)
                .addBox(-20.0F, 0.0F, -7.0F, 38.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 27)
                .addBox(-20.0F, 1.0F, -10.0F, 38.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offset(1.0F, -6.0F, -1.0F)
        );
        PartDefinition poutresSupport = root.addOrReplaceChild("Poutres support", CubeListBuilder.create(), PartPose.offset(-15.0F, 3.0F, -3.0F));
        poutresSupport.addOrReplaceChild(
            "Poutrenorddroite_r1",
            CubeListBuilder.create()
                .texOffs(52, 59)
                .addBox(-1.0F, -2.0F, -9.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
                .texOffs(28, 59)
                .addBox(30.0F, -2.0F, -9.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        poutresSupport.addOrReplaceChild(
            "Poutresudgauche_r1",
            CubeListBuilder.create()
                .texOffs(64, 37)
                .addBox(-1.0F, -10.0F, 7.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F))
                .texOffs(28, 71)
                .addBox(-32.0F, -10.0F, 7.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(31.0F, 12.0F, 5.0F, 0.7854F, 0.0F, 0.0F)
        );
        poutresSupport.addOrReplaceChild(
            "Poutredessousgauche_r1",
            CubeListBuilder.create().texOffs(70, 49).addBox(-19.0F, -2.0F, -1.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(40.0F, -1.0F, 3.0F, 0.0F, 0.0F, -0.7854F)
        );
        poutresSupport.addOrReplaceChild(
            "Poutredessousdroite_r1",
            CubeListBuilder.create().texOffs(64, 49).addBox(-19.0F, -2.0F, -1.0F, 1.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(16.0F, 25.0F, 3.0F, 0.0F, 0.0F, 0.7854F)
        );
        root.addOrReplaceChild(
            "bone",
            CubeListBuilder.create()
                .texOffs(0, 59)
                .addBox(-25.0F, -30.0F, 6.0F, 3.0F, 30.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(14, 59)
                .addBox(6.0F, -30.0F, 6.0F, 3.0F, 30.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 37)
                .addBox(-23.0F, -30.0F, 7.0F, 30.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(8.0F, 24.0F, -8.0F)
        );
        root.addOrReplaceChild(
            "bb_main",
            CubeListBuilder.create()
                .texOffs(91, 50)
                .addBox(6.0F, -25.0F, -2.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(91, 44)
                .addBox(-10.0F, -23.0F, -2.0F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(91, 36)
                .addBox(-2.0F, -21.0F, -2.0F, 5.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 128, 128);
    }
}
