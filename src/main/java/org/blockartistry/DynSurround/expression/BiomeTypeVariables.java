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
package org.blockartistry.DynSurround.expression;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;

import org.blockartistry.DynSurround.client.handlers.EnvironStateHandler.EnvironState;
import org.blockartistry.lib.expression.Dynamic;
import org.blockartistry.lib.expression.DynamicVariantList;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class BiomeTypeVariables extends DynamicVariantList {

	public BiomeTypeVariables() {
		// Scan the BiomeDictionary adding the the types
		try {
			final Field accessor = ReflectionHelper.findField(BiomeDictionary.Type.class, "byName");
			if (accessor != null) {
				@SuppressWarnings("unchecked")
				final Map<String, BiomeDictionary.Type> stuff = (Map<String, BiomeDictionary.Type>) accessor.get(null);
				for (final Entry<String, Type> e : stuff.entrySet()) {
					this.add(new Dynamic.DynamicBoolean("biome.is" + e.getKey()) {
						@Override
						public void update() {
							this.value = EnvironState.getTruePlayerBiome().isBiomeType(e.getValue());
						}
					});
				}
			}

		} catch (final Throwable t) {
			throw new RuntimeException("Cannot locate BiomeDictionary.Type table!");
		}

	}
}