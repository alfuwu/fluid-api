package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.FullCauldronBlock;
import com.alfred.fluidapi.LeveledCauldronBlock;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.item.Items.BUCKET;
import static net.minecraft.item.Items.GLASS_BOTTLE;

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
	}

	// i could just call Items.register() from within Blocks.register()
	// but that's a little unintuitive
	public static void registerCauldrons() {
		for (Pair<Identifier, Pair<Boolean, Boolean>> cauldron : FluidBuilder.CAULDRONS) {
			boolean bl = cauldron.getSecond().getFirst(); // ideally switch to a factory-based cauldron entity collision function for maximum customization
			boolean bl2 = cauldron.getSecond().getSecond();
			Identifier id = cauldron.getFirst();
			AbstractCauldronBlock block;
			CustomFluid fluid = FluidBuilder.FLUIDS.getOrDefault(id, null);
			if (bl2)
				block = new LeveledCauldronBlock(FluidBuilder.CAULDRON_SETTINGS.get(id), FluidBuilder.FLUIDS.get(id), bl);
			else
				block = new FullCauldronBlock(FluidBuilder.CAULDRON_SETTINGS.get(id), bl);
			Registry.register(Registries.BLOCK, id.withSuffixedPath("_cauldron"), block);
			if (fluid != null && fluid.getTintColor() > -1 && fluid.getTintColor() < 0xFFFFFF)
				TINTED_BLOCKS.add(Pair.of(block, fluid.getTintColor()));
			CAULDRONS.put(id, block);
		}
		FluidBuilder.CAULDRON_SETTINGS.clear();

		for (Map.Entry<Identifier, AbstractCauldronBlock> entry : CAULDRONS.entrySet()) {
			CauldronBehavior.CauldronBehaviorMap behaviorMap = CauldronBehavior.createMap(entry.getKey().getPath());
			Map<Item, CauldronBehavior> map = behaviorMap.map();
			CauldronBehavior.registerBucketBehavior(map);
			CustomFluid fluid = FluidBuilder.FLUIDS.get(entry.getKey());
			if (fluid.getBucketItem() != null)
				map.put(BUCKET, (state, world, pos, player, hand, stack) ->
						CauldronBehavior.emptyCauldron(state, world, pos, player, hand, stack, new ItemStack(fluid.getBucketItem()), (statex) ->
								statex.getBlock() instanceof FullCauldronBlock || statex.get(LeveledCauldronBlock.LEVEL) == 3, fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
				);
			if (fluid.getBottleItem() != null) {
				Item bottle = fluid.getBottleItem();
				map.put(GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
					if (!world.isClient) {
						Item item = stack.getItem();
						player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(bottle)));
						player.incrementStat(Stats.USE_CAULDRON);
						player.incrementStat(Stats.USED.getOrCreateStat(item));
						LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
						world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
						world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
					}

					return ActionResult.success(world.isClient);
				});
				map.put(bottle, (state, world, pos, player, hand, stack) -> {
					if (state.get(LeveledCauldronBlock.LEVEL) != 3) {
						if (!world.isClient) {
							player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(GLASS_BOTTLE)));
							player.incrementStat(Stats.USE_CAULDRON);
							player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
							world.setBlockState(pos, state.cycle(LeveledCauldronBlock.LEVEL));
							world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
							world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
						}

						return ActionResult.success(world.isClient);
					} else {
						return ActionResult.PASS;
					}
				});

				Map<Item, CauldronBehavior> map1 = CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.map();
				map1.put(bottle, (state, world, pos, player, hand, stack) -> {
					if (!world.isClient) {
						player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(GLASS_BOTTLE)));
						player.incrementStat(Stats.USE_CAULDRON);
						player.incrementStat(Stats.USED.getOrCreateStat(bottle));
						world.setBlockState(pos, entry.getValue().getDefaultState());
						world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
						world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
					}

					return ActionResult.success(world.isClient);
				});
			}

			entry.getValue().behaviorMap = behaviorMap;
		}

		for (Map.Entry<String, CauldronBehavior.CauldronBehaviorMap> entry : CauldronBehavior.BEHAVIOR_MAPS.entrySet()) {
			for (Map.Entry<Identifier, CustomFluid> entry1 : FluidBuilder.FLUIDS.entrySet()) {
				if (CAULDRONS.containsKey(entry1.getKey())) {
					CustomFluid fluid = entry1.getValue();
					AbstractCauldronBlock cauldron = CAULDRONS.get(entry1.getKey());
					if (fluid.getBucketItem() != null)
						entry.getValue().map().put(fluid.getBucketItem(), (state, world, pos, player, hand, stack) ->
								CauldronBehavior.fillCauldron(world, pos, player, hand, stack, cauldron instanceof LeveledCauldronBlock ?
										cauldron.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3) :
										cauldron.getDefaultState(),
										fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
						);
				}
			}
		}
		FluidBuilder.CAULDRONS.clear();
	}
}