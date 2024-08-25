package com.alfred.fluidapi;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;

public class LeveledCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<LeveledCauldronBlock> CODEC = createCodec(settings -> new LeveledCauldronBlock(settings, null, false));

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 3;
    public static final IntProperty LEVEL = Properties.LEVEL_3;
    private static final int BASE_FLUID_HEIGHT = 6;
    private static final double FLUID_HEIGHT_PER_LEVEL = 3.0;
    // Add custom precipitation?
    //private final Biome.Precipitation precipitation;
    protected final CustomFluid fluid;
    protected boolean burnsEntities;

    public MapCodec<LeveledCauldronBlock> getCodec() {
        return CODEC;
    }

    public LeveledCauldronBlock(AbstractBlock.Settings settings, CustomFluid fluid, boolean burnsEntities) {
        super(settings, null);
        //this.precipitation = precipitation;
        this.fluid = fluid;
        this.burnsEntities = burnsEntities;
        this.setDefaultState(this.stateManager.getDefaultState().with(LEVEL, MIN_LEVEL));
    }

    public boolean isFull(BlockState state) {
        return state.get(LEVEL) == MAX_LEVEL;
    }

    protected boolean canBeFilledByDripstone(Fluid fluid) {
        return fluid == this.fluid;// && this.precipitation == Biome.Precipitation.RAIN;
    }

    protected double getFluidHeight(BlockState state) {
        return (BASE_FLUID_HEIGHT + (double) state.get(LEVEL) * FLUID_HEIGHT_PER_LEVEL) / 16.0;
    }

    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        //if (!world.isClient && entity.isOnFire() && this.isEntityTouchingFluid(state, pos, entity)) {
        //    entity.extinguish();
        //    if (entity.canModifyAt(world, pos))
        //        this.onFireCollision(state, world, pos);
        //}
        // implement customization of what happens when an entity enters the cauldron?
        if (this.burnsEntities && this.isEntityTouchingFluid(state, pos, entity) && state.get(LEVEL) >= MIN_LEVEL)
            entity.setOnFireFromLava();
    }

    private void onFireCollision(BlockState state, World world, BlockPos pos) {
        //decrementFluidLevel(state, world, pos);
    }

    public static void decrementFluidLevel(BlockState state, World world, BlockPos pos) {
        int i = state.get(LEVEL) - 1;
        BlockState blockState = i == 0 ? Blocks.CAULDRON.getDefaultState() : state.with(LEVEL, i);
        world.setBlockState(pos, blockState);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
    }

    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        //if (CauldronBlock.canFillWithPrecipitation(world, precipitation) && state.get(LEVEL) != 3 && precipitation == this.precipitation) {
        //    BlockState blockState = state.cycle(LEVEL);
        //    world.setBlockState(pos, blockState);
        //    world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
        //}
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(LEVEL);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
        if (!this.isFull(state)) {
            BlockState blockState = state.with(LEVEL, state.get(LEVEL) + 1);
            world.setBlockState(pos, blockState);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
            world.syncWorldEvent(WorldEvents.POINTED_DRIPSTONE_DRIPS_WATER_INTO_CAULDRON, pos, 0);
        }
    }
}