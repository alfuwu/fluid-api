package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidState.class)
public abstract class FluidStateMixin {
    @Shadow public abstract Fluid getFluid();

    @Inject(method = "isIn(Lnet/minecraft/registry/tag/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void isFluidInTag(TagKey<Fluid> tag, CallbackInfoReturnable<Boolean> cir) {
        if (this.getFluid() instanceof CustomFluid fluid && tag.equals(fluid.getTag()))
            cir.setReturnValue(true);
    }
}
