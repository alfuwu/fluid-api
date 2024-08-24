package com.alfred.fluidapi.mixin.client.render;

import com.alfred.fluidapi.CustomFluid;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @SuppressWarnings("unused")
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getSubmersionType()Lnet/minecraft/client/render/CameraSubmersionType;"))
    private static CameraSubmersionType getFluidSubmersionType(CameraSubmersionType original, @Local Camera camera) {
        if (isSubmergedInFluid(camera))
            return getSubmergedFluid(camera).getSubmersionType();
        return original;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BackgroundRenderer;getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"))
    private static void modifyFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (isSubmergedInFluid(camera)) {
            CustomFluid.FogData fog = getSubmergedFluid(camera).getFog();
            red = fog.r;
            green = fog.g;
            blue = fog.b;
        }
    }

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void modifyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (isSubmergedInFluid(camera)) {
            ci.cancel();
            BackgroundRenderer.FogData fogData = new BackgroundRenderer.FogData(fogType);
            Entity entity = camera.getFocusedEntity();
            CustomFluid.FogData fog = getSubmergedFluid(camera).getFog();
            Pair<Float, Float> pair = fog.getFog(entity, viewDistance, entity instanceof ClientPlayerEntity clientPlayer ? clientPlayer.getUnderwaterVisibility() : -1);
            fogData.fogStart = pair.getFirst();
            fogData.fogEnd = pair.getSecond();
            RenderSystem.setShaderFogStart(fogData.fogStart);
            RenderSystem.setShaderFogEnd(fogData.fogEnd);
            RenderSystem.setShaderFogShape(fogData.fogShape);
        }
    }

    @Unique
    private static boolean isSubmergedInFluid(Camera camera) {
        if (camera.isReady()) {
            FluidState fluidState = camera.area.getFluidState(camera.getBlockPos());
            if (fluidState.getFluid() instanceof CustomFluid fluid) {
                if (FluidTags.WATER.equals(fluid.getTag())) {
                    return camera.getPos().y < camera.getBlockPos().getY() + fluidState.getHeight(camera.area, camera.getBlockPos());
                } else {
                    // why
                    Camera.Projection projection = camera.getProjection();
                    List<Vec3d> list = Arrays.asList(projection.center, projection.getBottomRight(), projection.getTopRight(), projection.getBottomLeft(), projection.getTopLeft());

                    for (Vec3d vec3d : list) {
                        Vec3d vec3d2 = camera.getPos().add(vec3d);
                        BlockPos blockPos = BlockPos.ofFloored(vec3d2);
                        FluidState fluidState2 = camera.area.getFluidState(blockPos);
                        if (fluidState2.getFluid() instanceof CustomFluid)
                            if (vec3d2.y <= (double) (fluidState2.getHeight(camera.area, blockPos) + (float) blockPos.getY()))
                                return true;
                    }
                }
            }
        }
        return false;
    }

    @Unique
    private static CustomFluid getSubmergedFluid(Camera camera) {
        return (CustomFluid) camera.area.getFluidState(camera.getBlockPos()).getFluid();
    }
}
