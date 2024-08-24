package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.FluidBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.Map;

public class FluidApiClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (Map.Entry<Identifier, CustomFluid> entry : FluidBuilder.FLUIDS.entrySet()) {
			BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), entry.getValue().getStill(), entry.getValue().getFlowing());

			FluidRenderHandlerRegistry.INSTANCE.register(entry.getValue().getStill(), entry.getValue().getFlowing(), new SimpleFluidRenderHandler(
					entry.getKey().withPrefixedPath("block/").withSuffixedPath("_still"),
					entry.getKey().withPrefixedPath("block/").withSuffixedPath("_flow"),
					entry.getKey().withPrefixedPath("block/").withSuffixedPath("_overlay"),
					entry.getValue().tintColor
			));
		}
	}
}