package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import info.mudbourn.mmsdungeons.dungeon.DddLoot;
import info.mudbourn.mmsdungeons.dungeon.DddMaterial;
import info.mudbourn.mmsdungeons.dungeon.DddPlacement;
import info.mudbourn.mmsdungeons.dungeon.DddRuin;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.OpenDoorways;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// Leaves the spawners out of Deadly Deadly Dungeon pieces placed in the dungeon, leaves their doorways open and sets their material and loot.
@Mixin(SinglePoolElement.class)
public abstract class DddPieceMixin {

    @Shadow
    @Final
    protected Either<Identifier, StructureTemplate> template;

    @ModifyExpressionValue(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/pools/SinglePoolElement;getSettings(Lnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;Z)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;"
        )
    )
    private StructurePlaceSettings mmsDungeons$noSpawners(StructurePlaceSettings settings, @Local(argsOnly = true) WorldGenLevel level) {
        boolean ddd = template.left().map(DddPlacement::isDddTemplate).orElse(false);
        if (ddd && DungeonDimension.isDungeon(level.getLevel())) {
            settings.addProcessor(DddPlacement.NO_SPAWNERS);
            settings.getProcessors().add(0, OpenDoorways.INSTANCE);
            DddRuin ruin = DddRuin.placing();
            if (ruin != null) {
                settings.addProcessor(DddMaterial.of(ruin));
                if (ruin == DddRuin.DEEPSLATE) {
                    settings.addProcessor(DddLoot.INSTANCE);
                }
            }
        }
        return settings;
    }
}
