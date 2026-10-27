package nullmod.net;

import nullmod.client.ClientHooks;
import nullmod.core.AbilityManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC, ClientHooks::onSync);
        registrar.playToClient(SpeechPayload.TYPE, SpeechPayload.CODEC, ClientHooks::onSpeech);
        registrar.playToClient(ZonesPayload.TYPE, ZonesPayload.CODEC, ClientHooks::onZones);
        registrar.playToServer(AbilityPayload.TYPE, AbilityPayload.CODEC, ModNetwork::onAbility);
    }

    private static void onAbility(AbilityPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AbilityManager.handle(player, payload);
        }
    }

    private ModNetwork() {}
}
