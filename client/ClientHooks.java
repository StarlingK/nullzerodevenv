package nullmod.client;

import nullmod.net.SpeechPayload;
import nullmod.net.SyncPayload;
import nullmod.net.ZonesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openRecipePaper() {
        Minecraft.getInstance().setScreen(new RecipePaperScreen());
    }

    public static void onSync(SyncPayload payload, IPayloadContext context) {
        ClientState.apply(payload);
    }

    public static void onZones(ZonesPayload payload, IPayloadContext context) {
        ZoneState.apply(payload);
    }

    public static void onSpeech(SpeechPayload payload, IPayloadContext context) {
        if (payload.kind() == SpeechPayload.CAPTAIN) {
            ClashRift.say(payload.arg());
            return;
        }
        ClientState.say(payload.kind(), payload.arg());
    }
}
