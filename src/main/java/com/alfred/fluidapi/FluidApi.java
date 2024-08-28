package com.alfred.fluidapi;

import com.alfred.fluidapi.registry.*;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;

import static net.minecraft.block.Blocks.CAULDRON;

public class FluidApi implements ModInitializer {
	///* Test fluid
	public static CustomFluid SOUP = FluidBuilder.create(new Identifier("fluid-api", "soup"))
			.fogColor(0x762b1c) // makes the fog a dark reddish color
			.tintColor(0xff5533) // makes the fluid a red color
			.tag(FluidTags.LAVA) // makes entities burn in the fluid and makes the fluid act as lava does for most things
			.submergedTexture(new Identifier("fluid-api", "textures/misc/in_fluid.png")) // the texture that will render on screen when a player is submerged within the fluid
			.blastResistance(2f) // makes soup provide very little resistance against explosions
			.flowSpeed(1) // makes the liquid move every tick
			.tickRate(1) // increases tick rate to the maximum possible
			.velocityMultiplier(0.5, 0.5, 0.5) // halves the velocity of all living entities moving through the fluid
			.fogFactory(((fog, entity, viewDistance, underwaterVisibility) -> { // you can call .lavaFog() to achieve the same effect as this
				if (entity.isSpectator())
					return Pair.of(-8.0f, viewDistance * 0.5f);
				else if (entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
					return Pair.of(0.0f, 3.0f);
				else
					return Pair.of(0.25f, 1.0f);
			})) // makes the fog that appears while submerged act exactly how the fog of lava does
			.blockSettings(FabricBlockSettings.create().luminance(5)) // makes the fluid slightly emissive
			.cauldronSettings(FabricBlockSettings.copyShallow(CAULDRON).luminance(ignored -> 5)) // makes the cauldron also slightly emissive
			.submersionType(CameraSubmersionType.LAVA) // doesn't really do much
			.bottle() // adds bottles and makes the cauldron a leveled cauldron (like that of the water cauldron)
			.customLingeringBottleItem(null) // removes the lingering bottle variant
			.bottleStatusEffects(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 3))
			.entityTick(entity -> {
				if (entity instanceof LivingEntity living)
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 3)); // gives living entities the nausea IV status effect while touching soup, and then for 10 seconds afterwards
			})
			.vanillaCombinesWith(true)
			.vanillaCombinesWithFlowing(true)
			//.combinesWithBasic(Blocks.SLIME_BLOCK, Blocks.NETHERRACK, null)
			.build(); // creates the fluid
	//*/

	@Override
	public void onInitialize() { }
}