package info.mudbourn.mmsdungeons.corpse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Consumed on use: brings one of the user's bodies, items and all, to lie in front of them.
public final class RecoveryCrossItem extends Item {

    private static final String COMMAND = "recoverbody";
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final int UPPER_DEPTH = -43;
    private static final int MIDDLE_DEPTH = -86;
    private static final int GRID = 100;

    public RecoveryCrossItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide() || !(user instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        List<CorpseData.Corpse> corpses = Corpses.corpsesOf(player.getUUID());
        if (corpses.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have nothing to recover."), true);
            return InteractionResult.FAIL;
        }
        if (corpses.size() == 1) {
            summon(player, corpses.get(0), player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.literal("The cross hums. Whose rest will you call back?"));
        for (CorpseData.Corpse corpse : corpses) {
            Style click = Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/" + COMMAND + " " + corpse.id));
            player.sendSystemMessage(Component.literal("  - " + describe(corpse)).withStyle(click));
        }
        return InteractionResult.SUCCESS;
    }

    static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND)
            .then(Commands.argument("body", StringArgumentType.word()).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ItemStack cross = heldCross(player);
                UUID id = parse(StringArgumentType.getString(ctx, "body"));
                CorpseData.Corpse corpse = id == null ? null : Corpses.corpse(id);
                if (cross == null) {
                    player.sendSystemMessage(Component.literal("Hold the recovery cross to call a body."), true);
                    return 0;
                }
                if (corpse == null || !player.getUUID().equals(corpse.owner)) {
                    player.sendSystemMessage(Component.literal("That body is no longer there to call."), true);
                    return 0;
                }
                summon(player, corpse, cross);
                return 1;
            })));
    }

    private static void summon(ServerPlayer player, CorpseData.Corpse corpse, ItemStack cross) {
        Corpses.summon(player, corpse);
        cross.consume(1, player);
        player.sendSystemMessage(Component.literal("Your body lies before you. Right-click it to take back your belongings."), true);
    }

    private static ItemStack heldCross(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Corpses.RECOVERY_CROSS)) {
                return stack;
            }
        }
        return null;
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // A plain line giving a body's rough place and how long ago they fell.
    private static String describe(CorpseData.Corpse corpse) {
        return place(corpse.pos) + ", fell " + ago(corpse.time);
    }

    private static String place(BlockPos pos) {
        if (pos == null) {
            return "Somewhere in the Periculum Region";
        }
        String depth = pos.getY() > UPPER_DEPTH ? "The upper Periculum" : pos.getY() > MIDDLE_DEPTH ? "The middle Periculum" : "The lowest depths of the Periculum";
        return depth + ", near x " + rounded(pos.getX()) + " z " + rounded(pos.getZ());
    }

    private static long rounded(int value) {
        return Math.round(value / (double) GRID) * GRID;
    }

    private static String ago(long time) {
        if (time <= 0L) {
            return "long ago";
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - time);
        if (elapsed < 2 * MINUTE) {
            return "moments ago";
        }
        if (elapsed < HOUR) {
            return elapsed / MINUTE + " minutes ago";
        }
        if (elapsed < 2 * HOUR) {
            return "about an hour ago";
        }
        if (elapsed < 2 * DAY) {
            return elapsed / HOUR + " hours ago";
        }
        return elapsed / DAY + " days ago";
    }
}
