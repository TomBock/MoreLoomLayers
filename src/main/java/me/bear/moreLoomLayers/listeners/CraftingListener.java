package me.bear.moreLoomLayers.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.Map;
import java.util.Set;

public class CraftingListener implements Listener {

	private final Set<Material> bannerTypes = Set.of(
			Material.BLACK_BANNER,
			Material.BLUE_BANNER,
			Material.BROWN_BANNER,
			Material.CYAN_BANNER,
			Material.GRAY_BANNER,
			Material.GREEN_BANNER,
			Material.LIGHT_BLUE_BANNER,
			Material.LIGHT_GRAY_BANNER,
			Material.LIME_BANNER,
			Material.MAGENTA_BANNER,
			Material.ORANGE_BANNER,
			Material.PINK_BANNER,
			Material.PURPLE_BANNER,
			Material.RED_BANNER,
			Material.WHITE_BANNER,
			Material.YELLOW_BANNER
	);

	@EventHandler
	public void onPrepareCrafting(PrepareItemCraftEvent event) {
		CraftingInventory inventory = event.getInventory();
		ItemStack result = inventory.getResult();

		// If there's already a result, don't modify it
		if(result != null) {
			return;
		}

		ItemStack[] matrix = inventory.getMatrix();

		ItemStack bannerWithPatterns = null;
		ItemStack blankBanner = null;

		for (ItemStack item : matrix) {
			if(item == null || !bannerTypes.contains(item.getType()))
				continue;

			BannerMeta meta = (BannerMeta) item.getItemMeta();
			if(meta != null && !meta.getPatterns().isEmpty()) {
				if(bannerWithPatterns != null) return; // Don't allow multiple patterned banners
				bannerWithPatterns = item;
			} else {
				if(blankBanner != null) return; // Don't allow multiple blank banners
				blankBanner = item;
			}
		}

		if(bannerWithPatterns != null && blankBanner != null && bannerWithPatterns.getType() == blankBanner.getType()) {
			ItemStack give = bannerWithPatterns.clone();
			give.setAmount(1);
			inventory.setResult(give);
		}
	}

	@EventHandler
	public void onClicked(InventoryClickEvent event) {
		if(!(event.getInventory() instanceof CraftingInventory inventory)) return;
		if(event.getSlotType() != InventoryType.SlotType.RESULT) return;
		ItemStack result = inventory.getResult();

		if(result == null || !bannerTypes.contains(result.getType())) return;
		BannerMeta meta = (BannerMeta) result.getItemMeta();
		if(meta.getPatterns().size() < 6) return;

		event.setCancelled(true);

		// Reduce empty banners in the matrix
		ItemStack bannerWithPatterns = null;
		ItemStack[] matrix = inventory.getMatrix();
		for (int i = 0; i < matrix.length; i++) {
			ItemStack item = matrix[i];
			if(item == null || !bannerTypes.contains(item.getType()))
				continue;
			BannerMeta itemMeta = (BannerMeta) item.getItemMeta();
			if(itemMeta != null && !itemMeta.getPatterns().isEmpty()) {
				bannerWithPatterns = item;
			} else {
				// Reduce amount of blank banners
				if(item.getAmount() > 1) {
					item.setAmount(item.getAmount() - 1);
					matrix[i] = item;
				} else {
					matrix[i] = null;
					inventory.setResult(null);
				}
				inventory.setMatrix(matrix);
			}
		}
		if(bannerWithPatterns == null) return;
		ItemStack give = bannerWithPatterns.clone();
		give.setAmount(1);

		// Give the player the copy to the cursor
		HumanEntity who = event.getWhoClicked();
		if(event.isShiftClick()) {
			giveOrDrop(who, give);
		} else {
			if(who.getItemOnCursor().getType() == Material.AIR) {
				who.setItemOnCursor(give);
			} else {
				giveOrDrop(who, give);
			}
		}
	}

	private static void giveOrDrop(HumanEntity who, ItemStack give) {
		Map<Integer, ItemStack> leftover = who.getInventory().addItem(give);
		leftover.values().forEach(it ->
				who.getWorld().dropItemNaturally(who.getLocation(), it)
		);
	}

}
