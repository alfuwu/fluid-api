package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.alfred.fluidapi.FullCauldronBlock;
import com.alfred.fluidapi.LeveledCauldronBlock;
import com.alfred.fluidapi.bottles.FluidLingeringPotionItem;
import com.alfred.fluidapi.bottles.FluidPotionItem;
import com.alfred.fluidapi.bottles.FluidSplashPotionItem;
import com.mojang.datafixers.util.Pair;
import io.netty.util.internal.UnstableApi;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.block.Blocks.CAULDRON;
import static net.minecraft.item.Items.*;

@SuppressWarnings("unused")
public class FluidBuilder {
    private static final AbstractBlock.Settings DEFAULT_FLUID_SETTINGS = FabricBlockSettings.create().replaceable().noCollision().strength(100.0f).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY);
    private static final Item.Settings DEFAULT_BUCKET_SETTINGS = new FabricItemSettings().recipeRemainder(BUCKET).maxCount(1);
    private static final AbstractBlock.Settings DEFAULT_CAULDRON_SETTINGS = FabricBlockSettings.copyShallow(CAULDRON);
    private static final Item.Settings DEFAULT_BOTTLE_SETTINGS = new FabricItemSettings().recipeRemainder(GLASS_BOTTLE).maxCount(1);
    public static final Map<Identifier, CustomFluid> FLUIDS = new HashMap<>();

    // yes
    public static final List<Pair<Block, Integer>> TINTED_BLOCKS = new ArrayList<>();
    public static final Map<Identifier, AbstractCauldronBlock> CAULDRONS = new HashMap<>();
    public static final Map<FlowableFluid, FluidBlock> FLUID_BLOCKS = new HashMap<>();

    protected final Identifier id;
    protected CustomFluid.FogData fog;
    protected CameraSubmersionType submersionType;
    protected RegistryKey<ItemGroup> bucketItemGroup;
    protected Item bucketAfter;
    protected CustomFluidFactory<?> factory;
    protected FluidFactory<?> flowingFactory;
    protected BucketFactory<Item> bucketFactory;
    protected Item bottleItem;
    protected BottleFactory<Item> bottleFactory;
    protected BottleFactory<Item> splashBottleFactory;
    protected BottleFactory<Item> lingeringBottleFactory;
    protected Settings settings;
    protected AbstractBlock.Settings fluidSettings;
    protected Item.Settings bucketSettings;
    protected AbstractBlock.Settings cauldronSettings;
    protected Item.Settings bottleSettings;
    protected Text bottleTooltip;
    protected List<StatusEffectInstance> bottleStatusEffects;
    protected boolean createBucket;
    protected boolean createCauldron;
    protected boolean cauldronBurns;
    protected boolean createBottle;

    private FluidBuilder(Identifier id) {
        this.id = id;
    }

    public static FluidBuilder create(Identifier identifier) {
        FluidBuilder builder = new FluidBuilder(identifier);
        builder.fog = new CustomFluid.FogData(0.3f, 0.3f, 0.3f);
        builder.submersionType = CameraSubmersionType.WATER;
        builder.bucketItemGroup = ItemGroups.TOOLS;
        builder.bucketAfter = Items.MILK_BUCKET;
        builder.bucketFactory = BucketItem::new;
        builder.bottleItem = null;
        builder.bottleFactory = FluidPotionItem::new;
        builder.splashBottleFactory = FluidSplashPotionItem::new;
        builder.lingeringBottleFactory = FluidLingeringPotionItem::new;
        builder.factory = CustomFluid::new;
        builder.flowingFactory = CustomFluid.Flowing::new;
        builder.settings = new FluidBuilder.Settings();
        builder.fluidSettings = DEFAULT_FLUID_SETTINGS;
        builder.bucketSettings = DEFAULT_BUCKET_SETTINGS;
        builder.cauldronSettings = DEFAULT_CAULDRON_SETTINGS;
        builder.bottleSettings = DEFAULT_BOTTLE_SETTINGS;
        builder.bottleTooltip = null;
        builder.bottleStatusEffects = new ArrayList<>();
        builder.createBucket = true;
        builder.createCauldron = true;
        builder.cauldronBurns = false;
        builder.createBottle = false;
        return builder;
    }

    /**
     * @param fog The fog data for the custom fluid
     * @return this
     */
    public FluidBuilder fog(CustomFluid.FogData fog) {
        this.fog = fog;
        return this;
    }

    /**
     * @param r The intensity of the color red
     * @param g The intensity of the color green
     * @param b The intensity of the color blue
     * @return this
     */
    public FluidBuilder fogColor(float r, float g, float b) {
        this.fog.r = r;
        this.fog.g = g;
        this.fog.b = b;
        return this;
    }

    /**
     * @param color A six digit hex value that will be interpreted as a color and used to tint the submerged overlay
     * @return this
     */
    public FluidBuilder fogColor(int color) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return this.fogColor(r / 255.0f, g / 255.0f, b / 255.0f);
    }

    /**
     * @param fogStartEnd A pair of float containing the fog's start and the fog's end value
     * @return this
     */
    public FluidBuilder fogViewDistance(Pair<Float, Float> fogStartEnd) {
        this.fog.viewDistance = fogStartEnd;
        return this;
    }

    /**
     * @param factory A function that takes in a FogData instance, an Entity, and two floats (view distance and underwater visibility) and outputs a fog start and fog end value
     * @return this
     */
    public FluidBuilder fogFactory(CustomFluid.FogData.FogFactory factory) {
        this.fog.setFog(factory);
        return this;
    }

    /**
     * Makes the custom fluid's fog function identical to that of lava
     * @return this
     */
    public FluidBuilder lavaFog() {
        this.fog.setFog(((fog, entity, viewDistance, underwaterVisibility) -> {
            if (entity.isSpectator())
                return Pair.of(-8.0f, viewDistance * 0.5f);
            else if (entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
                return Pair.of(0.0f, 3.0f);
            else
                return Pair.of(0.25f, 1.0f);
        }));
        return this;
    }


    /**
     * Makes the custom fluid's fog function identical to that of powdered snow
     * @return this
     */
    public FluidBuilder powderedSnowFog() {
        this.fog.setFog(((fog1, entity, viewDistance, underwaterVisibility) -> {
            if (entity.isSpectator())
                return Pair.of(-8.0f, viewDistance * 0.5f);
            else
                return Pair.of(0.0f, 2.0f);
        }));
        return this;
    }

    /**
     * @param type The camera submersion method that will be used when the player is submerged in the liquid
     * @return this
     */
    public FluidBuilder submersionType(CameraSubmersionType type) {
        this.submersionType = type;
        return this;
    } // crash: changing submersionType to CameraSubmersionType.POWDERED_SNOW causes the game to crash during initialization

    /**
     * @param group The creative inventory tab where the fluid's custom bucket item will appear in
     * @return this
     */
    public FluidBuilder itemGroup(@Nullable RegistryKey<ItemGroup> group) {
        this.bucketItemGroup = group;
        return this;
    }

    /**
     * @param after The item after which the fluid's custom bucket will appear after in the creative inventory
     * @return this
     */
    public FluidBuilder bucketAfter(@Nullable Item after) {
        this.bucketAfter = after;
        return this;
    }

    /**
     * @param factory The factory that will be used to instantiate the CustomFluid during building
     * @return this
     */
    public FluidBuilder customFluid(CustomFluidFactory<?> factory) {
        this.factory  = factory;
        return this;
    }

    /**
     * @param factory The factory that will be used to instantiate the flowing version of the custom fluid
     * @return this
     */
    public FluidBuilder customFlowingFluid(FluidFactory<?> factory) {
        this.flowingFactory = factory;
        return this;
    }

    /**
     * @param factory The factory that will be used to create the fluid's custom bucket item
     * @return this
     */
    public FluidBuilder customBucketItem(BucketFactory<Item> factory) {
        this.bucketFactory = factory;
        return this;
    }


    /**
     * @param factory The factory that will be used to create the fluid's custom bottle item
     * @return this
     */
    public FluidBuilder customBottleItem(@Nullable BottleFactory<Item> factory) {
        this.bottleFactory = factory;
        return this;
    }


    /**
     * @param factory The factory that will be used to create the fluid's custom splash bottle item
     * @return this
     */
    public FluidBuilder customSplashBottleItem(@Nullable BottleFactory<Item> factory) {
        this.splashBottleFactory = factory;
        return this;
    }


    /**
     * @param factory The factory that will be used to create the fluid's custom lingering bottle item
     * @return this
     */
    public FluidBuilder customLingeringBottleItem(@Nullable BottleFactory<Item> factory) {
        this.lingeringBottleFactory = factory;
        return this;
    }

    public FluidBuilder customExistingBottleItem(Item item) {
        this.bottleItem = item;
        return this;
    }

    /**
     * Prevents the API from automatically creating a bucket for the custom fluid
     * @return this
     */
    public FluidBuilder noBucket() {
        this.createBucket = false;
        return this;
    }

    /**
     * Prevents the API from automatically creating a cauldron variant for the custom fluid
     * @return this
     */
    public FluidBuilder noCauldron() {
        this.createCauldron = false;
        return this;
    }

    /**
     * Registers a bottle for the custom fluid<br>
     * Also makes the cauldron a LeveledCauldronBlock instead of a FullCauldronBlock
     * @return this
     */
    public FluidBuilder bottle() {
        this.createBottle = true;
        return this;
    }

    /**
     * @param settings The custom fluid's settings
     * @return this
     */
    public FluidBuilder settings(Settings settings) {
        this.settings = settings;
        return this;
    }

    /**
     * @param path The texture to show when submerged within the custom fluid
     * @return this
     */
    public FluidBuilder submergedTexture(Identifier path) {
        this.settings.submergedTexture(path);
        return this;
    }

    /**
     * Makes the liquid flow infinitely (like water)<br>
     * Or, at least, it <i>should</i>
     * @return this
     */
    @UnstableApi
    public FluidBuilder infinite() {
        this.settings.infinite();
        return this;
    }

    /**
     * @param infinite A function that provides a boolean when given a World object
     * @return this
     */
    @UnstableApi
    public FluidBuilder infinite(Function<World, Boolean> infinite) {
        this.settings.infinite(infinite);
        return this;
    }

    /**
     * @param flowSpeed How many ticks it takes for the liquid to flow one block
     * @return this
     */
    public FluidBuilder flowSpeed(int flowSpeed) {
        this.settings.flowSpeed(flowSpeed);
        return this;
    }

    /**
     * @param flowSpeed A function that provides an integer when given a WorldView object
     * @return this
     */
    public FluidBuilder flowSpeed(Function<WorldView, Integer> flowSpeed) {
        this.settings.flowSpeed(flowSpeed);
        return this;
    }

    /**
     * @param levelDecreasePerBlock The amount of levels the liquid decreases per block
     * @return this
     */
    public FluidBuilder levelDecreasePerBlock(int levelDecreasePerBlock) {
        this.settings.levelDecreasePerBlock(levelDecreasePerBlock);
        return this;
    }

    /**
     * @param levelDecreasePerBlock A function that provides an integer when given a WorldView object
     * @return this
     */
    public FluidBuilder levelDecreasePerBlock(Function<WorldView, Integer> levelDecreasePerBlock) {
        this.settings.levelDecreasePerBlock(levelDecreasePerBlock);
        return this;
    }

    /**
     * @param tickRate Determines how fast the fluid ticks
     * @return this
     */
    public FluidBuilder tickRate(int tickRate) {
        this.settings.tickRate(tickRate);
        return this;
    }

    /**
     * @param tickRate A function that provides an integer when given a WorldView object
     * @return this
     */
    public FluidBuilder tickRate(Function<WorldView, Integer> tickRate) {
        this.settings.tickRate(tickRate);
        return this;
    }

    /**
     * @param blastResistance The resistance the liquid provides against explosions
     * @return this
     */
    public FluidBuilder blastResistance(float blastResistance) {
        this.settings.blastResistance(blastResistance);
        return this;
    }

    /**
     * @param tintColor A six digit hex color that modifies the color of the liquid
     * @return this
     */
    public FluidBuilder tintColor(int tintColor) {
        this.settings.tintColor(tintColor);
        return this;
    }

    /**
     * @param tag The fluid tag that the custom fluid will automatically pass for
     * @return this
     */
    public FluidBuilder tag(@Nullable TagKey<Fluid> tag) {
        this.settings.tag(tag);
        if (FluidTags.LAVA.equals(tag))
            this.cauldronBurns = true;
        return this;
    }

    /**
     * Values greater than 1 not recommended
     * @param movementSpeed A Vec3d that multiplies any entity within the custom fluid's speed
     * @return this
     */
    public FluidBuilder velocityMultiplier(Vec3d movementSpeed) {
        this.settings.velocityMultiplier(movementSpeed);
        return this;
    }

    /**
     * Values greater than 1 not recommended
     * @param x The X velocity multiplier
     * @param y The Y velocity multiplier
     * @param z The Z velocity multiplier
     * @return this
     */
    public FluidBuilder velocityMultiplier(double x, double y, double z) {
        this.settings.velocityMultiplier(x, y, z);
        return this;
    }

    /**
     * Prevents dripstone from filling up cauldrons
     * @return this
     */
    public FluidBuilder notDrippable() {
        this.settings.notDrippable();
        return this;
    }

    public FluidBuilder swimSound(SoundEvent sound) {
        this.settings.swimSound(sound);
        return this;
    }

    public FluidBuilder playerSwimSound(SoundEvent sound) {
        this.settings.playerSwimSound(sound);
        return this;
    }

    public FluidBuilder splashSound(SoundEvent sound) {
        this.settings.splashSound(sound);
        return this;
    }

    public FluidBuilder playerSplashSound(SoundEvent sound) {
        this.settings.playerSplashSound(sound);
        return this;
    }

    public FluidBuilder highSpeedSplashSound(SoundEvent sound) {
        this.settings.highSpeedSplashSound(sound);
        return this;
    }

    public FluidBuilder playerHighSpeedSplashSound(SoundEvent sound) {
        this.settings.playerHighSpeedSplashSound(sound);
        return this;
    }

    public FluidBuilder splashSoundVolume(float volume) {
        this.settings.splashSoundVolume(volume);
        return this;
    }

    public FluidBuilder swimSoundVolume(float volume) {
        this.settings.swimSoundVolume(volume);
        return this;
    }

    public FluidBuilder breathable() {
        this.settings.breathable();
        return this;
    }

    public FluidBuilder entityTick(Consumer<Entity> func) {
        this.settings.entityTick(func);
        return this;
    }

    public FluidBuilder combinesWith(CombinesWithPredicate predicate) {
        this.settings.combinesWith(predicate);
        return this;
    }

    public FluidBuilder combinesWithBasic(Block lavaCombination, Block waterCombination, Block otherCombination) {
        this.settings.combinesWithBasic(lavaCombination, waterCombination, otherCombination);
        return this;
    }

    public FluidBuilder vanillaCombinesWith(boolean playExtinguishSound) {
        this.settings.vanillaCombinesWith(playExtinguishSound);
        return this;
    }

    public FluidBuilder combinesWithFlowing(CombinesWithFlowingPredicate predicate) {
        this.settings.combinesWithFlowing(predicate);
        return this;
    }

    public FluidBuilder combinesWithFlowingBasic(Block lavaCombination, Block waterCombination, Block otherCombination) {
        this.settings.combinesWithFlowingBasic(lavaCombination, waterCombination, otherCombination);
        return this;
    }

    public FluidBuilder vanillaCombinesWithFlowing(boolean playExtinguishSound) {
        this.settings.vanillaCombinesWithFlowing(playExtinguishSound);
        return this;
    }

    /**
     * @param settings The block settings for the custom fluid
     * @return this
     */
    public FluidBuilder blockSettings(AbstractBlock.Settings settings) {
        this.fluidSettings = settings.replaceable().noCollision().strength(100f).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY);
        return this;
    }

    /**
     * Note: does not apply default fluid block settings. Have fun :)
     * @param settings The block settings for the custom fluid
     * @return this
     */
    public FluidBuilder blockSettingsNoDefaults(AbstractBlock.Settings settings) {
        this.fluidSettings = settings;
        return this;
    }

    /**
     * @param settings The item settings that will be used when registering the bucket item
     * @return this
     */
    public FluidBuilder bucketSettings(Item.Settings settings) {
        this.bucketSettings = settings;
        return this;
    }

    /**
     * @param settings The cauldron settings that will be used when registering the cauldron block
     * @return this
     */
    public FluidBuilder cauldronSettings(AbstractBlock.Settings settings) {
        this.cauldronSettings = settings;
        return this;
    }

    /**
     * @param settings The item settings that will be used when registering the bottle item
     * @return this
     */
    public FluidBuilder bottleSettings(Item.Settings settings) {
        this.bottleSettings = settings;
        return this;
    }

    public FluidBuilder bottleTooltip(Text tooltip) {
        this.bottleTooltip = tooltip;
        return this;
    }

    public FluidBuilder bottleStatusEffects(StatusEffectInstance... statusEffects) {
        this.bottleStatusEffects = Arrays.stream(statusEffects).toList();
        return this;
    }

    /**
     * Builds the custom fluid and returns it
     * @return CustomFluid
     */
    public CustomFluid build() {
        if (FLUIDS.containsKey(this.id))
            throw new RuntimeException("ID already registered: " + this.id);
        CustomFluid fluid = this.factory.create(this.submersionType, this.fog, this.settings, this.flowingFactory);

        FLUIDS.put(this.id, fluid);

        // registration
        Registry.register(Registries.FLUID, this.id, fluid.getStill());
        Registry.register(Registries.FLUID, this.id.withPrefixedPath("flowing_"), fluid.getFlowing());
        FluidBlock fluidBlock = Registry.register(Registries.BLOCK, this.id, new FluidBlock(fluid.getStill(), this.fluidSettings));
        FLUID_BLOCKS.put(fluid.getStill(), fluidBlock);
        FLUID_BLOCKS.put(fluid.getFlowing(), fluidBlock);

        if (this.createBucket) {
            Item bucket = Registry.register(Registries.ITEM, this.id.withSuffixedPath("_bucket"), this.bucketFactory.create(fluid, this.bucketSettings));
            fluid.setBucketItem(bucket);
            Pair<Item, Item> pair = Pair.of(bucket, this.bucketAfter);
            if (this.bucketItemGroup != null)
                ItemGroupEvents.modifyEntriesEvent(this.bucketItemGroup).register(content -> {
                    if (this.bucketAfter != null)
                        content.addAfter(this.bucketAfter, bucket);
                    else
                        content.add(bucket);
                });
        }

        if (this.createBottle && this.bottleItem == null) {
            Item bottle = null, splashBottle = null, lingeringBottle = null;
            if (this.bottleFactory != null)
                bottle = registerBottle(this.bottleFactory, this.bottleSettings, fluid, this.bottleTooltip, this.bottleStatusEffects, this.id.withSuffixedPath("_bottle"), null, 0);
            if (this.splashBottleFactory != null)
                splashBottle = registerBottle(this.splashBottleFactory, this.bottleSettings, fluid, this.bottleTooltip, this.bottleStatusEffects, this.id.withPrefixedPath("splash_").withSuffixedPath("_bottle"), bottle, 1);
            if (this.lingeringBottleFactory != null)
                lingeringBottle = registerBottle(this.lingeringBottleFactory, this.bottleSettings, fluid, this.bottleTooltip, this.bottleStatusEffects, this.id.withPrefixedPath("lingering_").withSuffixedPath("_bottle"), splashBottle, 2);

            // why
            Item bottle1 = bottle, splashBottle1 = splashBottle, lingeringBottle1 = lingeringBottle;
            // add custom bottles before the water bottle item
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
                if (bottle1 != null)
                    content.addBefore(itemStack -> itemStack.getItem() instanceof PotionItem,
                            Arrays.stream(new Item[] { bottle1 }).map(ItemStack::new).toList(),
                            ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
                if (splashBottle1 != null)
                    content.addBefore(itemStack -> itemStack.getItem() instanceof SplashPotionItem,
                            Arrays.stream(new Item[] { splashBottle1 }).map(ItemStack::new).toList(),
                            ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
                if (lingeringBottle1 != null)
                    content.addBefore(itemStack -> itemStack.getItem() instanceof LingeringPotionItem,
                            Arrays.stream(new Item[] { lingeringBottle1 }).map(ItemStack::new).toList(),
                            ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
            });
        } else if (this.bottleItem != null) {
            fluid.setBottleItem(this.bottleItem);
        }

        if (this.createCauldron) {
            AbstractCauldronBlock block;
            if (this.createBottle || this.bottleItem != null)
                block = new LeveledCauldronBlock(this.cauldronSettings, fluid, this.cauldronBurns);
            else
                block = new FullCauldronBlock(this.cauldronSettings, fluid.getEntityTick() != null ? fluid.getEntityTick() : entity -> {}, this.cauldronBurns);
            Registry.register(Registries.BLOCK, this.id.withSuffixedPath("_cauldron"), block);
            if (fluid.getTintColor() > -1 && fluid.getTintColor() < 0xffffff)
                TINTED_BLOCKS.add(Pair.of(block, fluid.getTintColor()));

            CauldronBehavior.CauldronBehaviorMap behaviorMap = CauldronBehavior.createMap(this.id.toString());
            Map<Item, CauldronBehavior> map = behaviorMap.map();
            CauldronBehavior.registerBucketBehavior(map);
            if (fluid.getBucketItem() != null) {
                map.put(BUCKET, (state, world, pos, player, hand, stack) ->
                        CauldronBehavior.emptyCauldron(state, world, pos, player, hand, stack, new ItemStack(fluid.getBucketItem()), (statex) ->
                                statex.getBlock() instanceof FullCauldronBlock || statex.get(LeveledCauldronBlock.LEVEL) == 3, fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
                );

                for (Map.Entry<String, CauldronBehavior.CauldronBehaviorMap> entry : CauldronBehavior.BEHAVIOR_MAPS.entrySet())
                    entry.getValue().map().put(fluid.getBucketItem(), (state, world, pos, player, hand, stack) ->
                            CauldronBehavior.fillCauldron(world, pos, player, hand, stack, block instanceof LeveledCauldronBlock ?
                                            block.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3) :
                                            block.getDefaultState(),
                                    fluid.getBucketFillSound().isPresent() ? fluid.getBucketFillSound().get() : SoundEvents.ITEM_BUCKET_FILL)
                    );
            }
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
                        world.setBlockState(pos, block.getDefaultState());
                        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
                    }

                    return ActionResult.success(world.isClient);
                });
            }

            block.behaviorMap = behaviorMap;
        }

        return fluid;
    }

    private static Item registerBottle(FluidBuilder.BottleFactory<Item> factory, Item.Settings settings, CustomFluid fluid, Text tooltip, List<StatusEffectInstance> statusEffects, Identifier id, @Nullable Item previous, int type) {
        Item bottle = Registry.register(Registries.ITEM, id, factory.create(settings, fluid.getTintColor(), tooltip, statusEffects));
        if (type == 0)
            fluid.setBottleItem(bottle);
        // make it possible to brew splash bottles and lingering bottles too
        if (bottle instanceof PotionItem potion) {
            BrewingRecipeRegistry.registerPotionType(potion); // not sure if this is needed or not
            if (previous instanceof PotionItem ingredient)
                BrewingRecipeRegistry.registerItemRecipe(ingredient, type == 1 ? GUNPOWDER : DRAGON_BREATH, potion);
        }
        return bottle;
    }

    interface CustomFluidFactory<T extends CustomFluid> {
        T create(CameraSubmersionType submersionType, CustomFluid.FogData fog,
                 Settings settings, FluidFactory<?> flowingFactory);
    }

    public interface FluidFactory<T> {
        T create(CustomFluid parent, Settings settings);
    }

    public interface BucketFactory<T> {
        T create(FlowableFluid fluid, Item.Settings settings);
    }

    public interface BottleFactory<T> {
        T create(Item.Settings settings, int tintColor, @Nullable Text tooltip, @Nullable List<StatusEffectInstance> statusEffects);
    }

    public interface CombinesWithPredicate {
        @Nullable BlockState test(FluidBlock self, World world, BlockPos pos, Direction direction);
    }

    public interface CombinesWithFlowingPredicate {
        @Nullable BlockState test(Fluid self, Fluid other, WorldAccess world, BlockPos pos);
    }

    public static class Settings {
        Identifier submergedTexture;
        Function<World, Boolean> infinite;
        Function<WorldView, Integer> flowSpeed;
        Function<WorldView, Integer> levelDecreasePerBlock;
        Function<WorldView, Integer> tickRate;
        float blastResistance;
        int tintColor;
        TagKey<Fluid> tag;
        Vec3d velocityMultiplier;
        boolean drippable;
        SoundEvent entitySwimSound, entitySplashSound, entityHighSpeedSplashSound;
        SoundEvent playerSwimSound, playerSplashSound, playerHighSpeedSplashSound;
        float splashSoundVolume, swimSoundVolume;
        boolean breathable;
        Consumer<Entity> entityTick;
        CombinesWithPredicate combinesWith;
        CombinesWithFlowingPredicate combinesWithFlowing;

        public Settings() {
            this.submergedTexture = new Identifier("textures/misc/underwater");
            this.infinite = ignored -> false;
            this.flowSpeed = ignored -> 4;
            this.levelDecreasePerBlock = ignored -> 1;
            this.tickRate = ignored -> 15;
            this.blastResistance = 100f;
            this.tintColor = -1;
            this.tag = FluidTags.WATER;
            this.velocityMultiplier = new Vec3d(1, 1, 1);
            this.drippable = true;
            this.entitySwimSound = SoundEvents.ENTITY_GENERIC_SWIM;
            this.entitySplashSound = SoundEvents.ENTITY_GENERIC_SPLASH;
            this.entityHighSpeedSplashSound = SoundEvents.ENTITY_GENERIC_SPLASH;
            this.playerSwimSound = SoundEvents.ENTITY_PLAYER_SWIM;
            this.playerSplashSound = SoundEvents.ENTITY_PLAYER_SPLASH;
            this.playerHighSpeedSplashSound = SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED;
            this.splashSoundVolume = 1f;
            this.swimSoundVolume = 1f;
            this.breathable = false;
            this.entityTick = entity -> {};
            this.combinesWith = (fluidBlock, world, pos, direction) -> null;
            this.combinesWithFlowing = (self, other, world, pos) -> null;
        }

        protected Settings submergedTexture(Identifier path) {
            this.submergedTexture = path;
            return this;
        }

        protected Settings infinite() {
            this.infinite = ignored -> true;
            return this;
        }

        protected Settings infinite(Function<World, Boolean> infinite) {
            this.infinite = infinite;
            return this;
        }

        protected Settings flowSpeed(int flowSpeed) {
            this.flowSpeed = ignored -> flowSpeed;
            return this;
        }

        protected Settings flowSpeed(Function<WorldView, Integer> flowSpeed) {
            this.flowSpeed = flowSpeed;
            return this;
        }

        protected Settings levelDecreasePerBlock(int levelDecreasePerBlock) {
            this.levelDecreasePerBlock = ignored -> levelDecreasePerBlock;
            return this;
        }

        protected Settings levelDecreasePerBlock(Function<WorldView, Integer> levelDecreasePerBlock) {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        protected Settings tickRate(int tickRate) {
            this.tickRate = ignored -> tickRate;
            return this;
        }

        protected Settings tickRate(Function<WorldView, Integer> tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        protected Settings blastResistance(float blastResistance) {
            this.blastResistance = blastResistance;
            return this;
        }

        protected Settings tintColor(int tintColor) {
            this.tintColor = tintColor;
            return this;
        }

        protected Settings tag(TagKey<Fluid> tag) {
            this.tag = tag;
            return this;
        }

        protected Settings velocityMultiplier(Vec3d velocityMultiplier) {
            this.velocityMultiplier = velocityMultiplier;
            return this;
        }

        protected Settings velocityMultiplier(double x, double y, double z) {
            this.velocityMultiplier(new Vec3d(x, y, z));
            return this;
        }

        protected Settings notDrippable() {
            this.drippable = false;
            return this;
        }

        public Settings swimSound(SoundEvent sound) {
            this.entitySwimSound = sound;
            return this;
        }

        public Settings playerSwimSound(SoundEvent sound) {
            this.playerSwimSound = sound;
            return this;
        }

        public Settings splashSound(SoundEvent sound) {
            this.entitySplashSound = sound;
            return this;
        }

        public Settings playerSplashSound(SoundEvent sound) {
            this.playerSplashSound = sound;
            return this;
        }

        public Settings highSpeedSplashSound(SoundEvent sound) {
            this.entityHighSpeedSplashSound = sound;
            return this;
        }

        public Settings playerHighSpeedSplashSound(SoundEvent sound) {
            this.playerHighSpeedSplashSound = sound;
            return this;
        }

        public Settings splashSoundVolume(float volume) {
            this.splashSoundVolume = volume;
            return this;
        }

        public Settings swimSoundVolume(float volume) {
            this.swimSoundVolume = volume;
            return this;
        }

        public Settings breathable() {
            this.breathable = true;
            return this;
        }

        public Settings entityTick(Consumer<Entity> func) {
            this.entityTick = func;
            return this;
        }

        public Settings combinesWith(CombinesWithPredicate predicate) {
            this.combinesWith = predicate;
            return this;
        }

        public Settings combinesWithBasic(@Nullable Block lavaCombination, @Nullable Block waterCombination, @Nullable Block otherCombination) {
            this.combinesWith((fluidBlock, world, pos, direction) -> {
                Fluid fluid = world.getFluidState(pos).getFluid();
                if (fluid.matchesType(Fluids.LAVA) && lavaCombination != null)
                    return lavaCombination.getDefaultState();
                else if (fluid.matchesType(Fluids.WATER) && waterCombination != null)
                    return waterCombination.getDefaultState();
                else if (!world.getFluidState(pos.offset(direction)).getFluid().matchesType(world.getFluidState(pos).getFluid()) && !fluid.matchesType(Fluids.EMPTY) && otherCombination != null)
                    return otherCombination.getDefaultState();
                return null;
            });
            return this;
        }

        public Settings vanillaCombinesWith(boolean playExtinguishSound) {
            this.combinesWith((fluidBlock, world, pos, direction) -> {
                BlockPos originalPos = pos.offset(direction);
                //System.out.println(world.getFluidState(pos).getFluid());
                if (!world.getFluidState(originalPos).getFluid().matchesType(world.getFluidState(pos).getFluid()) && (world.getFluidState(pos).isIn(FluidTags.WATER) || world.getFluidState(pos).isIn(FluidTags.LAVA))) {
                    if (playExtinguishSound)
                        world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, originalPos, 0);
                    return (direction == Direction.DOWN ? Blocks.STONE : (world.getFluidState(originalPos).isStill() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE)).getDefaultState();
                }
                return null;
            });
            return this;
        }

        public Settings combinesWithFlowing(CombinesWithFlowingPredicate predicate) {
            this.combinesWithFlowing = predicate;
            return this;
        }

        public Settings combinesWithFlowingBasic(@Nullable Block lavaCombination, @Nullable Block waterCombination, @Nullable Block otherCombination) {
            this.combinesWithFlowing((self, other, world, pos) -> {
                if (other.matchesType(Fluids.LAVA) && lavaCombination != null)
                    return lavaCombination.getDefaultState();
                else if (other.matchesType(Fluids.WATER) && waterCombination != null)
                    return waterCombination.getDefaultState();
                else if (!self.matchesType(other) && !other.isEmpty() && otherCombination != null)
                    return otherCombination.getDefaultState();
                return null;
            });
            return this;
        }

        public Settings vanillaCombinesWithFlowing(boolean playExtinguishSound) {
            this.combinesWithFlowing((self, other, world, pos) -> {
                if (other.matchesType(Fluids.LAVA)) {
                    if (playExtinguishSound)
                        world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
                    return Blocks.OBSIDIAN.getDefaultState();
                } else if (!self.matchesType(other) && !other.isEmpty()) {
                    if (playExtinguishSound)
                        world.syncWorldEvent(WorldEvents.LAVA_EXTINGUISHED, pos, 0);
                    return Blocks.STONE.getDefaultState();
                }
                return null;
            });
            return this;
        }

        public Identifier getSubmergedTexture() {
            return this.submergedTexture;
        }

        public Function<World, Boolean> isInfinite() {
            return this.infinite;
        }

        public Function<WorldView, Integer> getFlowSpeed() {
            return this.flowSpeed;
        }

        public Function<WorldView, Integer> getLevelDecreasePerBlock() {
            return this.levelDecreasePerBlock;
        }

        public Function<WorldView, Integer> getTickRate() {
            return this.tickRate;
        }

        public float getBlastResistance() {
            return this.blastResistance;
        }

        public int getTintColor() {
            return this.tintColor;
        }

        public TagKey<Fluid> getTag() {
            return this.tag;
        }

        public Vec3d getVelocityMultiplier() {
            return this.velocityMultiplier;
        }

        public boolean isDrippable() {
            return this.drippable;
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
            return splashSoundVolume;
        }

        public float getSwimSoundVolume() {
            return swimSoundVolume;
        }

        public boolean isBreathable() {
            return this.breathable;
        }

        public Consumer<Entity> getEntityTick() {
            return this.entityTick;
        }

        public CombinesWithPredicate getCombinesWith() {
            return this.combinesWith;
        }

        public CombinesWithFlowingPredicate getCombinesWithFlowing() {
            return this.combinesWithFlowing;
        }
    }
}