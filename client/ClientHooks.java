package dev.nullmod.client;

import dev.nullmod.net.SpeechPayload;
import dev.nullmod.net.SyncPayload;
import dev.nullmod.net.ZonesPayload;
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
        ClientState.say(payload.kind(), payload.arg());
    }
}
