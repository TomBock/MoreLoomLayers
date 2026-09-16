package me.bear.moreLoomLayers;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import me.bear.moreLoomLayers.commands.BannerLayerViewerCommand;
import me.bear.moreLoomLayers.config.PersistentPatternConfig;
import me.bear.moreLoomLayers.listeners.CraftingListener;
import me.bear.moreLoomLayers.listeners.LoomListener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MoreLoomLayers extends JavaPlugin {

	private PersistentPatternConfig patternConfig;
	private ExtendedPatterns extendedPatterns;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		patternConfig = new PersistentPatternConfig(this);
		extendedPatterns = new ExtendedPatterns(this, patternConfig);

		if (getServer().getPluginManager().getPermission(BannerLayerViewerCommand.PERMISSION) == null) {
			getServer().getPluginManager().addPermission(new Permission(BannerLayerViewerCommand.PERMISSION,
					"Allows viewing the layers of a held banner", PermissionDefault.TRUE));
		}

		getServer().getPluginManager().registerEvents(new LoomListener(this, extendedPatterns), this);
		getServer().getPluginManager().registerEvents(new CraftingListener(this, extendedPatterns), this);

		// paper-plugin.yml has no commands section, so commands go through the
		// Brigadier lifecycle API.
		getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
				event.registrar().register("showbannerlayers",
						"Shows the layers of the banner in your main hand",
						List.of("sbl"),
						new BannerLayerViewerCommand(extendedPatterns, patternConfig)));

		getLogger().info("MoreLoomLayers enabled, allowing up to " + ExtendedPatterns.MAX_PATTERNS + " banner layers.");
	}

	@Override
	public void onDisable() {
		getLogger().info("MoreLoomLayers disabled.");
	}
}
