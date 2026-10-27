package nullmod.client;

import nullmod.NullMod;
import nullmod.core.AbilityManager;
import nullmod.net.SyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ShaderController {
    private static final ResourceLocation CHAIN =
            ResourceLocation.fromNamespaceAndPath(NullMod.MODID, "shaders/post/chronos.json");

    private static PostChain ours;
    private static boolean broken;

    private static float bullet, ghost, stop, wd, glitch, psy, trail, echo, impact;
    private static int echoTicks;

    private ShaderController() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (ClientState.glitching()) {
            if (echoTicks > 0) {
                echoTicks--;
            } else if (mc.level != null && mc.level.random.nextFloat() < 0.04f) {
                echoTicks = 2 + mc.level.random.nextInt(3);
            }
        } else {
            echoTicks = 0;
        }
    }

    public static void frame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || broken) {
            unload(mc);
            return;
        }
        float zBullet = 0f, zGhost = 0f, zStop = 0f;
        double r2 = (double) ZoneState.range() * ZoneState.range();
        for (ZoneState.Zone z : ZoneState.zones()) {
            if (z.owner() == mc.player.getId() || mc.player.distanceToSqr(z.x(), z.y(), z.z()) > r2) continue;
            switch (z.kind()) {
                case AbilityManager.K_STOP -> zStop = 1f;
                case AbilityManager.K_GHOST -> zGhost = 1f;
                default -> zBullet = 1f;
            }
        }
        float tBullet = ClientState.has(SyncPayload.FLAG_BULLET) ? 1f : zBullet;
        float tGhost = ClientState.has(SyncPayload.FLAG_GHOST) || ClientState.has(SyncPayload.FLAG_REWIND) ? 1f : zGhost;
        float tStop = ClientState.has(SyncPayload.FLAG_STOP) ? 1f : zStop;
        float tWd = switch (ClientState.stage) { case 1 -> 0.15f; case 2 -> 0.4f; case 3 -> 0.7f; case 4 -> 1f; default -> 0f; };
        float tGlitch = ClientState.glitching() ? 1f : (ClientState.clashGlitch() ? 0.6f : 0f);
        float tPsy = ClientState.stage >= 4 && ClientState.glitching() ? 1f : 0f;

        bullet = Mth.lerp(0.2f, bullet, tBullet);
        ghost = Mth.lerp(0.2f, ghost, tGhost);
        stop = Mth.lerp(0.25f, stop, tStop);
        wd = Mth.lerp(0.05f, wd, tWd);
        glitch = Mth.lerp(0.4f, glitch, tGlitch);
        psy = Mth.lerp(0.4f, psy, tPsy);
        trail = Math.max(bullet * 0.35f, ghost * 0.45f);
        echo = echoTicks > 0 ? 0.55f : 0f;
        impact = ClientState.has(SyncPayload.FLAG_IMPACT) ? 1f : 0f;

        if (ours == null || mc.gameRenderer.currentEffect() != ours) {
            try {
                mc.gameRenderer.loadEffect(CHAIN);
                ours = mc.gameRenderer.currentEffect();
            } catch (Exception e) {
                NullMod.LOGGER.error("Could not load the Chronos shader chain; visuals disabled", e);
                broken = true;
                ours = null;
                return;
            }
        }
        if (ours != null) {
            ours.setUniform("Bullet", bullet);
            ours.setUniform("Ghost", ghost);
            ours.setUniform("Stop", stop);
            ours.setUniform("Wd", wd);
            ours.setUniform("Glitch", glitch);
            ours.setUniform("Psy", psy);
            ours.setUniform("Trail", trail);
            ours.setUniform("Echo", echo);
            ours.setUniform("Impact", impact);
            ours.setUniform("Seed", mc.level != null ? mc.level.random.nextFloat() : 0f);
        }
    }

    private static void unload(Minecraft mc) {
        if (ours != null) {
            if (mc.gameRenderer.currentEffect() == ours) {
                mc.gameRenderer.shutdownEffect();
            }
            ours = null;
        }
        bullet = ghost = stop = wd = glitch = psy = trail = echo = impact = 0f;
    }
}
