package com.alfred.fluidapi.mixin.client;

import com.alfred.fluidapi.CustomFluid;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Shadow private int underwaterVisibilityTicks;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "getUnderwaterVisibility", at = @At("HEAD"), cancellable = true)
    private void customFluidVisibility(CallbackInfoReturnable<Float> cir) {
        double d = this.getEyeY() - 0.1111111119389534;
        Entity entity = this.getVehicle();
        if (entity instanceof BoatEntity boatEntity)
            if (!boatEntity.isSubmergedInWater() && boatEntity.getBoundingBox().maxY >= d && boatEntity.getBoundingBox().minY <= d)
                return;
        BlockPos blockPos = BlockPos.ofFloored(this.getX(), d, this.getZ());
        FluidState fluidState = this.getWorld().getFluidState(blockPos);
        double e = blockPos.getY() + fluidState.getHeight(this.getWorld(), blockPos);
        if (e > d && fluidState.getFluid() instanceof CustomFluid fluid)
            cir.setReturnValue(fluid.getViewDistance(this, this.underwaterVisibilityTicks));
    }
}
