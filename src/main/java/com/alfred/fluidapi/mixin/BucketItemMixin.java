package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/BlockHitResult;getBlockPos()Lnet/minecraft/util/math/BlockPos;"), cancellable = true)
    private void dontDrain(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir, @Local BlockHitResult result) {
        if (result.getType() == HitResult.Type.BLOCK && world.getBlockState(result.getBlockPos()).getBlock() instanceof FluidBlock fluidBlock && fluidBlock.getFluidState(world.getBlockState(result.getBlockPos())).getFluid() instanceof CustomFluid fluid && fluid.getBucketItem() == null)
            cir.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
    }
}
