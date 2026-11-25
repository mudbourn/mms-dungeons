package info.mudbourn.mmsdungeons.client;

import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.net.PartyMember;
import info.mudbourn.mmsdungeons.party.PartyView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

// Draws the teammate rows with health, hunger and armor bars, plus the attack alert and its pointer.
public final class PartyHudRenderer implements HudElement {

    private static final int BAR_WIDTH = 22;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_GAP = 3;
    private static final int ROW_PADDING = 3;
    private static final int BAR_BACK = 0xAA000000;
    private static final int HEALTH_COLOR = 0xFFE04040;
    private static final int FOOD_COLOR = 0xFFC8861C;
    private static final int ARMOR_COLOR = 0xFFB0B8C8;
    private static final int ALERT_COLOR = 0xFFFF5555;

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || !FeaturesConfig.get().hudEnabled) {
            return;
        }
        List<PartyMember> teammates = PartyView.members().stream()
            .filter(member -> !member.id().equals(minecraft.player.getUUID()))
            .toList();
        if (teammates.isEmpty()) {
            return;
        }
        drawRows(context, minecraft, teammates);
        drawAlert(context, minecraft);
    }

    private static void drawRows(GuiGraphicsExtractor context, Minecraft minecraft, List<PartyMember> teammates) {
        FeaturesConfig config = FeaturesConfig.get();
        Font font = minecraft.font;
        int nameWidth = 0;
        for (PartyMember member : teammates) {
            nameWidth = Math.max(nameWidth, font.width(member.name()));
        }
        int rowHeight = font.lineHeight + ROW_PADDING;
        int width = nameWidth + 4 + 3 * BAR_WIDTH + 2 * BAR_GAP;
        int height = teammates.size() * rowHeight;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = config.hudHorizontal.equals("right") ? screenWidth - width - config.hudOffsetX : config.hudOffsetX;
        int y = switch (config.hudVertical) {
            case "top" -> config.hudOffsetY;
            case "bottom" -> screenHeight - height - config.hudOffsetY;
            default -> (screenHeight - height) / 2 + config.hudOffsetY;
        };
        for (PartyMember member : teammates) {
            context.text(font, member.name(), x, y, 0xFFFFFFFF, true);
            int barX = x + nameWidth + 4;
            int barY = y + (font.lineHeight - BAR_HEIGHT) / 2;
            drawBar(context, barX, barY, member.health() / Math.max(1.0F, member.maxHealth()), HEALTH_COLOR);
            drawBar(context, barX + BAR_WIDTH + BAR_GAP, barY, member.food() / 20.0F, FOOD_COLOR);
            drawBar(context, barX + 2 * (BAR_WIDTH + BAR_GAP), barY, member.armor() / 20.0F, ARMOR_COLOR);
            y += rowHeight;
        }
    }

    private static void drawBar(GuiGraphicsExtractor context, int x, int y, float fraction, int color) {
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_BACK);
        int filled = Math.round(BAR_WIDTH * Mth.clamp(fraction, 0.0F, 1.0F));
        if (filled > 0) {
            context.fill(x, y, x + filled, y + BAR_HEIGHT, color);
        }
    }

    private static void drawAlert(GuiGraphicsExtractor context, Minecraft minecraft) {
        Map.Entry<UUID, Long> alert = PartyView.latestAlert(System.currentTimeMillis());
        if (alert == null) {
            return;
        }
        PartyMember victim = PartyView.find(alert.getKey());
        if (victim == null) {
            return;
        }
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        context.centeredText(font, Component.literal(victim.name() + " is under attack!"), screenWidth / 2, 24, ALERT_COLOR);

        var player = minecraft.player;
        if (!victim.dimension().equals(player.level().dimension().identifier())) {
            return;
        }
        double dx = victim.x() - player.getX();
        double dy = victim.y() - player.getEyeY();
        double dz = victim.z() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(horizontal * horizontal + dy * dy);
        float yawTo = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float pitchTo = (float) (-Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
        float relativeYaw = Mth.wrapDegrees(yawTo - player.getYRot());
        float relativePitch = pitchTo - player.getXRot();

        double verticalFov = minecraft.options.fov().get();
        double aspect = (double) screenWidth / screenHeight;
        double horizontalHalf = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(verticalFov / 2.0)) * aspect));
        if (Math.abs(relativeYaw) < horizontalHalf && Math.abs(relativePitch) < verticalFov / 2.0) {
            return;
        }
        double radians = Math.toRadians(relativeYaw);
        double centerX = screenWidth / 2.0;
        double centerY = screenHeight / 2.0;
        double radiusX = screenWidth * 0.35;
        double radiusY = screenHeight * 0.35;
        for (double step : new double[] {0.55, 0.7, 0.85}) {
            int dotX = (int) (centerX + Math.sin(radians) * radiusX * step);
            int dotY = (int) (centerY - Math.cos(radians) * radiusY * step);
            context.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, ALERT_COLOR);
        }
        int markerX = (int) (centerX + Math.sin(radians) * radiusX);
        int markerY = (int) (centerY - Math.cos(radians) * radiusY);
        context.fill(markerX - 3, markerY - 3, markerX + 4, markerY + 4, ALERT_COLOR);
        String label = victim.name() + " " + Math.round(distance) + "m";
        int labelX = Mth.clamp(markerX - font.width(label) / 2, 2, screenWidth - font.width(label) - 2);
        context.text(font, label, labelX, markerY + 6, 0xFFFFFFFF, true);
    }
}
