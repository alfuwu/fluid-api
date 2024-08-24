package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.item.Items.BUCKET;

public class Items {
    public static void register() {
        Map<RegistryKey<ItemGroup>, List<Pair<Item, Item>>> itemMap = new HashMap<>();
        for (Map.Entry<Identifier, Pair<RegistryKey<ItemGroup>, Item>> entry : FluidBuilder.BUCKET_GROUP_MAPPING.entrySet()) {
            CustomFluid fluid = FluidBuilder.FLUIDS.get(entry.getKey());
            Item bucket = fluid.getBucketFactory().create(fluid, new FabricItemSettings().recipeRemainder(BUCKET).maxCount(1));
            fluid.setBucketItem(Registry.register(Registries.ITEM, entry.getKey().withSuffixedPath("_bucket"), bucket));
            Pair<Item, Item> pair = Pair.of(fluid.getBucketItem(), entry.getValue().getSecond());
            RegistryKey<ItemGroup> group = entry.getValue().getFirst();
            if (itemMap.containsKey(group))
                itemMap.get(group).add(pair);
            else
                itemMap.put(group, new ArrayList<>() {{ add(pair); }});
        }

        for (Map.Entry<RegistryKey<ItemGroup>, List<Pair<Item, Item>>> items : itemMap.entrySet()) {
            ItemGroupEvents.modifyEntriesEvent(items.getKey()).register(content -> {
                for (Pair<Item, Item> pair : items.getValue())
                    if (pair.getSecond() == null)
                        content.add(pair.getFirst());
                    else
                        content.addAfter(pair.getSecond(), pair.getFirst());
            });
        }
    }
}
