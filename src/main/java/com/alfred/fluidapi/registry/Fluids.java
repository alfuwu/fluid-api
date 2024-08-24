package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Map;

public class Fluids {
    public static void register() {
		for (Map.Entry<Identifier, CustomFluid> entry : FluidBuilder.FLUIDS.entrySet()) {
            Registry.register(Registries.FLUID, entry.getKey(), entry.getValue().getStill());
            Registry.register(Registries.FLUID, entry.getKey().withPrefixedPath("flowing_"), entry.getValue().getFlowing());
        }
    }
}
