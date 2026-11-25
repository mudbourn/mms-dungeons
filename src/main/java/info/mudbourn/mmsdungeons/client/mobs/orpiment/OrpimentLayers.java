package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class OrpimentLayers {

    private OrpimentLayers() {
    }

    public static LayerDefinition wraith() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition whole = root.addOrReplaceChild("whole", CubeListBuilder.create(), PartPose.offset(0.0f, 27.0f, 0.0f));
        whole.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create()
                .texOffs(22, 40)
                .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f)
                .texOffs(48, 46)
                .addBox(-2.0f, -3.0f, -2.0f, 4.0f, 14.0f, 4.0f),
            PartPose.offsetAndRotation(-5.0f, -10.0f, 0.0f, -1.4835f, 0.0f, 0.0f)
        );
        whole.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create()
                .texOffs(14, 40)
                .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 12.0f, 2.0f)
                .texOffs(32, 32)
                .addBox(-2.0f, -3.0f, -2.0f, 4.0f, 14.0f, 4.0f),
            PartPose.offsetAndRotation(5.0f, -10.0f, 0.0f, -1.4835f, 0.0f, 0.0f)
        );
        whole.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(32, 16)
                .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f)
                .texOffs(0, 0)
                .addBox(-4.0f, 0.0f, -3.0f, 8.0f, 18.0f, 6.0f),
            PartPose.offset(0.0f, -12.0f, 0.0f)
        );
        PartDefinition head = whole.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 24).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
            PartPose.offset(0.0f, -12.0f, 0.0f)
        );
        head.addOrReplaceChild(
            "headwear",
            CubeListBuilder.create().texOffs(28, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.5f)),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition wisp() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bb_main = root.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0f, 24.0f, 0.0f));
        bb_main.addOrReplaceChild(
            "outer_r1",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
                .texOffs(0, 16)
                .addBox(-3.0f, -7.0f, -3.0f, 6.0f, 6.0f, 6.0f),
            PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, 0.0f, 3.1416f, 0.0f)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition wispFlipped() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "bb_main",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-3.0f, -10.0f, -3.0f, 6.0f, 6.0f, 6.0f)
                .texOffs(0, 0)
                .addBox(-4.0f, -11.0f, -4.0f, 8.0f, 8.0f, 8.0f),
            PartPose.offset(0.0f, 24.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition craw() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "right_wing",
            CubeListBuilder.create()
                .texOffs(56, 37)
                .addBox(-1.0f, -1.0f, -4.0f, 1.0f, 5.0f, 7.0f)
                .texOffs(16, 56)
                .addBox(-1.0f, 4.0f, -4.0f, 1.0f, 3.0f, 8.0f),
            PartPose.offset(6.0f, 8.0f, -1.0f)
        );
        root.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(52, 56)
                .addBox(-1.0f, -1.0f, -4.0f, 1.0f, 5.0f, 7.0f)
                .texOffs(34, 56)
                .addBox(-1.0f, 4.0f, -4.0f, 1.0f, 3.0f, 8.0f),
            PartPose.offset(-5.0f, 8.0f, -1.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(36, 67)
                .addBox(-2.0f, -2.0f, -2.0f, 3.0f, 11.0f, 3.0f)
                .texOffs(40, 49)
                .addBox(-3.0f, 9.0f, -7.0f, 5.0f, 0.0f, 7.0f)
                .texOffs(16, 67)
                .addBox(-3.0f, -2.0f, -3.0f, 5.0f, 3.0f, 5.0f),
            PartPose.offset(3.0f, 15.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create()
                .texOffs(60, 22)
                .addBox(-2.0f, -2.0f, -3.0f, 5.0f, 3.0f, 5.0f)
                .texOffs(16, 49)
                .addBox(-2.0f, 9.0f, -7.0f, 5.0f, 0.0f, 7.0f)
                .texOffs(48, 68)
                .addBox(-1.0f, -2.0f, -2.0f, 3.0f, 11.0f, 3.0f),
            PartPose.offset(-3.0f, 15.0f, 0.0f)
        );
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -8.0f, -4.0f, 10.0f, 8.0f, 14.0f),
            PartPose.offset(0.0f, 13.0f, -3.0f)
        );
        body.addOrReplaceChild(
            "tail_r1",
            CubeListBuilder.create().texOffs(60, 30).addBox(-3.0f, -2.0f, 0.7321f, 6.0f, 7.0f, 0.0f),
            PartPose.offsetAndRotation(0.0f, -4.0f, 10.0f, 0.5236f, 0.0f, 0.0f)
        );
        body.addOrReplaceChild(
            "neck",
            CubeListBuilder.create()
                .texOffs(48, 14)
                .addBox(-4.0f, 4.0f, -4.0f, 8.0f, 3.0f, 5.0f)
                .texOffs(32, 22)
                .addBox(-4.0f, -5.0f, -5.0f, 8.0f, 9.0f, 6.0f),
            PartPose.offset(0.0f, -5.0f, -5.0f)
        );
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(36, 84)
                .addBox(-4.0f, -7.0f, -2.0f, 8.0f, 7.0f, 6.0f)
                .texOffs(48, 0)
                .addBox(-4.0f, -9.0f, -4.0f, 8.0f, 11.0f, 3.0f),
            PartPose.offset(0.0f, 3.0f, -11.0f)
        );
        head.addOrReplaceChild(
            "bottom_beak",
            CubeListBuilder.create().texOffs(0, 76).addBox(-3.0f, -3.0f, -12.0f, 6.0f, 3.0f, 10.0f),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        head.addOrReplaceChild(
            "top_beak",
            CubeListBuilder.create().texOffs(0, 22).addBox(-3.0f, -10.0f, -9.0f, 6.0f, 5.0f, 10.0f),
            PartPose.offset(0.0f, 2.0f, -3.0f)
        );
        head.addOrReplaceChild(
            "head_tuft",
            CubeListBuilder.create().texOffs(0, 49).addBox(0.0f, -4.0f, -5.0f, 0.0f, 12.0f, 8.0f),
            PartPose.offset(0.0f, -6.0f, 4.0f)
        );
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition crawSaddle() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "right_wing",
            CubeListBuilder.create()
                .texOffs(80, 0)
                .addBox(-1.0f, -1.0f, -4.0f, 1.0f, 5.0f, 7.0f)
                .texOffs(0, 79)
                .addBox(-1.0f, 4.0f, -4.0f, 1.0f, 3.0f, 8.0f),
            PartPose.offset(6.0f, 8.0f, -1.0f)
        );
        root.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(80, 12)
                .addBox(-1.0f, -1.0f, -4.0f, 1.0f, 5.0f, 7.0f)
                .texOffs(18, 79)
                .addBox(-1.0f, 4.0f, -4.0f, 1.0f, 3.0f, 8.0f),
            PartPose.offset(-5.0f, 8.0f, -1.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(56, 83)
                .addBox(-2.0f, -2.0f, -2.0f, 3.0f, 11.0f, 3.0f)
                .texOffs(46, 76)
                .addBox(-3.0f, 9.0f, -7.0f, 5.0f, 0.0f, 7.0f)
                .texOffs(80, 24)
                .addBox(-3.0f, -2.0f, -3.0f, 5.0f, 3.0f, 5.0f),
            PartPose.offset(3.0f, 15.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create()
                .texOffs(36, 83)
                .addBox(-2.0f, -2.0f, -3.0f, 5.0f, 3.0f, 5.0f)
                .texOffs(70, 76)
                .addBox(-2.0f, 9.0f, -7.0f, 5.0f, 0.0f, 7.0f)
                .texOffs(68, 83)
                .addBox(-1.0f, -2.0f, -2.0f, 3.0f, 11.0f, 3.0f),
            PartPose.offset(-3.0f, 15.0f, 0.0f)
        );
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-5.0f, -8.0f, -4.0f, 10.0f, 8.0f, 14.0f)
                .texOffs(0, 22)
                .addBox(-5.0f, -8.0f, -2.0f, 10.0f, 5.0f, 9.0f, new CubeDeformation(0.5f)),
            PartPose.offset(0.0f, 13.0f, -3.0f)
        );
        body.addOrReplaceChild(
            "tail_r1",
            CubeListBuilder.create().texOffs(80, 83).addBox(-3.0f, -2.0f, 0.7321f, 6.0f, 7.0f, 0.0f),
            PartPose.offsetAndRotation(0.0f, -4.0f, 10.0f, 0.5236f, 0.0f, 0.0f)
        );
        body.addOrReplaceChild(
            "neck",
            CubeListBuilder.create()
                .texOffs(60, 68)
                .addBox(-4.0f, 4.0f, -4.0f, 8.0f, 3.0f, 5.0f)
                .texOffs(32, 55)
                .addBox(-4.0f, -5.0f, -5.0f, 8.0f, 9.0f, 6.0f),
            PartPose.offset(0.0f, -5.0f, -5.0f)
        );
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(60, 55)
                .addBox(-4.0f, -7.0f, -2.0f, 8.0f, 7.0f, 6.0f)
                .texOffs(0, 65)
                .addBox(-4.0f, -9.0f, -4.0f, 8.0f, 11.0f, 3.0f)
                .texOffs(68, 34)
                .addBox(0.0f, -10.0f, -1.0f, 0.0f, 12.0f, 8.0f)
                .texOffs(0, 36)
                .addBox(-5.0f, -8.0f, -12.0f, 10.0f, 8.0f, 8.0f, new CubeDeformation(0.5f))
                .texOffs(48, 0)
                .addBox(-4.9f, -3.5f, -5.5f, 0.0f, 3.0f, 16.0f)
                .texOffs(36, 36)
                .addBox(5.1f, -3.5f, -5.5f, 0.0f, 3.0f, 16.0f)
                .texOffs(22, 70)
                .addBox(-4.0f, -7.0f, 0.0f, 8.0f, 5.0f, 4.0f, new CubeDeformation(0.5f)),
            PartPose.offset(0.0f, 3.0f, -11.0f)
        );
        head.addOrReplaceChild(
            "bottom_beak",
            CubeListBuilder.create().texOffs(0, 52).addBox(-3.0f, -3.0f, -12.0f, 6.0f, 3.0f, 10.0f),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        head.addOrReplaceChild(
            "top_beak",
            CubeListBuilder.create().texOffs(48, 19).addBox(-3.0f, -10.0f, -9.0f, 6.0f, 5.0f, 10.0f),
            PartPose.offset(0.0f, 2.0f, -3.0f)
        );
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition tarCubeInner() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "core",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 4.0f, 4.0f),
            PartPose.offset(0.0f, 22.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition cubeOuter() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "outer",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -6.0f, -4.0f, 8.0f, 8.0f, 8.0f),
            PartPose.offset(0.0f, 22.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition sapInner() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "core",
            CubeListBuilder.create().texOffs(0, 16).addBox(-1.0f, -3.0f, -1.0f, 2.0f, 2.0f, 2.0f),
            PartPose.offset(0.0f, 22.0f, 0.0f)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }
}
