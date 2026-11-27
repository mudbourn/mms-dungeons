package info.mudbourn.mmsdungeons.mixin.dungeon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerLevelStructureCheckAccessor {

    @Accessor("structureCheck")
    StructureCheck mmsDungeons$structureCheck();
}
