package info.mudbourn.mmsdungeons.mixin.dungeon;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import info.mudbourn.mmsdungeons.dungeon.DungeonCommands;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Stops blocked mod commands before they run for non-operators in the dungeon.
@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$refuseInDungeon(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        List<ParsedCommandNode<CommandSourceStack>> nodes = parse.getContext().getNodes();
        if (!nodes.isEmpty() && DungeonCommands.refuse(parse.getContext().getSource(), nodes.getFirst().getNode().getName())) {
            ci.cancel();
        }
    }
}
