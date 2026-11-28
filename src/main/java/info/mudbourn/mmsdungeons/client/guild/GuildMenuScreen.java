package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.net.GuildMenuActionPayload;
import info.mudbourn.mmsdungeons.net.GuildMenuPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

// The guild menu in Adventurer Tools: browse, found and join guilds, and run your own, without typing /guild.
public final class GuildMenuScreen extends Screen {

    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int ROW_HEIGHT = 22;
    private static final int SMALL_BUTTON = 54;
    private static final int PANEL_WIDTH = 300;
    private static final int TREASURY_FIELD = 70;

    private final @Nullable Screen parent;
    private @Nullable GuildMenuPayload state;
    private boolean ownTab;
    private @Nullable RowList list;

    public GuildMenuScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.mms_dungeons.guild_menu"));
        this.parent = parent;
    }

    // A fresh menu state from the server: rebuilds the open guild menu, if any.
    static void receive(Minecraft client, GuildMenuPayload payload) {
        if (client.gui.screen() instanceof GuildMenuScreen screen) {
            screen.ownTab |= screen.state == null && payload.mine() >= 0;
            screen.state = payload;
            screen.rebuildWidgets();
        }
    }

    private static void send(String command) {
        ClientPlayNetworking.send(new GuildMenuActionPayload(command));
    }

    private static String quoted(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    protected void init() {
        if (state == null) {
            send("");
            addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.close"), button -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20)
                .build());
            return;
        }
        int left = width / 2 - PANEL_WIDTH / 2;
        boolean inGuild = state.mine() >= 0;
        boolean member = state.member();
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.all_guilds"), button -> tab(false))
            .bounds(left, 26, PANEL_WIDTH / 2 - 2, 20)
            .build()).active = ownTab;
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.my_guild"), button -> tab(true))
            .bounds(left + PANEL_WIDTH / 2 + 2, 26, PANEL_WIDTH / 2 - 2, 20)
            .build()).active = !ownTab && inGuild;
        if (ownTab && inGuild) {
            initOwn(left);
        } else {
            ownTab = false;
            initAll(left, member);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.close"), button -> onClose())
            .bounds(width / 2 - 50, height - 28, 100, 20)
            .build());
    }

    private void tab(boolean own) {
        ownTab = own;
        rebuildWidgets();
    }

    // Every guild with its join button, and a name field to found a new one.
    private void initAll(int left, boolean inGuild) {
        int bottom = height - 34;
        if (!inGuild) {
            EditBox name = new EditBox(font, left, bottom - 20, PANEL_WIDTH - SMALL_BUTTON - 4, 20,
                Component.translatable("gui.mms_dungeons.guild_name"));
            name.setMaxLength(32);
            name.setHint(Component.translatable("gui.mms_dungeons.guild_name"));
            addRenderableWidget(name);
            addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.found"),
                    button -> send("found " + quoted(name.getValue().strip())))
                .bounds(left + PANEL_WIDTH - SMALL_BUTTON, bottom - 20, SMALL_BUTTON, 20)
                .build());
            bottom -= 26;
        }
        list = addRenderableWidget(new RowList(52, bottom - 52));
        for (GuildMenuPayload.Row row : state.guilds()) {
            String detail = (row.isPublic() ? "Public, " : "") + "led by " + row.master() + ", "
                + row.members() + " members, fee " + row.fee();
            List<Button> buttons = inGuild ? List.of() : List.of(small("gui.mms_dungeons.join",
                () -> send("join " + quoted(row.commandName()) + " confirm")));
            list.add(row.name(), detail, row.charter(), buttons);
        }
    }

    // The player's guild: its numbers, and for its manager the settings, members and exiles.
    private void initOwn(int left) {
        GuildMenuPayload.Row guild = state.guilds().get(state.mine());
        list = addRenderableWidget(new RowList(52, height - 86));
        list.add(guild.name(), "Fee " + guild.fee() + ", commission " + guild.commission() + "%", guild.charter(), List.of());
        this.treasury();
        if (state.manager()) {
            setting("gui.mms_dungeons.set_charter", guild.charter(), 200, text -> "charter " + text);
            setting("gui.mms_dungeons.set_fee", guild.fee(), 12, text -> "fee " + text);
            setting("gui.mms_dungeons.set_commission", Integer.toString(guild.commission()), 2, text -> "commission " + text);
            list.add(Component.translatable("gui.mms_dungeons.depot_hint").getString(), "", "", List.of(
                small("gui.mms_dungeons.set_depot", () -> send("depot")),
                small("gui.mms_dungeons.bind_board", () -> send("bind"))));
        }
        for (String member : state.members()) {
            List<Button> buttons = !state.manager() || member.equals(guild.master()) ? List.of() : List.of(
                small("gui.mms_dungeons.kick", () -> send("kick " + member)),
                small("gui.mms_dungeons.banish", () -> send("banish " + member)),
                small("gui.mms_dungeons.transfer", () -> send("transfer " + member)));
            list.add(member, member.equals(guild.master()) ? "Guildmaster" : "Member", "", buttons);
        }
        for (String exile : state.banished()) {
            List<Button> buttons = state.manager() ? List.of(small("gui.mms_dungeons.pardon", () -> send("pardon " + exile)))
                : List.of();
            list.add(exile, "Banished", "", buttons);
        }
        if (state.member() && !guild.master().equals(minecraft.player.getGameProfile().name())) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.leave_guild"), button -> send("leave"))
                .bounds(left, height - 56, PANEL_WIDTH, 20)
                .build());
        }
    }

    // The treasury: its balance, an amount field, and Deposit beside it, with Withdraw for whoever runs the guild.
    private void treasury() {
        EditBox amount = new EditBox(font, 0, 0, TREASURY_FIELD, 18, Component.translatable("gui.mms_dungeons.amount"));
        amount.setMaxLength(12);
        amount.setHint(Component.translatable("gui.mms_dungeons.amount"));
        List<Button> buttons = new ArrayList<>();
        buttons.add(small("gui.mms_dungeons.deposit", () -> send("deposit " + amount.getValue().strip())));
        if (state.manager()) {
            buttons.add(small("gui.mms_dungeons.withdraw", () -> send("withdraw " + amount.getValue().strip())));
        }
        list.addField(Component.translatable("gui.mms_dungeons.treasury", state.treasury()).getString(), amount, buttons);
    }

    // A settings row: a text field and a Set button that sends the command it builds.
    private void setting(String key, String value, int max, UnaryOperator<String> command) {
        EditBox box = new EditBox(font, 0, 0, PANEL_WIDTH - SMALL_BUTTON - 8 - 100, 18, Component.translatable(key));
        box.setMaxLength(max);
        box.setValue(value);
        Button set = small("gui.mms_dungeons.set", () -> send(command.apply(box.getValue().strip())));
        list.addField(Component.translatable(key).getString(), box, List.of(set));
    }

    private Button small(String key, Runnable action) {
        return Button.builder(Component.translatable(key), button -> action.run()).size(SMALL_BUTTON, 18).build();
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, NAME_COLOR);
        if (state == null) {
            graphics.centeredText(font, Component.translatable("gui.mms_dungeons.loading"), width / 2, height / 2, LABEL_COLOR);
        } else if (state.guilds().isEmpty()) {
            graphics.centeredText(font, Component.translatable("gui.mms_dungeons.no_guilds"), width / 2, 60, LABEL_COLOR);
        }
    }

    private final class RowList extends ContainerObjectSelectionList<RowList.Entry> {

        RowList(int y, int height) {
            super(GuildMenuScreen.this.minecraft, GuildMenuScreen.this.width, height, y, ROW_HEIGHT);
        }

        @Override
        public int getRowWidth() {
            return PANEL_WIDTH;
        }

        void add(String name, String detail, String tooltip, List<Button> buttons) {
            addEntry(new Entry(name, detail, tooltip, null, buttons));
        }

        void addField(String name, EditBox box, List<Button> buttons) {
            addEntry(new Entry(name, "", "", box, buttons));
        }

        // One row: a name with a grey detail, an optional text field, and buttons on the right.
        final class Entry extends ContainerObjectSelectionList.Entry<Entry> {

            private final String name;
            private final String detail;
            // The charter shown on hover, or empty for none.
            private final String tooltip;
            private final @Nullable EditBox box;
            private final List<Button> buttons;
            private final List<GuiEventListener> children = new ArrayList<>();

            Entry(String name, String detail, String tooltip, @Nullable EditBox box, List<Button> buttons) {
                this.name = name;
                this.detail = detail;
                this.tooltip = tooltip;
                this.box = box;
                this.buttons = buttons;
                if (box != null) {
                    children.add(box);
                }
                children.addAll(buttons);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return children;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return buttons;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                                       float partialTick) {
                int x = getContentX() + 4;
                int right = getContentRight();
                for (int i = buttons.size() - 1; i >= 0; i--) {
                    Button button = buttons.get(i);
                    right -= button.getWidth();
                    button.setX(right);
                    button.setY(getContentYMiddle() - button.getHeight() / 2);
                    button.extractRenderState(graphics, mouseX, mouseY, partialTick);
                    right -= 2;
                }
                if (box != null) {
                    box.setX(right - box.getWidth() - 2);
                    box.setY(getContentYMiddle() - box.getHeight() / 2);
                    box.extractRenderState(graphics, mouseX, mouseY, partialTick);
                    right = box.getX() - 2;
                }
                int textWidth = right - x - 4;
                if (detail.isEmpty()) {
                    graphics.text(font, font.plainSubstrByWidth(name, textWidth), x, getContentYMiddle() - 4, NAME_COLOR, true);
                } else {
                    graphics.text(font, font.plainSubstrByWidth(name, textWidth), x, getContentY() + 1, NAME_COLOR, true);
                    graphics.text(font, font.plainSubstrByWidth(detail, textWidth), x, getContentY() + 11, LABEL_COLOR, false);
                }
                if (hovered && !tooltip.isEmpty() && mouseX < right) {
                    graphics.setTooltipForNextFrame(font, Component.translatable("gui.mms_dungeons.charter_tooltip")
                        .append(Component.literal(tooltip).withStyle(ChatFormatting.ITALIC)), mouseX, mouseY);
                }
            }
        }
    }
}
