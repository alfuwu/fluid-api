package com.alfred.fluidapi.mixin.client.render;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.FluidApi;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @SuppressWarnings("unused")
    @WrapOperation(method = "renderOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSubmergedIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private static boolean renderCustomOverlay(ClientPlayerEntity instance, TagKey<Fluid> tag, Operation<Boolean> original, MinecraftClient client, MatrixStack matrices) {
        double d = instance.getEyeY() - 0.1111111119389534;
        Entity entity = instance.getVehicle();
        if (entity instanceof BoatEntity boatEntity)
            if (!boatEntity.isSubmergedInWater() && boatEntity.getBoundingBox().maxY >= d && boatEntity.getBoundingBox().minY <= d)
                return original.call(instance);
        BlockPos blockPos = BlockPos.ofFloored(instance.getX(), d, instance.getZ());
        FluidState fluidState = instance.getWorld().getFluidState(blockPos);
        double e = blockPos.getY() + fluidState.getHeight(instance.getWorld(), blockPos);
        if (e > d && fluidState.getFluid() instanceof CustomFluid fluid) {
            renderSubmergedTexture(client, matrices, fluid.getSubmergedTexture());
            return false;
        } else {
            return original.call(instance, tag);
        }
    }

    @Unique
    private static void renderSubmergedTexture(MinecraftClient client, MatrixStack matrices, Identifier texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        BlockPos blockPos = BlockPos.ofFloored(client.player.getX(), client.player.getEyeY(), client.player.getZ());
        float f = LightmapTextureManager.getBrightness(client.player.getWorld().getDimension(), client.player.getWorld().getLightLevel(blockPos));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(f, f, f, 0.1F);
        float g = 4.0F;
        float h = -1.0F;
        float i = 1.0F;
        float j = -1.0F;
        float k = 1.0F;
        float l = -0.5F;
        float m = -client.player.getYaw() / 64.0F;
        float n = client.player.getPitch() / 64.0F;
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, -1.0F, -1.0F, -0.5F).texture(4.0F + m, 4.0F + n).next();
        bufferBuilder.vertex(matrix4f, 1.0F, -1.0F, -0.5F).texture(0.0F + m, 4.0F + n).next();
        bufferBuilder.vertex(matrix4f, 1.0F, 1.0F, -0.5F).texture(0.0F + m, 0.0F + n).next();
        bufferBuilder.vertex(matrix4f, -1.0F, 1.0F, -0.5F).texture(4.0F + m, 0.0F + n).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
