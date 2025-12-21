package dev.sakura.mixin.world;

import dev.sakura.Sakura;
import dev.sakura.events.entity.LiquidPushEvent;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.sakura.Sakura.mc;

@Mixin(FlowableFluid.class)
public class MixinFlowableFluid {
    @Inject(method = "getVelocity", at = @At("HEAD"), cancellable = true)
    private void onGetVelocity(BlockView world, BlockPos pos, FluidState state, CallbackInfoReturnable<Vec3d> cir) {
        if (mc.player == null) return;
/*
这有点笼统，因为它影响所有流体速度计算，
但通常这是用来查询实体移动的。
一个更有针对性的方法是 MixinEntity.updateMovementInFluid，但这通常也很有效。
*/

        LiquidPushEvent event = new LiquidPushEvent(mc.player);
        Sakura.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(Vec3d.ZERO);
        }
    }
}
