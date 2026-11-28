package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.guild.BountyKind;
import info.mudbourn.mmsdungeons.guild.BountyStatus;
import info.mudbourn.mmsdungeons.guild.BountyView;
import info.mudbourn.mmseconomy.economy.Currency;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

// A scrolling list of quests and contracts with the reward, proof, poster and hold state on each row.
final class BountyList extends ObjectSelectionList<BountyList.Row> {

    private static final int ROW_HEIGHT = 54;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;
    private static final int GOLD = 0xFFFFAA00;
    private static final int RED = 0xFFFF5555;

    BountyList(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_HEIGHT);
    }

    @Override
    public int getRowWidth() {
        return Math.min(this.width - 40, 380);
    }

    // Replaces the rows and keeps the same posting selected when it is still listed.
    void setViews(List<BountyView> views) {
        UUID keep = selectedView() == null ? null : selectedView().bounty().id;
        clearEntries();
        for (BountyView view : views) {
            Row row = new Row(view);
            addEntry(row);
            if (view.bounty().id.equals(keep)) {
                setSelected(row);
            }
        }
    }

    BountyView selectedView() {
        Row row = getSelected();
        return row == null ? null : row.view;
    }

    final class Row extends ObjectSelectionList.Entry<Row> {

        private final BountyView view;

        Row(BountyView view) {
            this.view = view;
        }

        @Override
        public Component getNarration() {
            return Component.literal(view.bounty().title);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            int x = getContentX();
            int y = getContentY();
            int width = getContentWidth();
            var bounty = view.bounty();
            boolean quest = bounty.kind == BountyKind.QUEST;

            graphics.item(bounty.proof.stack(), x + 4, y + 4);
            // The count sits centered under the 16-pixel item icon.
            String count = "x" + bounty.proof.count();
            graphics.text(mc.font, count, x + 4 + (16 - mc.font.width(count)) / 2, y + 24, GRAY, true);
            graphics.text(mc.font, bounty.title, x + 28, y + 3, WHITE, true);
            String tag = Component.translatable(quest ? "gui.mms_dungeons.quest" : "gui.mms_dungeons.contract").getString()
                + " - " + bounty.type.label();
            graphics.text(mc.font, tag, x + width - 4 - mc.font.width(tag), y + 3, quest ? GREEN : GOLD, true);
            graphics.text(mc.font, mc.font.plainSubstrByWidth(bounty.description, width - 34), x + 28, y + 14, GRAY, false);
            String money = Component.translatable("gui.mms_dungeons.reward", Currency.format(bounty.reward)).getString()
                + "   " + Component.translatable("gui.mms_dungeons.poster", view.posterName()).getString()
                + "   " + Component.translatable("gui.mms_dungeons.days_left", view.daysLeft()).getString();
            graphics.text(mc.font, money, x + 28, y + 26, WHITE, false);

            int color = GRAY;
            String state = bounty.status.label();
            if (bounty.status == BountyStatus.IN_PROGRESS) {
                String key = view.holding() ? "gui.mms_dungeons.held_by_you" : "gui.mms_dungeons.held_by";
                state = Component.translatable(key, view.takerName()).getString();
                color = view.holding() ? GREEN : RED;
            } else if (bounty.status == BountyStatus.AVAILABLE) {
                color = GREEN;
            }
            graphics.text(mc.font, state, x + 28, y + 38, color, false);
        }
    }
}
