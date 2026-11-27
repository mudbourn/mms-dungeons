package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Adds grey hint lines under an item's name from the numbered lang keys tooltip.item.mms_dungeons.<item>.<n>.
public final class FangsNClawsTooltips {

    private static final String KEY_PREFIX = "tooltip.item." + MmsDungeons.MOD_ID + ".";

    private FangsNClawsTooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals(MmsDungeons.MOD_ID)) {
                return;
            }
            int position = 1;
            for (int line = 1; Language.getInstance().has(key(id, line)); line++) {
                Component hint = Component.translatable(key(id, line)).withStyle(ChatFormatting.GRAY);
                lines.add(Math.min(position++, lines.size()), hint);
            }
        });
    }

    private static String key(Identifier id, int line) {
        return KEY_PREFIX + id.getPath() + "." + line;
    }
}
