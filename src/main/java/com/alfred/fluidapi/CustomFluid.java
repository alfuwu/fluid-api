package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.FluidBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

// what is this monstrosity
public class CustomFluid extends FlowableFluid {
    protected FlowableFluid flowing;
    protected final CameraSubmersionType submersionType;
    protected final FogData fog;
    protected final Identifier submergedTexture;
    protected Item bucket;
    protected Item bottle;
    protected final Function<World, Boolean> infinite; // broken when true (??? visible confusion)
    protected final Function<WorldView, Integer> flowSpeed;
    protected final Function<WorldView, Integer> levelDecreasePerBlock;
    protected final Function<WorldView, Integer> tickRate;
    protected final float blastResistance;
    protected final int tintColor;
    protected final TagKey<Fluid> tag;
    protected final Vec3d velocityMultiplier;
    protected final boolean drippable;
    protected SoundEvent entitySwimSound, entitySplashSound, entityHighSpeedSplashSound;
    protected SoundEvent playerSwimSound, playerSplashSound, playerHighSpeedSplashSound;
    protected float splashSoundVolume, swimSoundVolume;
    protected final boolean breathable;
    protected final Consumer<Entity> entityTick;
    protected final FluidBuilder.CombinesWithPredicate combinesWith;
    protected final FluidBuilder.CombinesWithFlowingPredicate combinesWithFlowing;

    // nvm the arguments have been unargumentified
    protected CustomFluid(CameraSubmersionType submersionType, FogData fog, FluidBuilder.Settings settings) {
        this.bucket = null;
        this.bottle = null;
        this.submersionType = submersionType;
        this.fog = fog;

        this.submergedTexture = settings.getSubmergedTexture();
        this.infinite = settings.isInfinite();
        this.flowSpeed = settings.getFlowSpeed();
        this.levelDecreasePerBlock = settings.getLevelDecreasePerBlock();
        this.tickRate = settings.getTickRate();
        this.blastResistance = settings.getBlastResistance();
        this.tintColor = settings.getTintColor();
        this.tag = settings.getTag();
        this.velocityMultiplier = settings.getVelocityMultiplier();
        this.drippable = settings.isDrippable();
        this.entitySwimSound = settings.getSwimSound();
        this.entitySplashSound = settings.getSplashSound();
        this.entityHighSpeedSplashSound = settings.getHighSpeedSplashSound();
        this.playerSwimSound = settings.getPlayerSwimSound();
        this.playerSplashSound = settings.getPlayerSplashSound();
        this.playerHighSpeedSplashSound = settings.getPlayerHighSpeedSplashSound();
        this.splashSoundVolume = settings.getSplashSoundVolume();
        this.swimSoundVolume = settings.getSwimSoundVolume();
        this.breathable = settings.isBreathable();
        this.entityTick = settings.getEntityTick();
        this.combinesWith = settings.getCombinesWith();
        this.combinesWithFlowing = settings.getCombinesWithFlowing();
        this.flowing = null;
    }

    public CustomFluid(CameraSubmersionType submersionType, FogData fog, FluidBuilder.Settings settings, FluidBuilder.FluidFactory<?> flowingFactory) {
        this(submersionType, fog, settings);
        this.flowing = (FlowableFluid) flowingFactory.create(this, settings);
    }

    public FlowableFluid getStill() {
        return this;
    }

    public FlowableFluid getFlowing() {
        return (FlowableFluid) this.flowing.getFlowing();
    }
    
    @Override
    protected boolean isInfinite(World world) {
        return this.infinite.apply(world);
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
        return this.flowSpeed.apply(world);
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return this.levelDecreasePerBlock.apply(world);
    }

    @Override
    public Item getBucketItem() {
        return this.bucket;
    }

    public void setBucketItem(Item bucket) {
        this.bucket = bucket;
    }

    public Item getBottleItem() {
        return this.bottle;
    }

    public void setBottleItem(Item bottle) {
        this.bottle = bottle;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN;
    }

    @Override
    public int getTickRate(WorldView world) {
        return this.tickRate.apply(world);
    }

    @Override
    protected float getBlastResistance() {
        return this.blastResistance;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return FluidBuilder.FLUID_BLOCKS.get(this).getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }

    @Override
    public Optional<SoundEvent> getBucketFillSound() {
        return Optional.of(SoundEvents.ITEM_BUCKET_FILL);
    }

    @Override
    protected void flow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
        if (direction == Direction.DOWN) {
            FluidState fluidState1 = world.getFluidState(pos);
            BlockState state1 = this.combinesWithFlowing.test(this, fluidState1.getFluid(), world, pos);
            System.out.println(fluidState1.getFluid());
            if (state1 != null) {
                if (state.getBlock() instanceof FluidBlock)
                    world.setBlockState(pos, state1, 3);

                return;
            }
        }

        super.flow(world, pos, state, direction, fluidState);
    }

    public int getLevel(FluidState state) {
        return 8;
    }

    public boolean isStill(FluidState state) {
        return true;
    }

    public int getTintColor() {
        return this.tintColor;
    }

    @Override
    public boolean isIn(TagKey<Fluid> tag) {
        return super.isIn(tag) || tag.equals(this.getTag());
    }

    @Nullable
    public TagKey<Fluid> getTag() {
        return this.tag;
    }

    public Vec3d getVelocityMultiplier() {
        return this.velocityMultiplier;
    }

    public boolean isDrippable() {
        return this.drippable;
    }

    public boolean isBreathable() {
        return this.breathable;
    }

    public SoundEvent getSwimSound() {
        return this.entitySwimSound;
    }

    public SoundEvent getSplashSound() {
        return this.entitySplashSound;
    }

    public SoundEvent getHighSpeedSplashSound() {
        return this.entityHighSpeedSplashSound;
    }

    public SoundEvent getPlayerSwimSound() {
        return this.playerSwimSound;
    }

    public SoundEvent getPlayerSplashSound() {
        return this.playerSplashSound;
    }

    public SoundEvent getPlayerHighSpeedSplashSound() {
        return this.playerHighSpeedSplashSound;
    }

    public float getSplashSoundVolume() {
        return this.splashSoundVolume;
    }

    public float getSwimSoundVolume() {
        return this.swimSoundVolume;
    }

    public Consumer<Entity> getEntityTick() {
        return this.entityTick;
    }

    public FluidBuilder.CombinesWithPredicate getCombinesWith() {
        return this.combinesWith;
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
        public float r, g, b;
        public Pair<Float, Float> viewDistance;
        protected FogFactory factory;

        public FogData(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.viewDistance = Pair.of(-8.0f, 96.0f);
            this.factory = (fog, entity, viewDistance, underwaterVisibility) -> {
                float fogStart = fog.viewDistance.getFirst();
                float fogEnd = fog.viewDistance.getSecond();
                if (entity instanceof PlayerEntity player) {
                    fogEnd *= Math.max(0.25f, underwaterVisibility);
                    RegistryEntry<Biome> registryEntry = player.getWorld().getBiome(player.getBlockPos());
                    if (registryEntry.isIn(BiomeTags.HAS_CLOSER_WATER_FOG))
                        fogEnd *= 0.85f;
                }
                return Pair.of(fogStart, fogEnd);
            };
        }

        public Pair<Float, Float> getFog(Entity entity, float viewDistance, float underwaterVisibility) {
            return this.factory.getFog(this, entity, viewDistance, underwaterVisibility);
        }

        public FogData setFog(FogFactory factory) {
            this.factory = factory;
            return this;
        }

        public interface FogFactory {
            Pair<Float, Float> getFog(FogData fog, Entity entity, float viewDistance, float underwaterVisibility);
        }
    }

    public static class Flowing extends CustomFluid {
        protected CustomFluid parent;

        public Flowing(CustomFluid parent, FluidBuilder.Settings settings) {
            super(parent.submersionType, parent.fog, settings);
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