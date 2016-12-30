/* This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
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

package org.blockartistry.mod.DynSurround.client.sound;

import java.util.Random;

import javax.annotation.Nonnull;

import org.blockartistry.mod.DynSurround.ModLog;
import org.blockartistry.mod.DynSurround.util.XorShiftRandom;

import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemConfig;
import net.minecraftforge.fml.relauncher.Side;

/*
 * Emitters are used to produce sounds that are continuous
 * or on repeat. They ensure that the sound is always queue
 * in the sound system even if the underlying sound system
 * cancels the sound.
 */
@SideOnly(Side.CLIENT)
public class Emitter {

	protected static final Random RANDOM = new XorShiftRandom();

	protected final SoundEffect effect;
	protected TrackingSound activeSound;

	protected int repeatDelay = 0;

	public Emitter(@Nonnull final SoundEffect sound) {
		this.effect = sound;
	}

	public void update() {
		// If the volume is turned off don't send
		// down a sound.
		if (SoundSystemConfig.getMasterGain() <= 0)
			return;
		
		// Allocate a new sound to send down if needed
		if(this.activeSound == null)
			this.activeSound = new TrackingSound(effect, true);

		// If it isn't playing send it to the sound engine
		if(!SoundManager.isSoundPlaying(this.activeSound)) {
			try {
				SoundManager.playSound(this.activeSound);
			} catch (final Throwable t) {
				ModLog.error("Unable to play sound", t);;
			}
		}
	}

	public void setVolume(final float volume) {
		if (this.activeSound != null)
			this.activeSound.setVolume(volume);
	}

	public float getVolume() {
		return this.activeSound != null ? this.activeSound.getVolume() : 0.0F;
	}

	public void fade() {
		if (this.activeSound != null) {
			ModLog.debug("FADE: " + this.activeSound.toString());
			this.activeSound.fadeAway();
		}
	}
}
