package me.bear.moreLoomLayers.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reports which build is actually installed, so that can be answered in game
 * instead of only from the startup line in the console.
 */
public class VersionCommand implements BasicCommand {

	public static final String PERMISSION = "moreloomlayers.version";

	private final JavaPlugin plugin;

	public VersionCommand(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(@NotNull CommandSourceStack source, String @NotNull [] args) {
		CommandSender sender = source.getSender();
		if (args.length > 0 && !args[0].equalsIgnoreCase("version")) {
			sender.sendMessage(Component.text("Usage: /moreloomlayers version", NamedTextColor.RED));
			return;
		}

		sender.sendMessage(Component.text()
				.append(Component.text(plugin.getPluginMeta().getName() + " ", NamedTextColor.GRAY))
				.append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.WHITE))
				.build());
		sender.sendMessage(Component.text("Running on " + Bukkit.getName() + " " + Bukkit.getMinecraftVersion(),
				NamedTextColor.GRAY));
	}

	@Override
	public @NotNull List<String> suggest(@NotNull CommandSourceStack source, String @NotNull [] args) {
		return args.length <= 1 ? List.of("version") : List.of();
	}

	@Override
	public @NotNull String permission() {
		return PERMISSION;
	}
}
