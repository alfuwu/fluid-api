package com.alfred.fluidapi.registry;

import com.alfred.fluidapi.CustomFluid;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.item.*;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.item.Items.*;

public class Items {
    public static void register() {
        Map<RegistryKey<ItemGroup>, List<Pair<Item, Item>>> itemMap = new HashMap<>();
        for (Map.Entry<Identifier, Pair<RegistryKey<ItemGroup>, Item>> entry : FluidBuilder.BUCKET_GROUP_MAPPING.entrySet()) {
            CustomFluid fluid = FluidBuilder.FLUIDS.get(entry.getKey());
            Item bucket = fluid.getBucketFactory().create(fluid, FluidBuilder.BUCKET_SETTINGS.get(entry.getKey()));
            fluid.setBucketItem(Registry.register(Registries.ITEM, entry.getKey().withSuffixedPath("_bucket"), bucket));
            Pair<Item, Item> pair = Pair.of(fluid.getBucketItem(), entry.getValue().getSecond());
            RegistryKey<ItemGroup> group = entry.getValue().getFirst();
            if (itemMap.containsKey(group))
                itemMap.get(group).add(pair);
            else
                itemMap.put(group, new ArrayList<>() {{ add(pair); }});
        }
        FluidBuilder.BUCKET_SETTINGS.clear();

        // nvm i fixed the nesting nightmare
        // i forgot to update the comment though
        List<Pair<Item, Integer>> bottles = new ArrayList<>();
        for (Pair<Identifier, Pair<Boolean, Boolean>> cauldron : FluidBuilder.CAULDRONS) {
            if (cauldron.getSecond().getSecond()) {
                CustomFluid fluid = FluidBuilder.FLUIDS.get(cauldron.getFirst());
                Item bottle = null;
                Item splashBottle = null;
                if (fluid.getBottleFactory() != null)
                    bottle = registerBottle(fluid.getBottleFactory(), fluid, cauldron.getFirst().withSuffixedPath("_bottle"), bottles, null, 0);
                if (fluid.getSplashBottleFactory() != null)
                    splashBottle = registerBottle(fluid.getSplashBottleFactory(), fluid, cauldron.getFirst().withPrefixedPath("splash_").withSuffixedPath("_bottle"), bottles, bottle, 1);
                if (fluid.getLingeringBottleFactory() != null)
                    registerBottle(fluid.getLingeringBottleFactory(), fluid, cauldron.getFirst().withPrefixedPath("lingering_").withSuffixedPath("_bottle"), bottles, splashBottle, 2);
            }
        }
        FluidBuilder.BOTTLE_SETTINGS.clear();

        for (Map.Entry<RegistryKey<ItemGroup>, List<Pair<Item, Item>>> items : itemMap.entrySet())
            if (items.getKey() != null)
                ItemGroupEvents.modifyEntriesEvent(items.getKey()).register(content -> {
                    for (Pair<Item, Item> pair : items.getValue())
                        if (pair.getSecond() == null)
                            content.add(pair.getFirst());
                        else
                            content.addAfter(pair.getSecond(), pair.getFirst());
                });

        // add custom bottles before the water bottle item
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(content -> {
            for (Pair<Item, Integer> bottle : bottles) {
                Item item = bottle.getFirst();
                int type = bottle.getSecond();
                content.addBefore(itemStack ->
                                type == 0 ? itemStack.getItem() instanceof PotionItem :
                                        type == 1 ? itemStack.getItem() instanceof SplashPotionItem :
                                                itemStack.getItem() instanceof LingeringPotionItem,
                        Arrays.stream(new Item[] { item }).map(ItemStack::new).toList(),
                        ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }

    private static Item registerBottle(FluidBuilder.BottleFactory<Item> factory, CustomFluid fluid, Identifier id, List<Pair<Item, Integer>> bottles, @Nullable Item previous, int type) {
        Item bottle = Registry.register(Registries.ITEM, id, factory.create(new FabricItemSettings().maxCount(1), fluid.getTintColor()));
        if (type == 0)
            fluid.setBottleItem(bottle);
        bottles.add(Pair.of(bottle, type));
        // make it possible to brew splash bottles and lingering bottles too
        if (bottle instanceof PotionItem potion) {
            BrewingRecipeRegistry.registerPotionType(potion); // not sure if this is needed or not
            if (previous instanceof PotionItem ingredient)
                BrewingRecipeRegistry.registerItemRecipe(ingredient, type == 1 ? GUNPOWDER : DRAGON_BREATH, potion);
        }
        return bottle;
    }
}
