package com.alfred.fluidapi;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class FullCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<FullCauldronBlock> CODEC = createCodec(settings -> new FullCauldronBlock(settings, entity -> {}, false));

    protected boolean burnsEntities;
    protected Consumer<Entity> entityTick;

    public MapCodec<FullCauldronBlock> getCodec() {
        return CODEC;
    }

    public FullCauldronBlock(AbstractBlock.Settings settings, Consumer<Entity> entityTick, boolean burnsEntities) {
        super(settings, null);
        this.entityTick = entityTick;
        this.burnsEntities = burnsEntities;
    }

    protected double getFluidHeight(BlockState state) {
        return 0.9375;
    }

    public boolean isFull(BlockState state) {
        return true;
    }

    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (this.isEntityTouchingFluid(state, pos, entity)) {
            if (this.burnsEntities)
                entity.setOnFireFromLava();
            this.entityTick.accept(entity);
        }
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return 3;
    }
}
