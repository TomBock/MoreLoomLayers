package me.bear.moreLoomLayers.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import me.bear.moreLoomLayers.config.PersistentPatternConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Shows every layer of the held banner as a separate banner in a preview inventory. */
public class BannerLayerViewerCommand implements BasicCommand {

	public static final String PERMISSION = "moreloomlayers.showlayers";

	private final ExtendedPatterns patterns;
	private final PersistentPatternConfig patternConfig;

	public BannerLayerViewerCommand(ExtendedPatterns patterns, PersistentPatternConfig patternConfig) {
		this.patterns = patterns;
		this.patternConfig = patternConfig;
	}

	@Override
	public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
		if (!(source.getSender() instanceof Player player)) {
			source.getSender().sendMessage(Component.text("This command can only be used by a player.", NamedTextColor.RED));
			return;
		}

		ItemStack held = player.getInventory().getItemInMainHand();
		if (!ExtendedPatterns.isBanner(held) || !(held.getItemMeta() instanceof BannerMeta meta)) {
			player.sendMessage(Component.text("Hold a banner in your main hand first.", NamedTextColor.RED));
			return;
		}

		List<Pattern> layers = patterns.allPatterns(meta);
		if (layers.isEmpty()) {
			player.sendMessage(Component.text("That banner has no layers.", NamedTextColor.RED));
			return;
		}

		openLayerView(player, held, layers);
	}

	@Override
	public @NotNull String permission() {
		return PERMISSION;
	}

	private void openLayerView(Player player, ItemStack banner, List<Pattern> layers) {
		int size = Math.max(9, ((layers.size() + 8) / 9) * 9);
		Inventory view = Bukkit.createInventory(player, size, Component.text("Banner Layers"));

		for (int i = 0; i < layers.size(); i++) {
			Pattern layer = layers.get(i);
			ItemStack item = new ItemStack(banner.getType());
			if (!(item.getItemMeta() instanceof BannerMeta itemMeta)) {
				continue;
			}

			itemMeta.addPattern(layer);
			itemMeta.customName(Component.text((i + 1) + ". " + patternConfig.getName(layer.getPattern()))
					.color(NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(itemMeta);
			view.setItem(i, item);
		}

		player.openInventory(view);
	}
}
