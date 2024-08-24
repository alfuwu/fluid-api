package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.util.Identifier;

public class FluidApi implements ModInitializer {
	public static CustomFluid fluid = FluidBuilder.create(FluidApi.identifier("custom_fluid"))
			.fogColor(0xFF5533)
			.submersionType(CameraSubmersionType.LAVA)
			.build();

	@Override
	public void onInitialize() {
		Blocks.register();
		Items.register();
		// god this is so scuffed
	}
	
	public static Identifier identifier(String path) {
		return new Identifier("fluid-api", path);
	}
}