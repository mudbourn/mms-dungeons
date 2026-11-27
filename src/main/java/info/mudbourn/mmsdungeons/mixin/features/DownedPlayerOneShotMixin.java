package info.mudbourn.mmsdungeons.mixin.features;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a player AbsolutRevive has downed die to the next damage of any kind.
 *
 * <p>AbsolutRevive lets a downed player take damage only from creatures and
 * blocks falls, fire, hunger and the like. Downed should mean one hit from a
 * creature finishes you, as it does for faction humans. This runs before the
 * mod's damage events: it clears the downed state, as giving up does, and kills
 * the player with the damage's own source. The mod's classes are reached by
 * reflection on its own names, so without the mod this does nothing.
 */
@Mixin(ServerPlayer.class)
public abstract class DownedPlayerOneShotMixin {

    private static final String HOLDER_CLASS = "goetic.mods.absolutrevive.common.AbsolutReviveDamageModelHolder";
    private static final String MODEL_CLASS = "goetic.mods.absolutrevive.common.damagesystem.PlayerDamageModel";

    private static final @Nullable Method GET_MODEL = holderMethod();
    private static final @Nullable Method IS_UNCONSCIOUS = modelMethod("isUnconscious");
    private static final @Nullable Method CLEAR_UNCONSCIOUS = modelMethod("clearUnconsciousState");

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$downedOneShot(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (amount <= 0.0f || source.getEntity() == null || player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        Object model = downedModel(player);
        if (model == null) {
            return;
        }
        try {
            CLEAR_UNCONSCIOUS.invoke(model);
        } catch (ReflectiveOperationException e) {
            return;
        }
        player.setHealth(0.0f);
        player.die(source);
        cir.setReturnValue(true);
    }

    // The player's damage model when AbsolutRevive has the player downed, else null.
    private static @Nullable Object downedModel(ServerPlayer player) {
        if (GET_MODEL == null || IS_UNCONSCIOUS == null || CLEAR_UNCONSCIOUS == null) {
            return null;
        }
        try {
            Object model = GET_MODEL.invoke(player);
            return model != null && IS_UNCONSCIOUS.getDeclaringClass().isInstance(model) && (Boolean) IS_UNCONSCIOUS.invoke(model) ? model : null;
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    private static @Nullable Method holderMethod() {
        try {
            return Class.forName(HOLDER_CLASS).getMethod("absolutrevive$getDamageModelNullable");
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static @Nullable Method modelMethod(String name) {
        try {
            return Class.forName(MODEL_CLASS).getMethod(name);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
