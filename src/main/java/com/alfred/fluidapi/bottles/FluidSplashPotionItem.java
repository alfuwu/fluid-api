package com.alfred.fluidapi.bottles;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.text.Text;

import java.util.List;

public class FluidSplashPotionItem extends SplashPotionItem {
    protected int tintColor;
    protected Text tooltip;
    protected List<StatusEffect> statusEffects;

    public FluidSplashPotionItem(Settings settings, int tintColor) {
        super(settings);
        this.tintColor = tintColor;
    }

    public int getTintColor() {
        return this.tintColor;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey();
    }
}
