package info.mudbourn.mmsdungeons.client.guild;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Lets the player pick any item as the proof of a quest or contract, with a name filter.
final class ItemSelectionScreen extends Screen {

    private final Screen parent;
    private final Consumer<ItemStack> picked;
    private ItemList list;
    private EditBox filter;

    ItemSelectionScreen(Screen parent, Consumer<ItemStack> picked) {
        super(Component.translatable("gui.mms_dungeons.choose_item"));
        this.parent = parent;
        this.picked = picked;
    }

    @Override
    protected void init() {
        filter = new EditBox(font, width / 2 - 100, 28, 200, 18, Component.translatable("gui.mms_dungeons.filter"));
        filter.setResponder(text -> list.fill(text));
        addRenderableWidget(filter);
        list = new ItemList(minecraft, width, height - 84, 50);
        addRenderableWidget(list);
        list.fill("");
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.cancel"), button -> onClose())
            .bounds(width / 2 - 100, height - 28, 200, 20)
            .build());
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    private final class ItemList extends ObjectSelectionList<ItemList.Entry> {

        ItemList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 24);
        }

        @Override
        public int getRowWidth() {
            return width - 50;
        }

        void fill(String text) {
            clearEntries();
            String needle = text.toLowerCase();
            BuiltInRegistries.ITEM.forEach(item -> {
                ItemStack stack = new ItemStack(item);
                if (item.isEnabled(minecraft.level.enabledFeatures())
                    && !stack.isEmpty()
                    && stack.getHoverName().getString().toLowerCase().contains(needle)) {
                    addEntry(new Entry(stack));
                }
            });
            setScrollAmount(0.0);
        }

        final class Entry extends ObjectSelectionList.Entry<Entry> {

            private final ItemStack stack;

            Entry(ItemStack stack) {
                this.stack = stack;
            }

            @Override
            public Component getNarration() {
                return stack.getHoverName();
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                if (event.button() != 0) {
                    return false;
                }
                picked.accept(stack);
                onClose();
                return true;
            }

            @Override
            public void extractContent(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
            ) {
                graphics.item(stack, getContentX() + 4, getContentY() + 2);
                graphics.text(
                    font,
                    stack.getHoverName(),
                    getContentX() + 28,
                    getContentY() + 6,
                    0xFFFFFFFF,
                    true
                );
            }
        }
    }
}
