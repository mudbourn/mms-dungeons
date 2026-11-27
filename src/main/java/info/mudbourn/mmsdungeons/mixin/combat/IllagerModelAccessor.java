package info.mudbourn.mmsdungeons.mixin.combat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.illager.IllagerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IllagerModel.class)
public interface IllagerModelAccessor {

    @Accessor("head")
    ModelPart mmsDungeons$head();

    @Accessor("arms")
    ModelPart mmsDungeons$crossedArms();

    @Accessor("rightArm")
    ModelPart mmsDungeons$rightArm();

    @Accessor("leftArm")
    ModelPart mmsDungeons$leftArm();

    @Accessor("rightLeg")
    ModelPart mmsDungeons$rightLeg();

    @Accessor("leftLeg")
    ModelPart mmsDungeons$leftLeg();
}
