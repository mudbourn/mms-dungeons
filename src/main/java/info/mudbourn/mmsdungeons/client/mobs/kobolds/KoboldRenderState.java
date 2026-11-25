package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class KoboldRenderState extends HumanoidRenderState {

    public Identifier texture;
    public @Nullable Identifier eyes;
    public boolean isAggressive;
    public boolean isBlocking;
    public boolean isCharging;
    public boolean isConverting;

    public boolean isLeftHanded() {
        return this.mainArm == HumanoidArm.LEFT;
    }

    public ItemStack getOffHandItemStack() {
        return this.mainArm == HumanoidArm.RIGHT ? this.leftHandItemStack : this.rightHandItemStack;
    }
}
