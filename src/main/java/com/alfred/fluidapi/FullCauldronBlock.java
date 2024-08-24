package com.alfred.fluidapi;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FullCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<FullCauldronBlock> CODEC = createCodec(settings -> new FullCauldronBlock(settings, false));

    public MapCodec<FullCauldronBlock> getCodec() {
        return CODEC;
    }
    protected boolean burnsEntities;

    public FullCauldronBlock(AbstractBlock.Settings settings, boolean burnsEntities) {
        super(settings, null);
        this.burnsEntities = burnsEntities;
    }

    protected double getFluidHeight(BlockState state) {
        return 0.9375;
    }

    public boolean isFull(BlockState state) {
        return true;
    }

    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (this.burnsEntities && this.isEntityTouchingFluid(state, pos, entity))
            entity.setOnFireFromLava();
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return 3;
    }
}
