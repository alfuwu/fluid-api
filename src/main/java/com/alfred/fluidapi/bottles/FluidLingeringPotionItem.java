package com.alfred.fluidapi.bottles;

import net.minecraft.item.LingeringPotionItem;

public class FluidLingeringPotionItem extends LingeringPotionItem {
    protected int tintColor;

    public FluidLingeringPotionItem(Settings settings, int tintColor) {
        super(settings);
        this.tintColor = tintColor;
    }

    public int getTintColor() {
        return this.tintColor;
    }
}
