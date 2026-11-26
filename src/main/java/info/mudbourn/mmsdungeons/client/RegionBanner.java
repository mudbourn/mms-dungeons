package info.mudbourn.mmsdungeons.client;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

// Draws the region name in Alagard with a glow and a fading underline.
public final class RegionBanner implements HudElement {

    private static final FontDescription FONT = new FontDescription.Resource(MmsDungeons.id("alagard"));
    private static final long FADE_IN = 1200L;
    private static final long HOLD = 3200L;
    private static final long FADE_OUT = 1600L;
    private static final float SCALE = 1.5f;
    private static final int GLOW_RINGS = 3;
    private static final int GLOW_STEPS = 16;
    private static final float GLOW_RADIUS = 1.4f;
    private static final float GLOW_ALPHA = 0.07f;
    private static final float UNDERLINE_SPREAD = 1.5f;
    private static final int UNDERLINE_SEGMENT = 2;

    private static Component title;
    private static int color;
    private static long shownAt;

    public static void show(String text, int rgb) {
        title = Component.literal(text).withStyle(Style.EMPTY.withFont(FONT));
        color = rgb & 0xFFFFFF;
        shownAt = System.currentTimeMillis();
    }

    public static void clear() {
        title = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (title == null) {
            return;
        }
        long age = System.currentTimeMillis() - shownAt;
        if (age > FADE_IN + HOLD + FADE_OUT) {
            title = null;
            return;
        }
        float fade = age < FADE_IN
            ? (float) age / FADE_IN
            : age < FADE_IN + HOLD ? 1.0f : 1.0f - (float) (age - FADE_IN - HOLD) / FADE_OUT;
        fade = fade * fade * (3.0f - 2.0f * fade);
        if (fade <= 0.01f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int textWidth = font.width(title);
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f - 28.0f;

        context.pose().pushMatrix();
        context.pose().translate(centerX, centerY);
        context.pose().scale(SCALE, SCALE);
        drawGlow(context, font, textWidth, fade);
        context.text(font, title, -textWidth / 2, -font.lineHeight / 2, argb(fade, lighten(color)), false);
        context.pose().popMatrix();

        drawUnderline(context, centerX, centerY + font.lineHeight * SCALE / 2.0f + 3.0f, textWidth * SCALE * UNDERLINE_SPREAD, fade);
    }

    private static void drawGlow(GuiGraphicsExtractor context, Font font, int textWidth, float fade) {
        int glow = argb(fade * GLOW_ALPHA, color);
        for (int ring = 1; ring <= GLOW_RINGS; ring++) {
            float radius = GLOW_RADIUS * ring / GLOW_RINGS;
            for (int step = 0; step < GLOW_STEPS; step++) {
                float angle = Mth.TWO_PI * step / GLOW_STEPS;
                context.pose().pushMatrix();
                context.pose().translate(Mth.cos(angle) * radius, Mth.sin(angle) * radius);
                context.text(font, title, -textWidth / 2, -font.lineHeight / 2, glow, false);
                context.pose().popMatrix();
            }
        }
    }

    private static void drawUnderline(GuiGraphicsExtractor context, float centerX, float y, float width, float fade) {
        int left = Math.round(centerX - width / 2.0f);
        int right = Math.round(centerX + width / 2.0f);
        int line = Math.round(y);
        for (int x = left; x < right; x += UNDERLINE_SEGMENT) {
            float along = (x + UNDERLINE_SEGMENT / 2.0f - centerX) / (width / 2.0f);
            float edge = 1.0f - along * along;
            float alpha = fade * edge * edge;
            int end = Math.min(x + UNDERLINE_SEGMENT, right);
            context.fill(x, line - 1, end, line, argb(alpha * 0.25f, color));
            context.fill(x, line, end, line + 1, argb(alpha * 0.9f, lighten(color)));
            context.fill(x, line + 1, end, line + 2, argb(alpha * 0.25f, color));
        }
    }

    private static int lighten(int rgb) {
        int r = (rgb >> 16 & 0xFF) + (255 - (rgb >> 16 & 0xFF)) * 3 / 4;
        int g = (rgb >> 8 & 0xFF) + (255 - (rgb >> 8 & 0xFF)) * 3 / 4;
        int b = (rgb & 0xFF) + (255 - (rgb & 0xFF)) * 3 / 4;
        return r << 16 | g << 8 | b;
    }

    private static int argb(float alpha, int rgb) {
        int a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return a << 24 | rgb;
    }
}
