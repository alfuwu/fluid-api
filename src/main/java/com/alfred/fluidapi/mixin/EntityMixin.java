package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow private World world;
    @Shadow private BlockPos blockPos;
    @Shadow private Box boundingBox;

    @Shadow public abstract void emitGameEvent(GameEvent event);

    @Inject(method = "baseTick", at = @At("RETURN"))
    private void doThings(CallbackInfo ci) {
        Fluid fluid = this.getFluid();
        if (fluid instanceof CustomFluid fluid1 && fluid1.getEntityTick() != null)
            fluid1.getEntityTick().accept((Entity) (Object) this);
    }

    @Inject(method = "getSwimSound", at = @At("HEAD"), cancellable = true)
    private void getFluidSwimSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.world.getFluidState(this.blockPos);
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getSwimSound());
    }

    @Inject(method = "getSplashSound", at = @At("HEAD"), cancellable = true)
    private void getFluidSplashSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.world.getFluidState(this.blockPos);
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getSplashSound());
    }

    @Inject(method = "getHighSpeedSplashSound", at = @At("HEAD"), cancellable = true)
    private void getHighSpeedFluidSplashSound(CallbackInfoReturnable<SoundEvent> cir) {
        FluidState fluidState = this.world.getFluidState(this.blockPos);
        if (fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getHighSpeedSplashSound());
    }

    @Inject(method = "onSwimmingStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"), cancellable = true)
    private void spawnFluidParticles(CallbackInfo ci) {
        FluidState fluidState = this.world.getFluidState(this.blockPos);
        if (fluidState.getFluid() instanceof CustomFluid fluid) { // custom particles?
            ci.cancel();
            this.emitGameEvent(GameEvent.SPLASH);
        }
    }

    @ModifyArgs(method = "onSwimmingStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
    private void modifyFluidSplashSoundVolume(Args args) {
        Fluid fluid = this.getFluid();
        if (fluid instanceof CustomFluid fluid1)
            args.set(1, (float) args.get(1) * fluid1.getSplashSoundVolume());
    }

    @ModifyArgs(method = "playSwimSound(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
    private void modifyFluidSwimSoundVolume(Args args) {
        Fluid fluid = this.getFluid();
        if (fluid instanceof CustomFluid fluid1)
            args.set(1, (float) args.get(1) * fluid1.getSwimSoundVolume());
    }

    @Unique
    private Fluid getFluid() {
        Box box = this.boundingBox.contract(0.001);
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    mutable.set(p, q, r);
                    FluidState fluidState = this.world.getFluidState(mutable);
                    double e = ((float)q + fluidState.getHeight(this.world, mutable));
                    if (e >= box.minY)
                        return fluidState.getFluid();
                }
            }
        }
        return null;
    }
}
