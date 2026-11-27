package info.mudbourn.mmsdungeons.client.guild;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.guild.GuildBlocks;
import info.mudbourn.mmsdungeons.net.BountyListPayload;
import info.mudbourn.mmsdungeons.net.BoardNoticePayload;
import info.mudbourn.mmsdungeons.net.GuildMenuPayload;
import info.mudbourn.mmsdungeons.net.OpenBoardPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;

// Client side of guilds: the quest board renderer and screens, the packet receivers and the My Quests and Contracts key.
public final class GuildsClient {

    private GuildsClient() {
    }

    public static void register() {
        ModelLayerRegistry.registerModelLayer(QuestBoardRenderer.LAYER, QuestBoardModel::createBodyLayer);
        BlockEntityRendererRegistry.register(GuildBlocks.QUEST_BOARD_ENTITY, QuestBoardRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(OpenBoardPayload.TYPE, (payload, context) ->
            context.client().execute(() -> context.client().setScreenAndShow(
                new QuestBoardScreen(payload.guildId(), payload.guildName(), payload.postsQuests()))));
        ClientPlayNetworking.registerGlobalReceiver(BountyListPayload.TYPE, (payload, context) ->
            context.client().execute(() -> BountySync.push(payload.guildId(), payload.bounties())));
        ClientPlayNetworking.registerGlobalReceiver(GuildMenuPayload.TYPE, (payload, context) ->
            context.client().execute(() -> GuildMenuScreen.receive(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(BoardNoticePayload.TYPE, (payload, context) ->
            context.client().execute(() -> notice(context.client(), payload)));

        KeyMapping.Category category = KeyMapping.Category.register(MmsDungeons.id("guilds"));
        KeyMapping openMine = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.mms_dungeons.my_bounties", InputConstants.KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMine.consumeClick()) {
                if (client.player != null && client.canInterruptScreen()) {
                    client.setScreenAndShow(new MyBountiesScreen(null));
                }
            }
        });
    }

    // A board result: a toast over an open screen, where the action bar is hidden, else the action bar.
    private static void notice(Minecraft client, BoardNoticePayload payload) {
        Component message = Component.literal(payload.message());
        if (client.gui.screen() == null) {
            client.player.sendOverlayMessage(message);
            return;
        }
        Component title = Component.translatable(payload.failed() ? "gui.mms_dungeons.board_failed" : "gui.mms_dungeons.board_done");
        SystemToast.addOrUpdate(client.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
    }
}
