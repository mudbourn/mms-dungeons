package info.mudbourn.mmsdungeons.client.locator;

import info.mudbourn.mmsdungeons.locator.LocatorTarget;
import info.mudbourn.mmsdungeons.net.LocatorPick;
import info.mudbourn.mmsdungeons.net.LocatorPoi;
import info.mudbourn.mmsdungeons.net.OpenTuningPayload;
import info.mudbourn.mmsdungeons.net.TuneLocatorPayload;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Lets the player tune the locator to any set of signals, sites and biomes, one dungeon region at a time.
final class TuningScreen extends Screen {

    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int HEADER_COLOR = 0xFFFFD27F;
    private static final int ICON_SIZE = 12;
    private static final String[] REGION_KEYS = {
        "gui.mms_dungeons.region.sanctuary",
        "gui.mms_dungeons.region.deep",
        "gui.mms_dungeons.region.periculum"
    };
    private static final Identifier ON_ICON = Identifier.withDefaultNamespace("container/beacon/confirm");
    private static final Identifier OFF_ICON = Identifier.withDefaultNamespace("container/beacon/cancel");

    private final OpenTuningPayload data;
    private final Set<LocatorPick> picked = new LinkedHashSet<>();
    private final List<LocatorPick> shown = new ArrayList<>();
    private final Button[] regionButtons = new Button[REGION_KEYS.length];
    private int region;
    private TargetList list;
    private EditBox filter;

    TuningScreen(OpenTuningPayload data) {
        super(Component.translatable("gui.mms_dungeons.tune_locator"));
        this.data = data;
        this.region = Math.max(0, Math.min(REGION_KEYS.length - 1, data.region()));
        this.picked.addAll(data.picks());
    }

    @Override
    protected void init() {
        for (int i = 0; i < REGION_KEYS.length; i++) {
            int index = i;
            regionButtons[i] = Button.builder(Component.translatable(REGION_KEYS[i]), button -> showRegion(index))
                .bounds(width / 2 - 125 + i * 85, 22, 80, 20)
                .build();
            addRenderableWidget(regionButtons[i]);
        }
        filter = new EditBox(font, width / 2 - 100, 46, 200, 18, Component.translatable("gui.mms_dungeons.filter"));
        filter.setResponder(text -> list.fill(text));
        addRenderableWidget(filter);
        list = new TargetList(minecraft, width, height - 102, 68);
        addRenderableWidget(list);
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.enable_all"), button -> setAll(true))
            .bounds(width / 2 - 154, height - 28, 100, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.disable_all"), button -> setAll(false))
            .bounds(width / 2 - 50, height - 28, 100, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.done"), button -> onClose())
            .bounds(width / 2 + 54, height - 28, 100, 20)
            .build());
        showRegion(region);
    }

    @Override
    public void onClose() {
        ClientPlayNetworking.send(new TuneLocatorPayload(new ArrayList<>(picked)));
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 8, NAME_COLOR);
    }

    private void showRegion(int index) {
        region = index;
        for (int i = 0; i < regionButtons.length; i++) {
            regionButtons[i].active = i != index;
        }
        list.fill(filter.getValue());
    }

    private void setAll(boolean on) {
        if (on) {
            picked.addAll(shown);
        } else {
            picked.removeAll(shown);
        }
    }

    private void toggle(LocatorPick pick) {
        if (!picked.remove(pick)) {
            picked.add(pick);
        }
    }

    private final class TargetList extends ContainerObjectSelectionList<TargetList.Entry> {

        private static final int ROW_HEIGHT = 22;
        private static final int BUTTON_SIZE = 20;

        TargetList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }

        @Override
        public int getRowWidth() {
            return width - 50;
        }

        void fill(String text) {
            clearEntries();
            shown.clear();
            String needle = text.toLowerCase(Locale.ROOT);
            group("gui.mms_dungeons.tune_presets", LocatorTarget.Kind.PRESET, data.presets(), needle);
            group("gui.mms_dungeons.tune_structures", LocatorTarget.Kind.STRUCTURE, data.structures(), needle);
            group("gui.mms_dungeons.tune_biomes", LocatorTarget.Kind.BIOME, data.biomes(), needle);
            setScrollAmount(0.0);
        }

        private void group(String headerKey, LocatorTarget.Kind kind, List<LocatorPoi> pois, String needle) {
            List<Entry> entries = new ArrayList<>();
            for (LocatorPoi poi : pois) {
                if ((poi.regions() & (1 << region)) == 0) {
                    continue;
                }
                LocatorTarget target = LocatorTarget.single(kind, poi.id());
                if (target.label().getString().toLowerCase(Locale.ROOT).contains(needle) || poi.id().toString().contains(needle)) {
                    entries.add(new Entry(target.label(), new LocatorPick(kind, poi.id())));
                }
            }
            if (entries.isEmpty()) {
                return;
            }
            addEntry(new Entry(Component.translatable(headerKey), null));
            for (Entry entry : entries) {
                addEntry(entry);
                shown.add(entry.pick);
            }
        }

        // One row: the name, and for a signal a button on the right that switches it on and off.
        final class Entry extends ContainerObjectSelectionList.Entry<Entry> {

            private final Component name;
            private final LocatorPick pick;
            private final List<Button> buttons;

            Entry(Component name, LocatorPick pick) {
                this.name = name;
                this.pick = pick;
                this.buttons = pick == null
                    ? List.of()
                    : List.of(Button.builder(Component.empty(), button -> toggle(pick)).size(BUTTON_SIZE, BUTTON_SIZE).build());
            }

            // A section header is only a label: the mouse never lands on it, so it can't be clicked, hovered or focused.
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return pick != null && super.isMouseOver(mouseX, mouseY);
            }

            @Override
            public ComponentPath nextFocusPath(FocusNavigationEvent event) {
                return pick == null ? null : super.nextFocusPath(event);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return buttons;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return buttons;
            }

            @Override
            public void extractContent(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
            ) {
                int textY = getContentYMiddle() - font.lineHeight / 2;
                if (pick == null) {
                    graphics.text(font, name, getContentX() + 4, textY, HEADER_COLOR, true);
                    return;
                }
                graphics.text(font, name, getContentX() + 4, textY, NAME_COLOR, true);
                Button button = buttons.get(0);
                button.setX(getContentRight() - BUTTON_SIZE);
                button.setY(getContentYMiddle() - BUTTON_SIZE / 2);
                button.extractRenderState(graphics, mouseX, mouseY, partialTick);
                graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    picked.contains(pick) ? ON_ICON : OFF_ICON,
                    button.getX() + (BUTTON_SIZE - ICON_SIZE) / 2,
                    button.getY() + (BUTTON_SIZE - ICON_SIZE) / 2,
                    ICON_SIZE,
                    ICON_SIZE);
            }
        }
    }
}
