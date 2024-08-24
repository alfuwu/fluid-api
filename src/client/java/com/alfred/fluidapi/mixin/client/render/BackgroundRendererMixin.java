package com.alfred.fluidapi.mixin.client.render;

import com.alfred.fluidapi.CustomFluid;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow private static float red;
    @Shadow private static float green;
    @Shadow private static float blue;

    @SuppressWarnings("unused")
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getSubmersionType()Lnet/minecraft/client/render/CameraSubmersionType;"))
    private static CameraSubmersionType getSculkSubmersionType(CameraSubmersionType original, @Local Camera camera) {
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
            Pair<Float, Float> pair;
            if (entity.isSpectator())
                pair = fog.getSpectatorFog(viewDistance);
            else if (entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.NIGHT_VISION))
                pair = fog.getNightVisionFog(viewDistance);
            else
                pair = fog.getFog(viewDistance);
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
            return fluidState.getFluid() instanceof CustomFluid && camera.getPos().y < camera.getBlockPos().getY() + fluidState.getHeight(camera.area, camera.getBlockPos());
        }
        return false;
    }

    @Unique
    private static CustomFluid getSubmergedFluid(Camera camera) {
        return (CustomFluid) camera.area.getFluidState(camera.getBlockPos()).getFluid();
    }
}
