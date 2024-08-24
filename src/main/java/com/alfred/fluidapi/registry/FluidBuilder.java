package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.*;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class FluidBuilder {
    public static final Map<Identifier, CustomFluid> FLUIDS = new HashMap<>();
    public static final Map<Identifier, Pair<RegistryKey<ItemGroup>, Item>> BUCKET_GROUP_MAPPING = new HashMap<>();
    protected final Identifier id;
    protected final CustomFluid.FogData fog;
    protected CameraSubmersionType submersionType;
    protected RegistryKey<ItemGroup> bucketItemGroup;
    protected Item after;
    protected CustomFluidFactory<?> factory;
    protected FluidFactory<?> flowingFactory;
    protected BucketFactory<Item> bucketFactory;

    private FluidBuilder(Identifier id, CustomFluid.FogData fog) {
        this.id = id;
        this.fog = fog;
    }

    public static FluidBuilder create(Identifier identifier) {
        FluidBuilder builder = new FluidBuilder(identifier, new CustomFluid.FogData(0.3f, 0.3f, 0.3f));
        builder.submersionType = CameraSubmersionType.WATER;
        builder.bucketItemGroup = ItemGroups.TOOLS;
        builder.after = Items.MILK_BUCKET;
        builder.bucketFactory = BucketItem::new;
        builder.factory = CustomFluid::new;
        builder.flowingFactory = CustomFluid.Flowing::new;
        return builder;
    }

    public FluidBuilder fogColor(float r, float g, float b) {
        this.fog.r = r;
        this.fog.g = g;
        this.fog.b = b;
        return this;
    }

    public FluidBuilder fogColor(int color) {
        int r = color & 0xFF;
        int g = color & 0xFF00 >> 8;
        int b = color & 0xFF0000 >> 16;
        return this.fogColor(r / 255f, g / 255f, b / 255f);
    }

    public FluidBuilder submersionType(CameraSubmersionType type) {
        this.submersionType = type;
        return this;
    }

    public FluidBuilder itemGroup(RegistryKey<ItemGroup> group) {
        this.bucketItemGroup = group;
        return this;
    }

    public FluidBuilder appearAfter(Item after) {
        this.after = after;
        return this;
    }

    public FluidBuilder customFluid(CustomFluidFactory<?> factory) {
        this.factory = factory;
        return this;
    }

    public FluidBuilder customFlowingFluid(FluidFactory<CustomFluid> factory) {
        this.flowingFactory = factory;
        return this;
    }

    public FluidBuilder customBucketItem(BucketFactory<Item> factory) {
        this.bucketFactory = factory;
        return this;
    }

    public CustomFluid build() {
        if (FLUIDS.containsKey(this.id))
            throw new RuntimeException("ID already registered: " + this.id);
        CustomFluid fluid = this.factory.create(this.bucketFactory, this.submersionType, this.fog, this.flowingFactory);
        BUCKET_GROUP_MAPPING.put(this.id, Pair.of(this.bucketItemGroup, this.after));
        FLUIDS.put(this.id, fluid);
        return fluid;
    }

    interface CustomFluidFactory<T extends CustomFluid> {
        T create(BucketFactory<Item> bucketFactory, CameraSubmersionType submersionType, CustomFluid.FogData fog,
                 FluidFactory<?> flowingFactory);
    }

    public interface FluidFactory<T> {
        T create(BucketFactory<Item> bucketFactory, CustomFluid parent, CameraSubmersionType submersionType, CustomFluid.FogData fog);
    }

    public interface BucketFactory<T> {
        T create(FlowableFluid fluid, Item.Settings settings);
    }
}