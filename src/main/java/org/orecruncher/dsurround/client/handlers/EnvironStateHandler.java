/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.orecruncher.dsurround.client.handlers;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.orecruncher.dsurround.ModBase;
import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.capabilities.CapabilitySeasonInfo;
import org.orecruncher.dsurround.capabilities.season.ISeasonInfo;
import org.orecruncher.dsurround.capabilities.season.TemperatureRating;
import org.orecruncher.dsurround.client.handlers.scanners.BattleScanner;
import org.orecruncher.dsurround.client.handlers.scanners.CeilingCoverage;
import org.orecruncher.dsurround.client.weather.Weather;
import org.orecruncher.dsurround.event.DiagnosticEvent;
import org.orecruncher.dsurround.expression.ExpressionEngine;
import org.orecruncher.dsurround.registry.RegistryManager;
import org.orecruncher.dsurround.registry.biome.BiomeInfo;
import org.orecruncher.dsurround.registry.biome.BiomeRegistry;
import org.orecruncher.dsurround.registry.dimension.DimensionData;
import org.orecruncher.dsurround.registry.dimension.DimensionRegistry;
import org.orecruncher.dsurround.registry.item.ItemClass;
import org.orecruncher.lib.DiurnalUtils;
import org.orecruncher.lib.DiurnalUtils.DayCycle;
import org.orecruncher.lib.MinecraftClock;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EnvironStateHandler extends EffectHandlerBase {

	public static class EnvironState {

		// State that is gathered from the various sources
		// to avoid requery. Used during the tick.
		private static BiomeInfo playerBiome = null;
		private static BiomeInfo truePlayerBiome = null;
		private static int dimensionId;
		private static String dimensionName;
		private static DimensionData dimInfo = DimensionData.NONE;
		private static BlockPos playerPosition;
		private static TemperatureRating playerTemperature;
		private static TemperatureRating biomeTemperature;
		private static boolean inside;
		private static ItemStack armorStack;
		private static ItemStack footArmorStack;
		private static boolean inVillage;

		private static boolean isUnderground;
		private static boolean isInSpace;
		private static boolean isInClouds;

		private static int lightLevel;
		private static int tickCounter;

		private static DayCycle dayCycle;

		private static MinecraftClock clock = new MinecraftClock();
		private static BattleScanner battle = new BattleScanner();

		private static BlockPos getPlayerPos() {
			final EntityPlayer player = getPlayer();
			return new BlockPos(player.posX, player.getEntityBoundingBox().minY, player.posZ);
		}

		private static void reset() {
			final BiomeInfo WTF = RegistryManager.BIOME.WTF_INFO;
			playerBiome = WTF;
			truePlayerBiome = WTF;
			dimensionId = 0;
			dimensionName = StringUtils.EMPTY;
			dimInfo = DimensionData.NONE;
			playerPosition = BlockPos.ORIGIN;
			playerTemperature = TemperatureRating.MILD;
			biomeTemperature = TemperatureRating.MILD;
			inside = false;
			armorStack = ItemStack.EMPTY;
			footArmorStack = ItemStack.EMPTY;
			inVillage = false;
			isUnderground = false;
			isInSpace = false;
			isInClouds = false;
			lightLevel = 0;
			tickCounter = 1;
			dayCycle = DayCycle.NO_SKY;
			clock = new MinecraftClock();
			battle = new BattleScanner();
		}

		private static void tick(final World world, final EntityPlayer player) {

			final BiomeRegistry biomes = RegistryManager.BIOME;
			final ISeasonInfo season = CapabilitySeasonInfo.getCapability(getWorld());
			final DimensionRegistry dimensions = RegistryManager.DIMENSION;
			final EnvironStateHandler stateHandler = EffectManager.instance().lookupService(EnvironStateHandler.class);
			if (stateHandler == null)
				ModBase.log().warn("Null EnvironStateHandler in EnvironState.tick()");

			EnvironState.dimInfo = dimensions.getData(player.getEntityWorld());
			EnvironState.clock.update(getWorld());
			EnvironState.playerBiome = biomes.getPlayerBiome(player, false);
			EnvironState.dimensionId = world.provider.getDimension();
			EnvironState.dimensionName = world.provider.getDimensionType().getName();
			EnvironState.playerPosition = getPlayerPos();
			EnvironState.inside = stateHandler == null ? false : stateHandler.isReallyInside();

			EnvironState.truePlayerBiome = biomes.getPlayerBiome(player, true);
			EnvironState.playerTemperature = season.getPlayerTemperature();
			EnvironState.biomeTemperature = season.getBiomeTemperature(getPlayerPosition());

			EnvironState.armorStack = ItemClass.effectiveArmorStack(player);
			EnvironState.footArmorStack = ItemClass.footArmorStack(player);

			EnvironState.isUnderground = EnvironState.playerBiome == biomes.UNDERGROUND_INFO;
			EnvironState.isInSpace = EnvironState.playerBiome == biomes.OUTERSPACE_INFO;
			EnvironState.isInClouds = EnvironState.playerBiome == biomes.CLOUDS_INFO;

			final BlockPos pos = EnvironState.getPlayerPosition();
			final int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
			final int skyLight = world.getLightFor(EnumSkyBlock.SKY, pos) - world.calculateSkylightSubtracted(1.0F);
			EnvironState.lightLevel = Math.max(blockLight, skyLight);

			EnvironState.dayCycle = DiurnalUtils.getCycle(world);

			// Trigger the battle scanner
			EnvironState.battle.update();

			if (!Minecraft.getMinecraft().isGamePaused())
				EnvironState.tickCounter++;
		}

		public static MinecraftClock getClock() {
			return clock;
		}

		public static BattleScanner getBattleScanner() {
			return battle;
		}

		public static DimensionData getDimensionInfo() {
			return dimInfo;
		}

		public static BiomeInfo getPlayerBiome() {
			return playerBiome;
		}

		public static BiomeInfo getTruePlayerBiome() {
			return truePlayerBiome;
		}

		public static String getBiomeName() {
			return EnvironState.playerBiome.getBiomeName();
		}

		public static TemperatureRating getPlayerTemperature() {
			return playerTemperature;
		}

		public static TemperatureRating getBiomeTemperature() {
			return biomeTemperature;
		}

		public static int getDimensionId() {
			return dimensionId;
		}

		public static String getDimensionName() {
			return dimensionName;
		}

		public static EntityPlayer getPlayer() {
			return Minecraft.getMinecraft().player;
		}

		public static World getWorld() {
			return getPlayer().getEntityWorld();
		}

		public static BlockPos getPlayerPosition() {
			return playerPosition;
		}

		public static boolean isPlayer(final Entity entity) {
			return entity instanceof EntityPlayer && entity.getPersistentID().equals(getPlayer().getPersistentID());
		}

		public static boolean isPlayerHurt() {
			final EntityPlayer player = getPlayer();
			return ModOptions.player.playerHurtThreshold != 0 && !player.isCreative()
					&& player.getHealth() <= (ModOptions.player.playerHurtThreshold * player.getMaxHealth());
		}

		public static boolean isPlayerHungry() {
			final EntityPlayer player = getPlayer();
			return ModOptions.player.playerHungerThreshold != 0 && !player.isCreative()
					&& player.getFoodStats().getFoodLevel() <= ModOptions.player.playerHungerThreshold;
		}

		public static boolean isPlayerBurning() {
			return getPlayer().isBurning();
		}

		public static boolean isPlayerSuffocating() {
			return getPlayer().getAir() <= 0;
		}

		public static boolean isPlayerFlying() {
			return getPlayer().capabilities.isFlying;
		}

		public static boolean isPlayerSprinting() {
			return getPlayer().isSprinting();
		}

		public static boolean isPlayerInLava() {
			return getPlayer().isInLava();
		}

		public static boolean isPlayerInvisible() {
			return getPlayer().isInvisible();
		}

		public static boolean isPlayerBlind() {
			return getPlayer().isPotionActive(MobEffects.BLINDNESS);
		}

		public static boolean isPlayerInWater() {
			return getPlayer().isInWater();
		}

		public static boolean isPlayerRiding() {
			return getPlayer().isRiding();
		}

		public static boolean isPlayerMoving() {
			final EntityPlayer player = getPlayer();
			return player.distanceWalkedModified != player.prevDistanceWalkedModified;
		}

		public static boolean isPlayerInside() {
			return inside;
		}

		public static boolean isPlayerUnderground() {
			return isUnderground;
		}

		public static boolean isPlayerInSpace() {
			return isInSpace;
		}

		public static boolean isPlayerInClouds() {
			return isInClouds;
		}

		public static ItemStack getPlayerItemStack() {
			return armorStack;
		}

		public static ItemStack getPlayerFootArmorStack() {
			return footArmorStack;
		}

		public static boolean inVillage() {
			return inVillage;
		}

		public static void setInVillage(final boolean f) {
			inVillage = f;
		}

		public static int getLightLevel() {
			return lightLevel;
		}

		public static int getTickCounter() {
			return tickCounter;
		}

		public static DayCycle getDayCycle() {
			return dayCycle;
		}

		public static float getPartialTick() {
			return Minecraft.getMinecraft().getRenderPartialTicks();
		}

	}

	protected final CeilingCoverage ceiling = new CeilingCoverage();

	public EnvironStateHandler() {
		super("State Handler");
	}

	@Override
	public void process(@Nonnull final EntityPlayer player) {
		EnvironState.tick(player.getEntityWorld(), player);
		Weather.update();
		ExpressionEngine.instance().reset();
		this.ceiling.update();
	}

	public boolean isReallyInside() {
		return this.ceiling.isReallyInside();
	}

	@Override
	public void onConnect() {
		EnvironState.reset();
		ExpressionEngine.instance();
	}

	@Override
	public void onDisconnect() {
		EnvironState.reset();
	}

	// Use the new scripting system to pull out data to display
	// for debug. Good for testing.
	private final static String[] scripts = {
		//@formatter:off
		"'Dim: ' + dim.id + '/' + dim.name",
		"'Biome: ' + biome.name + ' (' + biome.id + '); Temp ' + biome.temperature + '/' + biome.temperatureValue + ' rainfall: ' + biome.rainfall",
		"'Weather: ' + IF(weather.isRaining,'rainfall: ' + weather.rainfall,'not raining') + IF(weather.isThundering,' thundering','') + ' Temp: ' + weather.temperature + '/' + weather.temperatureValue + ' ice: ' + IF(weather.canWaterFreeze, 'true', 'false') + ' ' + IF(weather.temperatureValue < 0.2, '(breath)', '')",
		"'Season: ' + season.season + IF(diurnal.isNight,' night',' day') + IF(player.isInside,' inside',' outside') + ' celestialAngle: ' + diurnal.celestialAngle",
		"'Player: Temp ' + player.temperature + '; health ' + player.health + '/' + player.maxHealth + '; food ' + player.food.level + '; saturation ' + player.food.saturation + IF(player.isHurt,' isHurt','') + IF(player.isHungry,' isHungry','') + ' pos: (' + player.X + ',' + player.Y + ',' + player.Z + ') light: ' + player.lightLevel",
		"'Village: ' + player.inVillage"
		//@formatter:on
	};

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void diagnostics(final DiagnosticEvent.Gather event) {

		event.output.add(TextFormatting.GREEN + "Minecraft Date: " + EnvironState.getClock().toString());

		for (final String s : scripts) {
			final String result = ExpressionEngine.instance().eval(s).toString();
			event.output.add(TextFormatting.YELLOW + result);
		}

		final List<String> badScripts = ExpressionEngine.instance().getNaughtyList();
		for (final String s : badScripts) {
			event.output.add("BAD SCRIPT: " + s);
		}
	}

}
