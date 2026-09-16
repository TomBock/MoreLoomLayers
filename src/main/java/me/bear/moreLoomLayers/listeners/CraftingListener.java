package me.bear.moreLoomLayers.listeners;

import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Vanilla refuses to copy a banner once it carries more layers than the loom allows,
 * so the copy recipe is reimplemented for those banners: one patterned banner plus one
 * blank banner of the same colour yields a copy and consumes the blank banner.
 */
public class CraftingListener implements Listener {

	private final JavaPlugin plugin;
	private final ExtendedPatterns patterns;

	public CraftingListener(JavaPlugin plugin, ExtendedPatterns patterns) {
		this.plugin = plugin;
		this.patterns = patterns;
	}

	/** Slot indices of the two banners taking part in the copy. */
	private record Duplication(int patternedSlot, int blankSlot) {
	}

	@EventHandler
	public void onPrepareCrafting(PrepareItemCraftEvent event) {
		CraftingInventory inventory = event.getInventory();

		ItemStack result = inventory.getResult();
		if (result != null && result.getType() != Material.AIR) {
			return; // a real recipe matched, leave it alone
		}

		ItemStack[] matrix = inventory.getMatrix();
		Duplication duplication = findDuplication(matrix);
		if (duplication == null) {
			return;
		}

		ItemStack copy = matrix[duplication.patternedSlot()].clone();
		copy.setAmount(1);
		inventory.setResult(copy);
	}

	@EventHandler
	public void onClicked(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof CraftingInventory inventory)) {
			return;
		}
		if (event.getSlotType() != InventoryType.SlotType.RESULT) {
			return;
		}
		if (inventory.getRecipe() != null) {
			return; // the server matched a recipe, so it handles the ingredients itself
		}

		ItemStack result = inventory.getResult();
		if (!ExtendedPatterns.isBanner(result)) {
			return;
		}

		// Only take over for the result this listener injected; anything else keeps
		// its normal behaviour, so no ingredients can be left unconsumed.
		ItemStack[] matrix = inventory.getMatrix();
		Duplication duplication = findDuplication(matrix);
		if (duplication == null) {
			return;
		}

		event.setCancelled(true);

		ItemStack blank = matrix[duplication.blankSlot()];
		if (blank.getAmount() > 1) {
			blank.setAmount(blank.getAmount() - 1);
		} else {
			matrix[duplication.blankSlot()] = null;
			inventory.setResult(null);
		}
		inventory.setMatrix(matrix);

		ItemStack copy = matrix[duplication.patternedSlot()].clone();
		copy.setAmount(1);

		HumanEntity who = event.getWhoClicked();
		if (!event.isShiftClick() && who.getItemOnCursor().getType() == Material.AIR) {
			who.setItemOnCursor(copy);
		} else {
			giveOrDrop(who, copy);
		}

		// The matrix was edited behind the client, so make sure it sees the same thing.
		if (who instanceof Player player) {
			player.getScheduler().runDelayed(plugin, task -> player.updateInventory(), null, 1L);
		}
	}

	/**
	 * Matches exactly one patterned banner plus one blank banner of the same colour and
	 * nothing else, and only for banners vanilla itself will not copy.
	 */
	private Duplication findDuplication(ItemStack[] matrix) {
		int patternedSlot = -1;
		int blankSlot = -1;

		for (int i = 0; i < matrix.length; i++) {
			ItemStack item = matrix[i];
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			if (!ExtendedPatterns.isBanner(item) || !(item.getItemMeta() instanceof BannerMeta meta)) {
				return null; // any other ingredient means this is not the copy recipe
			}

			if (meta.getPatterns().isEmpty() && !meta.getPersistentDataContainer().isEmpty()) {
				return null; // blank looking, but carrying data - do not touch it
			}

			if (meta.getPatterns().isEmpty()) {
				if (blankSlot != -1) {
					return null;
				}
				blankSlot = i;
			} else {
				if (patternedSlot != -1) {
					return null;
				}
				// A stack would make the vanilla remainder rules ambiguous.
				if (item.getAmount() != 1) {
					return null;
				}
				if (!(patterns.allPatterns(meta).size() > ExtendedPatterns.VISIBLE_PATTERNS)) {
					return null; // vanilla can copy this one on its own
				}
				patternedSlot = i;
			}
		}

		if (patternedSlot == -1 || blankSlot == -1) {
			return null;
		}
		if (matrix[patternedSlot].getType() != matrix[blankSlot].getType()) {
			return null;
		}
		return new Duplication(patternedSlot, blankSlot);
	}

	private static void giveOrDrop(HumanEntity who, ItemStack give) {
		Map<Integer, ItemStack> leftover = who.getInventory().addItem(give);
		leftover.values().forEach(item -> who.getWorld().dropItemNaturally(who.getLocation(), item));
	}
}
