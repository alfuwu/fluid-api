package com.alfred.fluidapi.bottles;

import net.minecraft.item.PotionItem;

public class FluidPotionItem extends PotionItem {
    protected int tintColor;

    public FluidPotionItem(Settings settings, int tintColor) {
        super(settings);
        this.tintColor = tintColor;
    }

    public int getTintColor() {
        return this.tintColor;
    }
}
