package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.LeveledCauldronBlock;
import com.alfred.fluidapi.registry.FluidBuilder;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {
    @Inject(method = "isFluidLiquid", at = @At("HEAD"), cancellable = true)
    private static void yesThisIsLiquid(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (fluid instanceof CustomFluid fluid1 && fluid1.isDrippable())
            cir.setReturnValue(true);
    }

    @Redirect(method = "dripTick", at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"))
    private static Object isEqual(Optional<PointedDripstoneBlock.DrippingFluid> instance) {
        PointedDripstoneBlock.DrippingFluid dripping = instance.get();
        Fluid fluid = dripping.fluid;
        if (fluid instanceof CustomFluid fluid1 && fluid1.isDrippable()) {
            AbstractCauldronBlock cauldron = FluidBuilder.CAULDRONS.get(FluidBuilder.FLUIDS.entrySet().stream().filter(entry -> fluid1.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElse(null));
            if (cauldron instanceof LeveledCauldronBlock)
                return new PointedDripstoneBlock.DrippingFluid(dripping.pos, fluid1 == fluid1.getStill() ? Fluids.WATER : Fluids.FLOWING_WATER, dripping.sourceState);
            else
                return new PointedDripstoneBlock.DrippingFluid(dripping.pos, fluid1 == fluid1.getStill() ? Fluids.LAVA : Fluids.FLOWING_LAVA, dripping.sourceState);
        }
        return instance.get();
    }

    // honestly this is so scuffed
    // maybe add an option to modify the chance of the dripstone filling up the cauldron, instead of only using vanilla water & vanilla lava chances?
    // also needs an option to modify the drip particle
    @Inject(method = "dripTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/PointedDripstoneBlock;getTipPos(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;IZ)Lnet/minecraft/util/math/BlockPos;"))
    private static void resetFluids(BlockState state, ServerWorld world, BlockPos pos, float dripChance, CallbackInfo ci, @Local Optional<PointedDripstoneBlock.DrippingFluid> optional, @Local LocalRef<Fluid> fluid) {
        fluid.set(optional.get().fluid);
    }
}
