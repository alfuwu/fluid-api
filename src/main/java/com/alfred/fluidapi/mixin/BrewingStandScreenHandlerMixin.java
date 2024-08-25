package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.bottles.FluidLingeringPotionItem;
import com.alfred.fluidapi.bottles.FluidPotionItem;
import com.alfred.fluidapi.bottles.FluidSplashPotionItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.screen.BrewingStandScreenHandler$PotionSlot")
public class BrewingStandScreenHandlerMixin {
    // why?
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void matches(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof FluidPotionItem || stack.getItem() instanceof FluidSplashPotionItem || stack.getItem() instanceof FluidLingeringPotionItem)
            cir.setReturnValue(true);
    }
}
