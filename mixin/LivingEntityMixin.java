package nullmod.mixin;

import nullmod.core.TimeScale;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "aiStep()V", at = @At("HEAD"), require = 0)
    private void nullmod$floatInSlowTime(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player || self.onGround() || self.level().isClientSide) {
            return;
        }
        Vec3 v = self.getDeltaMovement();
        if (v.y < 0.0 && TimeScale.factor(self) > 1) {
            self.setDeltaMovement(v.x, v.y * 0.4, v.z);
        }
    }
}
