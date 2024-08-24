package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.mojang.datafixers.util.Pair;
import io.netty.util.internal.UnstableApi;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.*;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.block.Blocks.CAULDRON;
import static net.minecraft.item.Items.BUCKET;

@SuppressWarnings("unused")
public class FluidBuilder {
    private static final AbstractBlock.Settings DEFAULT_FLUID_SETTINGS = FabricBlockSettings.create().replaceable().noCollision().strength(100.0f).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY);
    private static final Item.Settings DEFAULT_BUCKET_SETTINGS = new FabricItemSettings().recipeRemainder(BUCKET).maxCount(1);
    private static final AbstractBlock.Settings DEFAULT_CAULDRON_SETTINGS = FabricBlockSettings.copyShallow(CAULDRON);
    public static final Map<Identifier, CustomFluid> FLUIDS = new HashMap<>();
    public static final Map<Identifier, AbstractBlock.Settings> FLUID_SETTINGS = new HashMap<>();
    public static final Map<Identifier, Item.Settings> BUCKET_SETTINGS = new HashMap<>();
    public static final Map<Identifier, AbstractBlock.Settings> CAULDRON_SETTINGS = new HashMap<>();
    public static final Map<Identifier, Pair<RegistryKey<ItemGroup>, Item>> BUCKET_GROUP_MAPPING = new HashMap<>();
    public static final List<Pair<Identifier, Pair<Boolean, Boolean>>> CAULDRONS = new ArrayList<>();

    protected final Identifier id;
    protected CustomFluid.FogData fog;
    protected CameraSubmersionType submersionType;
    protected RegistryKey<ItemGroup> bucketItemGroup;
    protected RegistryKey<ItemGroup> bottleItemGroup;
    protected Item bucketAfter;
    protected Item bottleAfter;
    protected CustomFluidFactory<?> factory;
    protected FluidFactory<?> flowingFactory;
    protected BucketFactory<Item> bucketFactory;
    protected BottleFactory<Item> bottleFactory;
    protected Settings settings;
    protected AbstractBlock.Settings fluidSettings;
    protected Item.Settings bucketSettings;
    protected AbstractBlock.Settings cauldronSettings;
    protected boolean createBucket;
    protected boolean createCauldron;
    protected boolean cauldronBurns;
    protected boolean createBottleOfFluid;

    private FluidBuilder(Identifier id) {
        this.id = id;
    }

    public static FluidBuilder create(Identifier identifier) {
        FluidBuilder builder = new FluidBuilder(identifier);
        builder.fog = new CustomFluid.FogData(0.3f, 0.3f, 0.3f);
        builder.submersionType = CameraSubmersionType.WATER;
        builder.bucketItemGroup = ItemGroups.TOOLS;
        builder.bucketAfter = Items.MILK_BUCKET;
        //builder.bottleAfter = Items.GLASS_BOTTLE;
        builder.bucketFactory = BucketItem::new;
        //builder.bottleFactory = GlassBottleItem::new;
        builder.factory = CustomFluid::new;
        builder.flowingFactory = CustomFluid.Flowing::new;
        builder.settings = new FluidBuilder.Settings();
        builder.fluidSettings = DEFAULT_FLUID_SETTINGS;
        builder.bucketSettings = DEFAULT_BUCKET_SETTINGS;
        builder.cauldronSettings = DEFAULT_CAULDRON_SETTINGS;
        builder.createBucket = true;
        builder.createCauldron = true;
        builder.cauldronBurns = false;
        builder.createBottleOfFluid = false;
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
     * @param color A 6 digit hex value that will be interpreted as a color and used to tint the submerged overlay
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
    }

    /**
     * @param group The creative inventory tab where the fluid's custom bucket item will appear in
     * @return this
     */
    public FluidBuilder itemGroup(RegistryKey<ItemGroup> group) {
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
     * @param flowSpeed How many ticks it takes for the liquid to flow one block
     * @return this
     */
    public FluidBuilder flowSpeed(int flowSpeed) {
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
     * @param tickRate Determines how fast the fluid ticks
     * @return this
     */
    public FluidBuilder tickRate(int tickRate) {
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
     * @param movementSpeed A Vec3d that multiplies any entity within the custom fluid's speed
     * @return this
     */
    public FluidBuilder velocityMultiplier(Vec3d movementSpeed) {
        this.settings.velocityMultiplier(movementSpeed);
        return this;
    }

    /**
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
     * @param settings The block settings for the custom fluid
     * @return this
     */
    public FluidBuilder blockSettings(AbstractBlock.Settings settings) {
        this.fluidSettings = settings.replaceable().noCollision().strength(100f).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY);
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
     * Note: does not apply default fluid block settings. Have fun :)
     * @param settings The block settings for the custom fluid
     * @return this
     */
    public FluidBuilder blockSettingsNoDefaults(AbstractBlock.Settings settings) {
        this.fluidSettings = settings;
        return this;
    }

    /**
     * Builds the custom fluid and returns it
     * @return CustomFluid
     */
    public CustomFluid build() {
        if (FLUIDS.containsKey(this.id))
            throw new RuntimeException("ID already registered: " + this.id);
        CustomFluid fluid = this.factory.create(this.bucketFactory, this.submersionType, this.fog, this.settings, this.flowingFactory);
        if (this.createBucket) {
            BUCKET_GROUP_MAPPING.put(this.id, Pair.of(this.bucketItemGroup, this.bucketAfter));
            BUCKET_SETTINGS.put(this.id, this.bucketSettings);
        }
        if (this.createCauldron) {
            CAULDRONS.add(Pair.of(this.id, Pair.of(this.cauldronBurns, this.createBottleOfFluid)));
            CAULDRON_SETTINGS.put(this.id, this.cauldronSettings);
        }
        FLUIDS.put(this.id, fluid);
        FLUID_SETTINGS.put(this.id, this.fluidSettings);
        return fluid;
    }

    interface CustomFluidFactory<T extends CustomFluid> {
        T create(BucketFactory<Item> bucketFactory, CameraSubmersionType submersionType, CustomFluid.FogData fog,
                 Settings settings, FluidFactory<?> flowingFactory);
    }

    public interface FluidFactory<T> {
        T create(CustomFluid parent, Settings settings);
    }

    public interface BucketFactory<T> {
        T create(FlowableFluid fluid, Item.Settings settings);
    }

    public interface BottleFactory<T> {
        T create(Item.Settings settings);
    }

    public static class Settings {
        Identifier submergedTexture;
        boolean infinite;
        int flowSpeed;
        int levelDecreasePerBlock;
        int tickRate;
        float blastResistance;
        int tintColor;
        TagKey<Fluid> tag;
        Vec3d velocityMultiplier;

        public Settings() {
            this.submergedTexture = new Identifier("textures/misc/underwater");
            this.infinite = false;
            this.flowSpeed = 4;
            this.levelDecreasePerBlock = 1;
            this.tickRate = 15;
            this.blastResistance = 100f;
            this.tintColor = -1;
            this.tag = FluidTags.WATER;
            this.velocityMultiplier = new Vec3d(1, 1, 1);
        }

        protected Settings submergedTexture(Identifier path) {
            this.submergedTexture = path;
            return this;
        }

        protected Settings infinite() {
            this.infinite = true;
            return this;
        }

        protected Settings flowSpeed(int flowSpeed) {
            this.flowSpeed = flowSpeed;
            return this;
        }

        protected Settings levelDecreasePerBlock(int levelDecreasePerBlock) {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        protected Settings tickRate(int tickRate) {
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

        public Identifier getSubmergedTexture() {
            return this.submergedTexture;
        }

        public boolean isInfinite() {
            return this.infinite;
        }

        public int getFlowSpeed() {
            return this.flowSpeed;
        }

        public int getLevelDecreasePerBlock() {
            return this.levelDecreasePerBlock;
        }

        public int getTickRate() {
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
    }
}