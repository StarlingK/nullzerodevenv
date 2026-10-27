package nullmod.mixin;

import nullmod.core.TimeScale;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "serverAiStep()V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nullmod$slowThinking(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        if (TimeScale.sitOut(self, TimeScale.factor(self))) {
            ci.cancel();
        }
    }
}
