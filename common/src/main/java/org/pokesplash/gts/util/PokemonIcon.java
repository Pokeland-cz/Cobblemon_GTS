package org.pokesplash.gts.util;

import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import org.pokesplash.gts.Gts;

/**
 * Creates the item shown for a Pokemon in the GTS menus.
 */
public abstract class PokemonIcon {

	// Highest national dex number the icon pack has an icon for.
	private static final int MAX_ICON_DEX = 1025;

	/**
	 * Returns the Pokemon model, or a species icon from a resource pack if usePackIcons is enabled.
	 * The icon pack (same format as Hunt and Cobblemon Bingo use) overrides the configured item with
	 * custom_model_data = national dex number, and 0 as a fallback icon. Forms use the base species icon.
	 * @param pokemon The Pokemon to create the item for.
	 * @return The item to display.
	 */
	public static ItemStack of(Pokemon pokemon) {
		if (Gts.config == null || !Gts.config.isUsePackIcons()) {
			return PokemonItem.from(pokemon, 1);
		}

		Species species = pokemon.getSpecies();
		int dexNumber = species.getNationalPokedexNumber();
		boolean hasIcon = !species.getLabels().contains("custom") && dexNumber > 0 && dexNumber <= MAX_ICON_DEX;

		ItemStack stack = packIconItem();
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(hasIcon ? dexNumber : 0));
		return stack;
	}

	private static ItemStack packIconItem() {
		try {
			ItemStack stack = Utils.parseItemId(Gts.config.getPackIconItem());
			if (!stack.isEmpty()) {
				return stack;
			}
		} catch (Exception ignored) {
		}
		Gts.LOGGER.error("Invalid packIconItem in GTS config: " + Gts.config.getPackIconItem());
		return new ItemStack(Items.GRAY_DYE);
	}
}
