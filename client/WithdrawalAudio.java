package dev.nullmod.client;

import dev.nullmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class WithdrawalAudio {
    private static final float VOLUME = 0.12f;
    private static SoundInstance current;
    private static boolean wasGlitching;

    private WithdrawalAudio() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (ClientState.stage < 1) {
            stop();
            wasGlitching = false;
            return;
        }
        boolean glitching = ClientState.glitching();
        boolean playing = current != null && mc.getSoundManager().isActive(current);
        if (glitching && !wasGlitching && !playing) {
            SoundEvent event = (mc.level != null && mc.level.random.nextBoolean())
                    ? ModSounds.WITHDRAWAL_1.get() : ModSounds.WITHDRAWAL_2.get();
            current = new SimpleSoundInstance(event.getLocation(), SoundSource.AMBIENT, VOLUME, 1.0f,
                    RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            mc.getSoundManager().play(current);
        }
        wasGlitching = glitching;
    }

    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }
}
