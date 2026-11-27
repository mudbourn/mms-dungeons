package info.mudbourn.mmsdungeons.mixin.combat;

import com.zigythebird.playeranim.accessors.IAvatarAnimationState;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ArmedEntityRenderState.class)
abstract class ArmedRenderStateMixin implements IAvatarAnimationState {

    @Unique
    private boolean mmsDungeons$firstPersonPass;
    @Unique
    private AvatarAnimManager mmsDungeons$animManager;

    @Override
    public boolean playerAnimLib$isFirstPersonPass() {
        return this.mmsDungeons$firstPersonPass;
    }

    @Override
    public void playerAnimLib$setFirstPersonPass(boolean firstPersonPass) {
        this.mmsDungeons$firstPersonPass = firstPersonPass;
    }

    @Override
    public void playerAnimLib$setAnimManager(AvatarAnimManager manager) {
        this.mmsDungeons$animManager = manager;
    }

    @Override
    public AvatarAnimManager playerAnimLib$getAnimManager() {
        return this.mmsDungeons$animManager;
    }
}
