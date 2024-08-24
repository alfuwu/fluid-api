package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class Blocks {
	private static final AbstractBlock.Settings DEFAULT_SETTINGS = AbstractBlock.Settings.create().mapColor(MapColor.BLUE).replaceable().noCollision().strength(100.0F).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY);
	private static final Map<FlowableFluid, FluidBlock> FLUIDS = new HashMap<>();

	public static FluidBlock get(FlowableFluid fluid) {
		return FLUIDS.get(fluid);
	}

	public static void register() {
		Fluids.register();
		for (Map.Entry<Identifier, CustomFluid> entry : FluidBuilder.FLUIDS.entrySet()) {
			FluidBlock fluid = new FluidBlock(entry.getValue(), DEFAULT_SETTINGS);
			FLUIDS.put(entry.getValue(), Registry.register(Registries.BLOCK, entry.getKey(), fluid));
			FLUIDS.put(entry.getValue().getFlowing(), fluid);
		}
	}
}