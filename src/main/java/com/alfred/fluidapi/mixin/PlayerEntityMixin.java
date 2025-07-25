package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getSwimSound", at = @At("HEAD"), cancellable = true)
    private void getFluidSwimSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getPlayerSwimSound());
    }

    @Inject(method = "getSplashSound", at = @At("HEAD"), cancellable = true)
    private void getFluidSplashSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getPlayerSplashSound());
    }

    @Inject(method = "getHighSpeedSplashSound", at = @At("HEAD"), cancellable = true)
    private void getFluidSpeedSculkSplashSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getPlayerHighSpeedSplashSound());
    }
}

