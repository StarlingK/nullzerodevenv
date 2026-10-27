package nullmod.mixin;

import nullmod.core.TimeScale;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin {
    @Shadow
    private int swell;

    @Shadow
    public abstract int getSwellDir();

    @Inject(method = "tick()V", at = @At("HEAD"), require = 0)
    private void nullmod$slowFuse(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (self.level().isClientSide || getSwellDir() <= 0 || swell <= 0) {
            return;
        }
        if (TimeScale.sitOut(self, TimeScale.factor(self))) {
            swell = Math.max(0, swell - getSwellDir());
        }
    }
}
