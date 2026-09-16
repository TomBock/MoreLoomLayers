package me.bear.moreLoomLayers.listeners;

import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LoomListener implements Listener {

    private final JavaPlugin plugin;
    private final ExtendedPatterns patterns;

    // Loom inventory slots as per current version:
    // Slot 0: Banner input
    // Slot 1: Dye input
    // Slot 2: Pattern input (like a banner pattern item)
    // Slot 3: Output slot

    public LoomListener(JavaPlugin plugin, ExtendedPatterns patterns) {
        this.plugin = plugin;
        this.patterns = patterns;
    }

    @EventHandler
    public void onLoomClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getType() == InventoryType.LOOM)) {
            return; // Not a loom
        }

        Inventory loomInv = event.getView().getTopInventory();
        if (loomInv.getType() != InventoryType.LOOM) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot == 3 && event.getAction() == InventoryAction.PICKUP_ALL) {
            ItemStack output = loomInv.getItem(3);
            if (output == null || output.getType() == Material.AIR) {
                return;
            }
            patterns.restore(output);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack banner = loomInv.getItem(0);
                if (banner != null) {
                    patterns.hide(banner);
                }
            }, 1L);
        }
        if (rawSlot == 0 && isRemovingBannerFromSlot(event)) {
            // The player is attempting to take the banner out.
            ItemStack banner = loomInv.getItem(0);
            if (banner == null || banner.getType() == Material.AIR) {
                return;
            }
            patterns.restore(banner);
        }
    }

    private static boolean isRemovingBannerFromSlot(InventoryClickEvent event) {
        return switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, SWAP_WITH_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY ->
                    true;
            default -> false;
        };
    }

    @EventHandler
    public void onLoomClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getType() == InventoryType.LOOM)) {
            return; // Not a loom
        }

        Inventory loomInv = event.getView().getTopInventory();

        if (loomInv.getType() != InventoryType.LOOM) {
            return;
        }

        ItemStack banner = loomInv.getItem(0);
        if (banner == null || banner.getType() == Material.AIR) {
            return;
        }

        patterns.restore(banner);
    }
}
