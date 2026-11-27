package info.mudbourn.mmsdungeons.mobs.factions;

import com.mojang.brigadier.CommandDispatcher;
import info.mudbourn.mmsdungeons.dungeon.GateCheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

// /factionfear audit: scores every armor piece in the pack the way a faction human sizes up a player, and writes the list to logs/fear-audit.txt.
public final class FearAudit {

    private record Row(String id, EquipmentSlot slot, double declared, double fear, double netherite, boolean proven, boolean elite) {
    }

    private FearAudit() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> registerCommand(dispatcher));
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("factionfear")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("audit").executes(ctx -> audit(ctx.getSource()))));
    }

    private static int audit(CommandSourceStack source) {
        List<Row> rows = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || !equippable.slot().isArmor() || equippable.slot() == EquipmentSlot.BODY) {
                continue;
            }
            EquipmentSlot slot = equippable.slot();
            double declared = GateCheck.armorScore(stack.copy(), slot);
            boolean proven = stack.is(GateCheck.GATE_PROVEN_ARMOR);
            boolean elite = stack.is(FactionFear.ELITE_ARMOR);
            double netherite = GateCheck.provenFloor(slot);
            double fear = Math.max(declared, proven ? netherite : 0.0) + (elite ? netherite : 0.0);
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            rows.add(new Row(id.toString(), slot, declared, fear, netherite, proven, elite));
        }
        rows.sort(Comparator.comparingDouble((Row row) -> row.fear() / row.netherite()).reversed());
        List<String> lines = new ArrayList<>();
        lines.add("Fear score of every armor piece, highest share of a netherite piece in its slot first.");
        lines.add("declared = armor + toughness the item states; fear = what a faction human counts.");
        lines.add("Flags: ABOVE_NETHERITE (candidate for fear_elite_armor), NONE_DECLARED (no armor stat; cosmetic, or protection added in code), PROVEN, ELITE.");
        lines.add("");
        int above = 0;
        int none = 0;
        for (Row row : rows) {
            List<String> flags = new ArrayList<>();
            if (row.declared() > row.netherite() && !row.elite()) {
                flags.add("ABOVE_NETHERITE");
                above++;
            }
            if (row.declared() <= 0.0) {
                flags.add("NONE_DECLARED");
                none++;
            }
            if (row.proven()) {
                flags.add("PROVEN");
            }
            if (row.elite()) {
                flags.add("ELITE");
            }
            lines.add(String.format(Locale.ROOT, "%-60s %-5s declared %5.1f  fear %5.1f  (%3.0f%% of netherite)  %s",
                row.id(), row.slot().getName(), row.declared(), row.fear(), 100.0 * row.fear() / row.netherite(), String.join(" ", flags)));
        }
        Path file = source.getServer().getServerDirectory().resolve("logs").resolve("fear-audit.txt");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not write " + file + ": " + e.getMessage()));
            return 0;
        }
        int total = rows.size();
        int aboveCount = above;
        int noneCount = none;
        source.sendSuccess(() -> Component.literal(total + " armor pieces scored, " + aboveCount + " above netherite and not elite, "
            + noneCount + " with no armor stat. Written to logs/fear-audit.txt."), false);
        return 1;
    }
}
