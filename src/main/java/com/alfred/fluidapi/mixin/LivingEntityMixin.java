package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow protected abstract boolean shouldSwimInFluids();

    @Shadow public abstract boolean canWalkOnFluid(FluidState state);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void modifySwimSpeed(Vec3d movementInput, CallbackInfo ci) {
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if (this.isInLiquid() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState) && fluidState.getFluid() instanceof CustomFluid fluid)
            this.setVelocity(this.getVelocity().multiply(fluid.getVelocityMultiplier().x, fluid.getVelocityMultiplier().y, fluid.getVelocityMultiplier().z));
    }

    @Unique
    private boolean isInLiquid() {
        if (this.isTouchingWater())
            return true;

        Box box = this.getBoundingBox().contract(0.001);
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
                    FluidState fluidState = this.getWorld().getFluidState(mutable);
                    double e = ((float)q + fluidState.getHeight(this.getWorld(), mutable));
                    if (e >= box.minY)
                        return true;
                }
            }
        }
        return false;
    }
}
