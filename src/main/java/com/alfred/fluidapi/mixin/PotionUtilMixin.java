package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.bottles.FluidLingeringPotionItem;
import com.alfred.fluidapi.bottles.FluidPotionItem;
import com.alfred.fluidapi.bottles.FluidSplashPotionItem;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionUtil.class)
public class PotionUtilMixin {
    @Inject(method = "getColor(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private static void modifyBottleColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof FluidPotionItem pot && pot.getTintColor() > -1)
            cir.setReturnValue(pot.getTintColor());
        else if (stack.getItem() instanceof FluidSplashPotionItem pot && pot.getTintColor() > -1)
            cir.setReturnValue(pot.getTintColor());
        else if (stack.getItem() instanceof FluidLingeringPotionItem pot && pot.getTintColor() > -1)
            cir.setReturnValue(pot.getTintColor());
    }
}
