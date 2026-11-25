package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.guild.BountyType;
import info.mudbourn.mmsdungeons.net.PostBountyPayload;
import info.mudbourn.mmseconomy.economy.Currency;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// The form for posting a quest (from the guild treasury) or a contract (from the player's wallet).
final class PostBountyScreen extends Screen {

    private final Screen parent;
    private final boolean quest;
    private EditBox titleBox;
    private EditBox descriptionBox;
    private EditBox countBox;
    private EditBox rewardBox;
    private Button typeButton;
    private ItemStack proof = ItemStack.EMPTY;
    private BountyType type = BountyType.SUBJUGATION;
    private String titleText = "";
    private String descriptionText = "";
    private String countText = "1";
    private String rewardText = "";

    PostBountyScreen(Screen parent, boolean quest) {
        super(Component.translatable(quest ? "gui.mms_dungeons.post_quest" : "gui.mms_dungeons.post_contract"));
        this.parent = parent;
        this.quest = quest;
    }

    @Override
    protected void init() {
        int left = width / 2 - 120;
        titleBox = field(left, 40, 240, 40, titleText, "gui.mms_dungeons.field_title");
        descriptionBox = field(left, 72, 240, 200, descriptionText, "gui.mms_dungeons.field_description");
        countBox = field(left, 148, 60, 6, countText, "gui.mms_dungeons.field_count");
        rewardBox = field(left + 80, 148, 100, 12, rewardText, "gui.mms_dungeons.field_reward");
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.choose_item"), button -> chooseItem())
            .bounds(left, 104, 240, 20)
            .build());
        typeButton = addRenderableWidget(Button.builder(typeLabel(), button -> cycleType())
            .bounds(left, 176, 240, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.post"), button -> submit())
            .bounds(left, height - 52, 118, 20)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mms_dungeons.cancel"), button -> onClose())
            .bounds(left + 122, height - 52, 118, 20)
            .build());
    }

    private EditBox field(int x, int y, int w, int max, String text, String label) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.translatable(label));
        box.setMaxLength(max);
        box.setValue(text);
        return addRenderableWidget(box);
    }

    private Component typeLabel() {
        return Component.translatable("gui.mms_dungeons.type", type.label());
    }

    private void cycleType() {
        type = BountyType.values()[(type.ordinal() + 1) % BountyType.values().length];
        typeButton.setMessage(typeLabel());
    }

    private void chooseItem() {
        remember();
        minecraft.setScreenAndShow(new ItemSelectionScreen(this, stack -> proof = stack));
    }

    private void remember() {
        titleText = titleBox.getValue();
        descriptionText = descriptionBox.getValue();
        countText = countBox.getValue();
        rewardText = rewardBox.getValue();
    }

    private void submit() {
        remember();
        long pice = Currency.parse(rewardText);
        int amount;
        try {
            amount = Integer.parseInt(countText.trim());
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (proof.isEmpty() || amount < 1 || pice < 1) {
            minecraft.player.sendOverlayMessage(Component.translatable("gui.mms_dungeons.form_incomplete"));
            return;
        }
        ClientPlayNetworking.send(new PostBountyPayload(titleText, descriptionText, proof, amount, pice, type));
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 120;
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable(quest ? "gui.mms_dungeons.quest_note" : "gui.mms_dungeons.contract_note"),
            width / 2, 24, 0xFFAAAAAA);
        graphics.text(font, Component.translatable("gui.mms_dungeons.field_title"), left, 30, 0xFFAAAAAA, false);
        graphics.text(font, Component.translatable("gui.mms_dungeons.field_description"), left, 62, 0xFFAAAAAA, false);
        graphics.text(font, Component.translatable("gui.mms_dungeons.proof_item"), left, 94, 0xFFAAAAAA, false);
        graphics.text(font, Component.translatable("gui.mms_dungeons.field_count"), left, 138, 0xFFAAAAAA, false);
        graphics.text(font, Component.translatable("gui.mms_dungeons.field_reward"), left + 80, 138, 0xFFAAAAAA, false);
        if (!proof.isEmpty()) {
            graphics.item(proof, left + 4, 126);
            graphics.text(font, proof.getHoverName(), left + 26, 130, 0xFFFFFFFF, true);
        }
    }
}
