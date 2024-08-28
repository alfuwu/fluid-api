package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidBlock.class)
public abstract class FluidBlockMixin {
    @Shadow @Final protected FlowableFluid fluid;
    @Shadow @Final public static ImmutableList<Direction> FLOW_DIRECTIONS;

    @Inject(method = "receiveNeighborFluids", at = @At("HEAD"), cancellable = true)
    private void modifyCombinations(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (this.fluid instanceof CustomFluid fluid1) {
            for (Direction direction : FLOW_DIRECTIONS) {
                BlockState newState = fluid1.getCombinesWith().test((FluidBlock) (Object) this, world, pos.offset(direction.getOpposite()), direction);
                if (newState != null)
                    world.setBlockState(pos, newState);
            }
            cir.setReturnValue(true);
        }
    }
}
