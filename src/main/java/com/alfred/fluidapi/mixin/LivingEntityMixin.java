package com.alfred.fluidapi.mixin;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
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
import org.spongepowered.asm.mixin.injection.Redirect;
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
        if (this.isInLiquid() && this.shouldSwimInFluids() && !this.canWalkOnFluid(fluidState) && fluidState.getFluid() instanceof CustomFluid fluid && fluid.getVelocityMultiplier() != null)
            this.setVelocity(this.getVelocity().multiply(fluid.getVelocityMultiplier()));
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean isSubmergedInUnbreathableFluid(LivingEntity instance, TagKey<Fluid> tagKey) {
        Fluid submerged = getSubmergedFluid();
        return submerged instanceof CustomFluid fluid ? !fluid.isBreathable() : instance.isSubmergedIn(tagKey);
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

    @Unique
    private Fluid getSubmergedFluid() {
        double d = this.getEyeY() - 0.1111111119389534;
        Entity entity = this.getVehicle();
        if (entity instanceof BoatEntity boatEntity && !boatEntity.isSubmergedInWater() && boatEntity.getBoundingBox().maxY >= d && boatEntity.getBoundingBox().minY <= d)
            return null;

        BlockPos blockPos = BlockPos.ofFloored(this.getX(), d, this.getZ());
        FluidState fluidState = this.getWorld().getFluidState(blockPos);
        double e = blockPos.getY() + fluidState.getHeight(this.getWorld(), blockPos);
        if (e > d)
            return fluidState.getFluid();
        return null;
    }
}
