package com.alfred.fluidapi.bottles;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidSplashPotionItem extends SplashPotionItem {
    protected int tintColor;
    protected Text tooltip;
    protected List<StatusEffectInstance> statusEffects;

    public FluidSplashPotionItem(Settings settings, int tintColor, @Nullable Text tooltip, @Nullable List<StatusEffectInstance> statusEffects) {
        super(settings);
        this.tintColor = tintColor;
        this.tooltip = tooltip;
        this.statusEffects = statusEffects;
    }

    public int getTintColor() {
        return this.tintColor;
    }

    public List<StatusEffectInstance> getStatusEffects() {
        return this.statusEffects;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (this.tooltip == null)
            super.appendTooltip(stack, world, tooltip, context);
        else
            tooltip.add(this.tooltip);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey();
    }
}
