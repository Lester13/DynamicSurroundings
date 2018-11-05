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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.client.handlers.EnvironStateHandler.EnvironState;
import org.orecruncher.dsurround.client.handlers.bubbles.EntityBubbleContext;
import org.orecruncher.dsurround.client.handlers.bubbles.SpeechBubbleData;
import org.orecruncher.dsurround.event.SpeechTextEvent;
import org.orecruncher.lib.Translations;
import org.orecruncher.lib.WorldUtils;

import com.google.common.base.Function;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SpeechBubbleHandler extends EffectHandlerBase {

	private static final String SPLASH_TOKEN = "$MINECRAFT$";
	private static final ResourceLocation SPLASH_TEXT = new ResourceLocation("texts/splashes.txt");

	private final Int2ObjectOpenHashMap<EntityBubbleContext> messages = new Int2ObjectOpenHashMap<>();
	private final Translations xlate = new Translations();
	private final List<String> minecraftSplashText = new ArrayList<>();

	private static class Stripper implements Function<Entry<String, String>, String> {

		private final Pattern WEIGHT_PATTERN = Pattern.compile("^([0-9]*),(.*)");

		@Override
		public String apply(@Nonnull final Entry<String, String> input) {
			final Matcher matcher = this.WEIGHT_PATTERN.matcher(input.getValue());
			return matcher.matches() ? matcher.group(2) : input.getValue();
		}

	}

	private void loadText() {
		this.xlate.load("/assets/dsurround/dsurround/data/chat/");
		this.xlate.transform(new Stripper());

		try (final IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXT)) {
			@SuppressWarnings("deprecation")
			final BufferedReader bufferedreader = new BufferedReader(
					new InputStreamReader(resource.getInputStream(), Charsets.UTF_8));
			String s;

			while ((s = bufferedreader.readLine()) != null) {
				s = s.trim();

				if (!s.isEmpty()) {
					this.minecraftSplashText.add(s);
				}
			}

		} catch (final Throwable t) {
			;
		}
	}

	public SpeechBubbleHandler() {
		super("Speech Bubbles");
		loadText();
	}

	private void addSpeechBubbleFormatted(@Nonnull final Entity entity, @Nonnull final String message,
			final Object... parms) {
		String xlated = this.xlate.format(message, parms);
		if (this.minecraftSplashText.size() > 0 && SPLASH_TOKEN.equals(xlated)) {
			xlated = this.minecraftSplashText.get(this.RANDOM.nextInt(this.minecraftSplashText.size()));
		}
		addSpeechBubble(entity, xlated);
	}

	private void addSpeechBubble(@Nonnull final Entity entity, @Nonnull final String message) {
		if (StringUtils.isEmpty(message))
			return;

		EntityBubbleContext ctx = this.messages.get(entity.getEntityId());
		if (ctx == null) {
			this.messages.put(entity.getEntityId(), ctx = new EntityBubbleContext());
		}

		final int expiry = EnvironState.getTickCounter() + (int) (ModOptions.speechbubbles.speechBubbleDuration * 20F);
		ctx.add(new SpeechBubbleData(message, expiry));
		ctx.handleBubble(entity);
	}

	@Override
	public boolean doTick(final int tick) {
		return this.messages.size() > 0;
	}

	@Override
	public void process(@Nonnull final EntityPlayer player) {
		final int currentTick = EnvironState.getTickCounter();
		this.messages.int2ObjectEntrySet().removeIf(entry -> {
			return entry.getValue().clean(currentTick);
		});
	}

	@Override
	public void onConnect() {
		this.messages.clear();
	}

	@Override
	public void onDisconnect() {
		this.messages.clear();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public void onSpeechTextEvent(@Nonnull final SpeechTextEvent event) {

		final Entity entity = WorldUtils.locateEntity(EnvironState.getWorld(), event.entityId);
		if (entity == null)
			return;
		else if ((entity instanceof EntityPlayer) && !ModOptions.speechbubbles.enableSpeechBubbles)
			return;
		else if (!ModOptions.speechbubbles.enableEntityChat)
			return;

		if (event.translate)
			addSpeechBubbleFormatted(entity, event.message);
		else
			addSpeechBubble(entity, event.message);
	}
}