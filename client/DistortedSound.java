package dev.nullmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;

public class DistortedSound extends AbstractSoundInstance {
    public DistortedSound(SoundInstance original, float pitchMul, float volumeMul) {
        super(original.getLocation(), original.getSource(), RandomSource.create());
        original.resolve(Minecraft.getInstance().getSoundManager());

        this.volume = original.getVolume() * volumeMul;
        this.pitch = original.getPitch() * pitchMul;
        this.x = original.getX();
        this.y = original.getY();
        this.z = original.getZ();
        this.looping = original.isLooping();
        this.delay = original.getDelay();
        this.attenuation = original.getAttenuation();
        this.relative = original.isRelative();
    }
}
