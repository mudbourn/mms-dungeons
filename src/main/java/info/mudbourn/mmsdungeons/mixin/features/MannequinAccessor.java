package info.mudbourn.mmsdungeons.mixin.features;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

// Reads the synced text under a mannequin's name.
@Mixin(Mannequin.class)
public interface MannequinAccessor {

    @Invoker("getDescription")
    Component mmsDungeons$getDescription();

    @Accessor("DATA_DESCRIPTION")
    static EntityDataAccessor<Optional<Component>> mmsDungeons$descriptionData() {
        throw new AssertionError();
    }
}
