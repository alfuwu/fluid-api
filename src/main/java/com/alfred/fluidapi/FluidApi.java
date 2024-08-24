package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.*;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;

import static net.minecraft.block.Blocks.CAULDRON;

public class FluidApi implements ModInitializer {
	public static CustomFluid SOUP = FluidBuilder.create(FluidApi.identifier("soup"))
			.fogColor(0x762B1C)
			.tintColor(0xFF5533)
			.tag(FluidTags.LAVA)
			.submergedTexture(identifier("textures/misc/in_fluid.png"))
			.blastResistance(2f)
			.flowSpeed(1)
			.tickRate(1)
			.velocityMultiplier(0.5, 0.5, 0.5)
			.fogFactory(((fog, entity, viewDistance, underwaterVisibility) -> { // you can call .lavaFog() to achieve the same effect as this
				if (entity.isSpectator())
					return Pair.of(-8.0f, viewDistance * 0.5f);
				else if (entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
					return Pair.of(0.0f, 3.0f);
				else
					return Pair.of(0.25f, 1.0f);
			}))
			.blockSettings(FabricBlockSettings.create().luminance(5))
			.cauldronSettings(FabricBlockSettings.copyShallow(CAULDRON).luminance(ignored -> 5))
			.submersionType(CameraSubmersionType.LAVA)
			.build();

	@Override
	public void onInitialize() {
		Blocks.register();
		Items.register();
		Blocks.registerCauldrons();
		// god this is so scuffed
	}
	
	public static Identifier identifier(String path) {
		return new Identifier("fluid-api", path);
	}
}