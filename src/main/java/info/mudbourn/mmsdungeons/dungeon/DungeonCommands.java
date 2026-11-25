package info.mudbourn.mmsdungeons.dungeon;

import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Mod commands a non-operator cannot run inside the dungeon.
public final class DungeonCommands {

    private static final Component REFUSED = Component.literal("The dungeon swallows the command. It does not work down here.");
    private static final Set<String> BLOCKED = Set.of(
        // Teleports out, or that pull other players in past the portal check.
        "home", "back", "spawn", "rtp", "top", "warp", "bed",
        "tpa", "tpahere", "tpaccept", "tpr", "dimension", "jump",
        // Fake players that draw aggro or hold chunks.
        "afk:summon", "afk:ready", "player", "peek",
        // Combat and survival cheats.
        "fly", "invuln", "god", "heal", "feed", "repair", "enderchest", "echest", "invsee", "near"
    );

    private DungeonCommands() {
    }

    // Whether this source is a non-operator player in the dungeon trying a blocked command, after telling them so.
    public static boolean refuse(CommandSourceStack source, String root) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !DungeonDimension.isDungeon(player.level())) {
            return false;
        }
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source) || !BLOCKED.contains(root.toLowerCase(Locale.ROOT))) {
            return false;
        }
        source.sendFailure(REFUSED);
        return true;
    }
}
