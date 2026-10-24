package dev.nullmod.client;

import dev.nullmod.NullMod;
import dev.nullmod.core.AbilityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class ThemeAudio {
    private static final float THEME_VOLUME = 0.45f;
    private static final float FADE_PER_TICK = 0.03f;
    private static final int IDLE_LIMIT = 6000;

    private static final Theme ZERO = new Theme("zero");
    private static final Theme DRAGON = new Theme("dragon");
    private static final Theme CONFESS = new Theme("fullconfess");

    private ThemeAudio() {}

    private static final class Theme {
        final SoundEvent event;
        ThemeSound sound;
        int idle;
        int grace;

        Theme(String name) {
            this.event = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NullMod.MODID, name));
        }

        void update(Minecraft mc, float target) {
            if (grace > 0) {
                grace--;
            } else if (sound != null && !mc.getSoundManager().isActive(sound)) {
                sound.end();
                sound = null;
            }
            if (sound == null) {
                if (target <= 0f) return;
                sound = new ThemeSound(event);
                grace = 40;
                mc.getSoundManager().play(sound);
            }
            sound.target = target * THEME_VOLUME;
            if (target <= 0f && sound.getVolume() <= 0.001f) {
                if (++idle > IDLE_LIMIT) {
                    mc.getSoundManager().stop(sound);
                    sound.end();
                    sound = null;
                    idle = 0;
                }
            } else {
                idle = 0;
            }
        }

        void stop(Minecraft mc) {
            if (sound != null) {
                mc.getSoundManager().stop(sound);
                sound.end();
                sound = null;
            }
            idle = 0;
        }
    }

    private static final class ThemeSound extends AbstractTickableSoundInstance {
        float target;

        ThemeSound(SoundEvent event) {
            super(event, SoundSource.PLAYERS, RandomSource.create());
            this.volume = 0f;
            this.looping = false;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            this.volume = Mth.approach(this.volume, target, FADE_PER_TICK);
        }

        void end() {
            this.stop();
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            stopAll();
            return;
        }
        float zero = 0f, dragon = 0f, confess = 0f;
        float range = Math.max(1f, ZoneState.range());
        for (ZoneState.Zone z : ZoneState.zones()) {
            float audible;
            if (z.owner() == player.getId()) {
                audible = 1f;
            } else {
                double dist = Math.sqrt(player.distanceToSqr(z.x(), z.y(), z.z()));
                audible = dist <= range
                        ? (float) (1.0 - 0.6 * dist / range)
                        : (float) Math.max(0.0, 0.4 - 0.4 * (dist - range) / (range * 0.5));
            }
            switch (z.kind()) {
                case AbilityManager.K_STOP -> dragon = Math.max(dragon, audible);
                case AbilityManager.K_GHOST -> {
                    if (z.clash()) confess = Math.max(confess, audible);
                    else zero = Math.max(zero, audible);
                }
                case AbilityManager.K_HUNTER -> {
                    if (z.clash()) confess = Math.max(confess, audible);
                }
                default -> { }
            }
        }
        if (dragon > 0f) {
            zero = 0f;
            confess = 0f;
        } else if (confess > 0f) {
            zero = 0f;
        }
        ZERO.update(mc, zero);
        DRAGON.update(mc, dragon);
        CONFESS.update(mc, confess);
    }

    public static void stopAll() {
        Minecraft mc = Minecraft.getInstance();
        ZERO.stop(mc);
        DRAGON.stop(mc);
        CONFESS.stop(mc);
    }
}
