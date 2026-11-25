package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.guild.BountyStatus;
import info.mudbourn.mmsdungeons.guild.BountyView;
import info.mudbourn.mmsdungeons.net.BountyActionPayload;
import info.mudbourn.mmsdungeons.net.RequestBountiesPayload;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// The quests and contracts the player posted or their party holds, with abandon and delete.
public final class MyBountiesScreen extends Screen implements BountySync.Listener {

    private final Screen parent;
    private BountyList list;
    private Button abandon;
    private Button delete;

    public MyBountiesScreen(Screen parent) {
        super(Component.translatable("gui.mms_dungeons.my_bounties"));
        this.parent = parent;
    }

    @Override
    public void added() {
        super.added();
        BountySync.set(this);
    }

    @Override
    public void removed() {
        super.removed();
        BountySync.clear(this);
    }

    @Override
    protected void init() {
        list = addRenderableWidget(new BountyList(minecraft, width, height - 68, 28));
        int left = width / 2 - 156;
        int y = height - 26;
        abandon = addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.abandon"),
                button -> act(BountyActionPayload.Action.ABANDON)
            )
            .bounds(left, y, 100, 20)
            .build());
        delete = addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.delete"),
                button -> act(BountyActionPayload.Action.DELETE)
            )
            .bounds(left + 106, y, 100, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.close"), button -> onClose())
            .bounds(left + 212, y, 100, 20)
            .build());
        ClientPlayNetworking.send(new RequestBountiesPayload(Optional.empty()));
    }

    private void act(BountyActionPayload.Action action) {
        BountyView view = list.selectedView();
        if (view != null) {
            ClientPlayNetworking.send(new BountyActionPayload(action, view.bounty().id));
            ClientPlayNetworking.send(new RequestBountiesPayload(Optional.empty()));
        }
    }

    @Override
    public void onBounties(Optional<UUID> scope, List<BountyView> views) {
        if (scope.isEmpty() && list != null) {
            list.setViews(views);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BountyView view = list.selectedView();
        abandon.active = view != null && view.holding();
        delete.active = view != null && view.bounty().status == BountyStatus.AVAILABLE;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }
}
