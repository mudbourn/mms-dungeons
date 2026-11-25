package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.guild.BountyKind;
import info.mudbourn.mmsdungeons.guild.BountyStatus;
import info.mudbourn.mmsdungeons.guild.BountyView;
import info.mudbourn.mmsdungeons.net.BountyActionPayload;
import info.mudbourn.mmsdungeons.net.RequestBountiesPayload;
import info.mudbourn.mmseconomy.economy.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// The quest board: a guild's quests and contracts, with take, hand in, post and the player's own list.
public final class QuestBoardScreen extends Screen implements BountySync.Listener {

    private static final int BUTTON_WIDTH = 74;

    private final UUID guild;
    private final String guildName;
    private final boolean postsQuests;
    private BountyList list;
    private Button take;
    private Button handIn;

    public QuestBoardScreen(UUID guild, String guildName, boolean postsQuests) {
        super(Component.translatable("gui.mms_dungeons.quest_board", guildName));
        this.guild = guild;
        this.guildName = guildName;
        this.postsQuests = postsQuests;
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
        list = addRenderableWidget(new BountyList(minecraft, width, height - 104, 28));
        int left = width / 2 - (BUTTON_WIDTH * 5 + 16) / 2;
        int y = height - 26;
        take = addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.take"),
                button -> act(BountyActionPayload.Action.TAKE)
            )
            .bounds(left, y, BUTTON_WIDTH, 20)
            .build());
        handIn = addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.hand_in"),
                button -> act(BountyActionPayload.Action.HAND_IN)
            )
            .bounds(left + (BUTTON_WIDTH + 4), y, BUTTON_WIDTH, 20)
            .build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.post"),
                button -> minecraft.setScreenAndShow(new PostBountyScreen(this, postsQuests))
            )
            .bounds(left + (BUTTON_WIDTH + 4) * 2, y, BUTTON_WIDTH, 20)
            .build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.mms_dungeons.mine"),
                button -> minecraft.setScreenAndShow(new MyBountiesScreen(this))
            )
            .bounds(left + (BUTTON_WIDTH + 4) * 3, y, BUTTON_WIDTH, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.close"), button -> onClose())
            .bounds(left + (BUTTON_WIDTH + 4) * 4, y, BUTTON_WIDTH, 20)
            .build());
        ClientPlayNetworking.send(new RequestBountiesPayload(Optional.of(guild)));
        refreshButtons();
    }

    private void act(BountyActionPayload.Action action) {
        BountyView view = list.selectedView();
        if (view != null) {
            ClientPlayNetworking.send(new BountyActionPayload(action, view.bounty().id));
            ClientPlayNetworking.send(new RequestBountiesPayload(Optional.of(guild)));
        }
    }

    @Override
    public void onBounties(Optional<UUID> scope, List<BountyView> views) {
        if (scope.isPresent() && scope.get().equals(guild) && list != null) {
            list.setViews(views);
            refreshButtons();
        }
    }

    private void refreshButtons() {
        BountyView view = list == null ? null : list.selectedView();
        take.active = view != null && view.bounty().status == BountyStatus.AVAILABLE;
        handIn.active = view != null && view.holding() && view.have() >= view.bounty().proof.count();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refreshButtons();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        BountyView view = list.selectedView();
        if (view != null) {
            extractDetails(graphics, view);
        } else {
            Component hint = Component.translatable("gui.mms_dungeons.select_hint");
            graphics.centeredText(font, hint, width / 2, height - 70, 0xFFAAAAAA);
        }
    }

    private void extractDetails(GuiGraphicsExtractor graphics, BountyView view) {
        var bounty = view.bounty();
        int x = width / 2 - 190;
        int y = height - 74;
        Component split = bounty.kind == BountyKind.CONTRACT
            ? Component.translatable(
                "gui.mms_dungeons.split_contract",
                Currency.format(bounty.reward),
                Currency.format(view.commission()),
                view.partySize()
            )
            : Component.translatable("gui.mms_dungeons.split_quest", Currency.format(bounty.reward), view.partySize());
        graphics.text(font, split, x, y, 0xFFFFFFFF, false);
        Component share = Component.translatable("gui.mms_dungeons.your_share", Currency.format(view.yourShare()));
        graphics.text(font, share, x, y + 11, 0xFF55FF55, false);
        Component proof = Component.translatable(
            "gui.mms_dungeons.proof_have",
            bounty.proof.count(),
            bounty.proof.stack().getHoverName(),
            view.have()
        );
        graphics.text(font, proof, x, y + 22, 0xFFAAAAAA, false);
    }
}
