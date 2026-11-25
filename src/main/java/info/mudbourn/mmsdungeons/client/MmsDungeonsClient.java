package info.mudbourn.mmsdungeons.client;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.client.guild.GuildsClient;
import info.mudbourn.mmsdungeons.client.mobs.kobolds.KoboldsClient;
import info.mudbourn.mmsdungeons.client.mobs.orpiment.OrpimentClient;
import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.dungeon.DungeonBlocks;
import info.mudbourn.mmsdungeons.net.PartyAlertPayload;
import info.mudbourn.mmsdungeons.net.PartySyncPayload;
import info.mudbourn.mmsdungeons.party.PartyView;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public final class MmsDungeonsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PartySyncPayload.TYPE, (payload, context) ->
            context.client().execute(() -> PartyView.update(payload.members())));
        ClientPlayNetworking.registerGlobalReceiver(PartyAlertPayload.TYPE, (payload, context) ->
            context.client().execute(() -> PartyView.alert(
                payload.victim(),
                System.currentTimeMillis() + FeaturesConfig.get().alertSeconds * 1000L)));
        BlockEntityRendererRegistry.register(DungeonBlocks.DUNGEON_PORTAL_ENTITY, context -> new DungeonPortalRenderer());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PartyView.clear());
        GuildsClient.register();
        KoboldsClient.register();
        OrpimentClient.register();

        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            MmsDungeons.id("party_hud"),
            new PartyHudRenderer());
    }
}
