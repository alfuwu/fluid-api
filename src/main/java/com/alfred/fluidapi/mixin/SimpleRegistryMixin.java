package com.alfred.fluidapi.mixin;

import net.minecraft.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/*@Mixin(SimpleRegistry.class)
public class SimpleRegistryMixin {
    @Redirect(method = "freeze", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    private boolean isEmpty(Map<?, ?> instance) {
        if (instance.containsValue())
    }
}
*/