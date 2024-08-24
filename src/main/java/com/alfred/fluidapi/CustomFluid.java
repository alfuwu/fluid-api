package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.Blocks;
import com.alfred.fluidapi.registry.FluidBuilder;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.Optional;
import java.util.function.Function;

import static net.minecraft.item.Items.BUCKET;

// what is this monstrosity
public class CustomFluid extends FlowableFluid {
    private static final Item.Settings DEFAULT_SETTINGS = new FabricItemSettings().recipeRemainder(BUCKET).maxCount(1);
    protected FlowableFluid flowing;
    protected final CameraSubmersionType submersionType;
    protected final FogData fog;
    protected final Identifier submergedTexture;
    protected Item bucket;
    protected final FluidBuilder.BucketFactory<Item> bucketFactory;
    protected final boolean infinite;
    protected final int flowSpeed;
    protected final int levelDecreasePerBlock;
    protected final int tickRate;
    protected final float blastResistance;
    protected final int tintColor = 0xFFFFFF;

    protected CustomFluid(FluidBuilder.BucketFactory<Item> bucketFactory, CameraSubmersionType submersionType, FogData fog) {
        this.bucket = null;
        this.bucketFactory = bucketFactory;
        this.submersionType = submersionType;
        this.fog = fog;
        // make fluid settings class allow customization for this
        this.submergedTexture = FluidApi.identifier("textures/misc/in_fluid.png");
        this.infinite = true;
        this.flowSpeed = 4;
        this.levelDecreasePerBlock = 1;
        this.tickRate = 15;
        this.blastResistance = 100f;
        this.flowing = null;
    }

    public CustomFluid(FluidBuilder.BucketFactory<Item> bucketFactory, CameraSubmersionType submersionType, FogData fog, FluidBuilder.FluidFactory<?> flowingFactory) {
        this(bucketFactory, submersionType, fog);
        this.flowing = (FlowableFluid) flowingFactory.create(bucketFactory, this, submersionType, fog);
    }

    public FlowableFluid getStill() {
        return this;
    }

    // suspicious cast
    public FlowableFluid getFlowing() {
        return (FlowableFluid) this.flowing.getFlowing();
    }
    
    @Override
    protected boolean isInfinite(World world) {
        return this.infinite;
    }

    @Override
    public boolean matchesType(Fluid fluid) {
        return fluid == this.getStill() || fluid == this.getFlowing();
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
        Block.dropStacks(state, world, pos, blockEntity);
    }

    @Override
    protected int getFlowSpeed(WorldView world) {
        return this.flowSpeed;
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return this.levelDecreasePerBlock;
    }

    @Override
    public Item getBucketItem() {
        return this.bucket;
    }

    public void setBucketItem(Item bucket) {
        this.bucket = bucket;
    }

    public FluidBuilder.BucketFactory<Item> getBucketFactory() {
        return this.bucketFactory;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.isIn(FluidTags.WATER);
    }

    @Override
    public int getTickRate(WorldView world) {
        return this.tickRate;
    }

    @Override
    protected float getBlastResistance() {
        return this.blastResistance;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return Blocks.get(this).getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }

    @Override
    public Optional<SoundEvent> getBucketFillSound() {
        return Optional.of(SoundEvents.ITEM_BUCKET_FILL);
    }

    public int getLevel(FluidState state) {
        return 8;
    }

    public boolean isStill(FluidState state) {
        return true;
    }

    public CameraSubmersionType getSubmersionType() {
        return this.submersionType;
    }

    public Identifier getSubmergedTexture() {
        return this.submergedTexture;
    }

    public float getViewDistance(PlayerEntity player, int underwaterVisibilityTicks) {
        if (underwaterVisibilityTicks >= 600) {
            return 1.0f;
        } else {
            float h = MathHelper.clamp(underwaterVisibilityTicks / 100.0f, 0.0f, 1.0f);
            float i = underwaterVisibilityTicks < 100.0f
                ? 0.0f
                : MathHelper.clamp((underwaterVisibilityTicks - 100.0f) / 500.0f, 0.0f, 1.0f);
            return h * 0.6f + i * 0.4f;
        }
    }

    public FogData getFog() {
        return this.fog;
    }

    public static class FogData {
        protected Function<Float, Pair<Float, Float>> spectatorFog = viewDistance -> Pair.of(-8.0f, viewDistance * 0.5f);
        protected Function<Float, Pair<Float, Float>> nightVisionFog = viewDistance -> Pair.of(0.0f, 3.0f);
        protected Function<Float, Pair<Float, Float>> defaultFog = viewDistance -> Pair.of(0.25f, 1.0f);
        public float r, g, b;

        public FogData(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Pair<Float, Float> getSpectatorFog(float f) {
            return this.spectatorFog.apply(f);
        }

        public Pair<Float, Float> getNightVisionFog(float f) {
            return this.nightVisionFog.apply(f);
        }

        public Pair<Float, Float> getFog(float f) {
            return this.defaultFog.apply(f);
        }

        public FogData setSpectatorFog(Function<Float, Pair<Float, Float>> func) {
            this.spectatorFog = func;
            return this;
        }

        public FogData setNightVisionFog(Function<Float, Pair<Float, Float>> func) {
            this.nightVisionFog = func;
            return this;
        }

        public FogData setFog(Function<Float, Pair<Float, Float>> func) {
            this.defaultFog = func;
            return this;
        }
    }

    public static class Flowing extends CustomFluid {
        protected CustomFluid parent;

        public Flowing(FluidBuilder.BucketFactory<Item> bucketFactory, CustomFluid parent, CameraSubmersionType submersionType, FogData fog) {
            super(bucketFactory, submersionType, fog);
            this.parent = parent;
        }

        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean matchesType(Fluid fluid) {
            return this.parent.matchesType(fluid);
        }

        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        public boolean isStill(FluidState state) {
            return false;
        }

        public FlowableFluid getFlowing() {
            return this;
        }
    }
}