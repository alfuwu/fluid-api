package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.FullCauldronBlock;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.block.Blocks.CAULDRON;

public class Blocks {
	private static final Map<FlowableFluid, FluidBlock> FLUIDS = new HashMap<>();
	public static final List<Pair<Block, Integer>> TINTED_BLOCKS = new ArrayList<>();
	public static final Map<Identifier, AbstractCauldronBlock> CAULDRONS = new HashMap<>();

	public static FluidBlock get(FlowableFluid fluid) {
		return FLUIDS.get(fluid);
	}

	public static void register() {
		Fluids.register();
		for (Map.Entry<Identifier, CustomFluid> entry : FluidBuilder.FLUIDS.entrySet()) {
			FluidBlock fluid = new FluidBlock(entry.getValue(), FluidBuilder.FLUID_SETTINGS.get(entry.getKey()));
			FLUIDS.put(entry.getValue(), Registry.register(Registries.BLOCK, entry.getKey(), fluid));
			FLUIDS.put(entry.getValue().getFlowing(), fluid);
		}
		FluidBuilder.FLUID_SETTINGS.clear();

		for (Pair<Identifier, Pair<Boolean, Boolean>> cauldron : FluidBuilder.CAULDRONS) {
			boolean bl = cauldron.getSecond().getFirst();
			boolean bl2 = cauldron.getSecond().getSecond();
			Identifier id = cauldron.getFirst();
			AbstractCauldronBlock block;
			CustomFluid fluid = FluidBuilder.FLUIDS.getOrDefault(id, null);
			if (bl2) {
				throw new NotImplementedException("haha bottles don't exist yet");
			} else {
				block = new FullCauldronBlock(FluidBuilder.CAULDRON_SETTINGS.get(id), bl);
				Registry.register(Registries.BLOCK, id.withSuffixedPath("_cauldron"), block);
			}
			if (fluid != null && fluid.getTintColor() > -1 && fluid.getTintColor() < 0xFFFFFF)
				TINTED_BLOCKS.add(Pair.of(block, fluid.getTintColor()));
			CAULDRONS.put(id, block);
		}
	}

	// i could just call Items.register() from within Blocks.register()
	// but that's a little unintuitive
	public static void registerCauldrons() {
		for (Map.Entry<Identifier, AbstractCauldronBlock> entry : CAULDRONS.entrySet()) {
			CauldronBehavior.CauldronBehaviorMap map = CauldronBehavior.createMap(entry.getKey().getPath());
			CauldronBehavior.registerBucketBehavior(map.map());
			CustomFluid fluid = FluidBuilder.FLUIDS.get(entry.getKey());
			if (fluid.getBucketItem() != null)
				map.map().put(net.minecraft.item.Items.BUCKET, (state, world, pos, player, hand, stack) ->
						CauldronBehavior.emptyCauldron(state, world, pos, player, hand, stack, new ItemStack(fluid.getBucketItem()), (statex) ->
								statex.getBlock() instanceof FullCauldronBlock || statex.get(LeveledCauldronBlock.LEVEL) == 3, fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
				);
			entry.getValue().behaviorMap = map;
		}

		for (Map.Entry<String, CauldronBehavior.CauldronBehaviorMap> entry : CauldronBehavior.BEHAVIOR_MAPS.entrySet()) {
			for (Map.Entry<Identifier, CustomFluid> entry1 : FluidBuilder.FLUIDS.entrySet()) {
				if (CAULDRONS.containsKey(entry1.getKey())) {
					CustomFluid fluid = entry1.getValue();
					if (fluid.getBucketItem() != null)
						entry.getValue().map().put(fluid.getBucketItem(), (state, world, pos, player, hand, stack) ->
								CauldronBehavior.fillCauldron(world, pos, player, hand, stack, CAULDRONS.get(entry1.getKey()).getDefaultState(), fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
						);
				}
			}
		}
	}
}