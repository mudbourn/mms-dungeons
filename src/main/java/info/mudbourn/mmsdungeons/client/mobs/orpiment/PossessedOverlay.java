package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

// Covers the screen in a haunting wash while the player is Possessed.
public final class PossessedOverlay implements HudElement {

    private static final Identifier TEXTURE = MmsDungeons.id("textures/gui/orpiment/possessed_overlay.png");
    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 256;

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.hasEffect(OrpimentEffects.POSSESSED)) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0f, 0.0f, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
