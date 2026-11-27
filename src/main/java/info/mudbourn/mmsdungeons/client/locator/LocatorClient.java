package info.mudbourn.mmsdungeons.client.locator;

import info.mudbourn.mmsdungeons.net.OpenTuningPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

// Client side of the locator: opens the tuning screen when the server sends the list.
public final class LocatorClient {

    private LocatorClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenTuningPayload.TYPE, (payload, context) ->
            context.client().execute(() -> context.client().setScreenAndShow(new TuningScreen(payload))));
    }
}
