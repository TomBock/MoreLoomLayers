package me.bear.moreLoomLayers.listeners;

import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LoomListener implements Listener {

	// Loom inventory slots:
	// 0: banner input, 1: dye input, 2: pattern item input, 3: output
	private static final int BANNER_SLOT = 0;
	private static final int RESULT_SLOT = 3;

	private final JavaPlugin plugin;
	private final ExtendedPatterns patterns;

	public LoomListener(JavaPlugin plugin, ExtendedPatterns patterns) {
		this.plugin = plugin;
		this.patterns = patterns;
	}

	@EventHandler
	public void onLoomClick(InventoryClickEvent event) {
		Inventory loom = event.getView().getTopInventory();
		if (loom.getType() != InventoryType.LOOM) {
			return;
		}

		int rawSlot = event.getRawSlot();
		// Whenever a banner leaves the loom - through the input or the output slot -
		// it has to carry all of its layers again.
		if ((rawSlot == BANNER_SLOT || rawSlot == RESULT_SLOT) && isTakingItem(event)) {
			ItemStack banner = loom.getItem(rawSlot);
			if (banner != null) {
				patterns.restore(banner);
			}
		}

		// The click may also have put a new banner in, or changed the dye. Re-mask one
		// tick later, once the loom has settled its slots.
		scheduleHide(event.getWhoClicked(), loom);
	}

	@EventHandler
	public void onLoomDrag(InventoryDragEvent event) {
		Inventory loom = event.getView().getTopInventory();
		if (loom.getType() != InventoryType.LOOM) {
			return;
		}
		if (!event.getRawSlots().contains(BANNER_SLOT)) {
			return;
		}
		scheduleHide(event.getWhoClicked(), loom);
	}

	@EventHandler
	public void onLoomClose(InventoryCloseEvent event) {
		Inventory loom = event.getView().getTopInventory();
		if (loom.getType() != InventoryType.LOOM) {
			return;
		}

		ItemStack banner = loom.getItem(BANNER_SLOT);
		if (banner != null) {
			patterns.restore(banner);
		}
	}

	/**
	 * Runs on the region that owns the player, so this works on Paper and Folia alike.
	 */
	private void scheduleHide(HumanEntity who, Inventory loom) {
		who.getScheduler().runDelayed(plugin, task -> {
			if (who.getOpenInventory().getTopInventory() != loom) {
				return; // the view is gone, the close handler already dealt with it
			}
			ItemStack banner = loom.getItem(BANNER_SLOT);
			if (banner != null) {
				patterns.hide(banner);
			}
		}, null, 1L);
	}

	private static boolean isTakingItem(InventoryClickEvent event) {
		return switch (event.getAction()) {
			case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, SWAP_WITH_CURSOR,
					DROP_ALL_SLOT, DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP,
					HOTBAR_MOVE_AND_READD, COLLECT_TO_CURSOR -> true;
			default -> false;
		};
	}
}
