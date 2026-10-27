package nullmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DestinationGhost {
    private static PlayerModel<AbstractClientPlayer> wide;
    private static PlayerModel<AbstractClientPlayer> slim;

    private DestinationGhost() {}

    public static void render(Minecraft mc, Player player, Vec3 feet, PoseStack ps,
                              MultiBufferSource.BufferSource buffers, float pt) {
        if (wide == null) {
            wide = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            wide.young = false;
            slim.young = false;
        }
        PlayerSkin skin;
        PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(player.getUUID()) : null;
        skin = info != null ? info.getSkin() : ((AbstractClientPlayer) player).getSkin();
        PlayerModel<AbstractClientPlayer> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
        model.head.xRot = player.getXRot() * ((float) Math.PI / 180f);
        model.head.yRot = 0f;
        model.hat.copyFrom(model.head);

        float yaw = lerpYaw(player, pt);
        ps.pushPose();
        ps.translate(feet.x, feet.y, feet.z);
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        ps.scale(-1.0f, -1.0f, 1.0f);
        ps.scale(0.9375f, 0.9375f, 0.9375f);
        ps.translate(0.0f, -1.501f, 0.0f);
        int color = FastColor.ARGB32.color(120, 120, 235, 255);
        model.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(skin.texture())),
                15728880, OverlayTexture.NO_OVERLAY, color);
        buffers.endBatch();
        ps.popPose();
    }

    private static float lerpYaw(Player p, float pt) {
        return net.minecraft.util.Mth.rotLerp(pt, p.yRotO, p.getYRot());
    }
}
