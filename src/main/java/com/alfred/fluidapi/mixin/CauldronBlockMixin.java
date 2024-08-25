package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.registry.Blocks;
import com.alfred.fluidapi.registry.FluidBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(CauldronBlock.class)
public class CauldronBlockMixin {
    @Inject(method = "canBeFilledByDripstone", at = @At("HEAD"), cancellable = true)
    private void dontDrip(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (fluid instanceof CustomFluid fluid1 && !fluid1.isDrippable())
            cir.setReturnValue(false);
    }

    @Inject(method = "fillFromDripstone", at = @At("HEAD"))
    private void fillWithCustomFluid(BlockState state, World world, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (fluid instanceof CustomFluid fluid1) {
            BlockState state1 = Blocks.CAULDRONS.get(FluidBuilder.FLUIDS.entrySet().stream().filter(entry -> fluid1.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().orElse(null)).getDefaultState();
            world.setBlockState(pos, state1);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state1));
        }
    }
}
