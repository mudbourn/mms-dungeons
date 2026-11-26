package info.mudbourn.mmsdungeons.mobs.knightquest;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// An item with one line of flavor text under its name.
public class LoreItem extends Item {

    private final String lore;

    public LoreItem(Properties properties, String lore) {
        super(properties);
        this.lore = lore;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> consumer,
        TooltipFlag flag
    ) {
        consumer.accept(Component.translatable("tooltip.item.mms_dungeons." + this.lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, display, consumer, flag);
    }
}
