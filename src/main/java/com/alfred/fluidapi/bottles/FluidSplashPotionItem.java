package com.alfred.fluidapi.bottles;

import net.minecraft.item.SplashPotionItem;

public class FluidSplashPotionItem extends SplashPotionItem {
    protected int tintColor;

    public FluidSplashPotionItem(Settings settings, int tintColor) {
        super(settings);
        this.tintColor = tintColor;
    }

    public int getTintColor() {
        return this.tintColor;
    }
}
