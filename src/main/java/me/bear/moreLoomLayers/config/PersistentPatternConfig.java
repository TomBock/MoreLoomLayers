package me.bear.moreLoomLayers.config;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Display names for banner patterns, plus the lookup table needed to read banners
 * that were stored by the old ordinal based format.
 */
public final class PersistentPatternConfig {

	private final JavaPlugin plugin;
	/** Ordinal -> pattern key, only populated from a legacy "patterns" section. */
	private final Map<Short, NamespacedKey> legacyPatternById = new HashMap<>();

	public PersistentPatternConfig(JavaPlugin plugin) {
		this.plugin = plugin;
		reload();
	}

	public void reload() {
		plugin.reloadConfig();
		addMissingNames();
		buildLegacyOrdinalMap();
	}

	/**
	 * Makes sure every pattern in the registry has a name entry, so new patterns
	 * added by Minecraft or a datapack can be translated by editing the config.
	 */
	private void addMissingNames() {
		Registry<PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
		boolean changed = false;

		for (NamespacedKey key : registry.keyStream().sorted().toList()) {
			String path = "names." + key.asString();
			if (!plugin.getConfig().isSet(path)) {
				plugin.getConfig().set(path, prettify(key));
				changed = true;
			}
		}

		if (changed) {
			plugin.saveConfig();
		}
	}

	private void buildLegacyOrdinalMap() {
		legacyPatternById.clear();
		ConfigurationSection section = plugin.getConfig().getConfigurationSection("patterns");
		if (section == null) {
			return;
		}

		for (String key : section.getKeys(false)) {
			NamespacedKey patternKey = NamespacedKey.fromString(key);
			if (patternKey == null) {
				plugin.getLogger().warning("Ignoring malformed pattern key in config: " + key);
				continue;
			}
			legacyPatternById.put((short) section.getInt(key), patternKey);
		}
	}

	/** Human readable name for the lore lines; falls back to a name derived from the key. */
	public String getName(PatternType patternType) {
		NamespacedKey key = RegistryAccess.registryAccess()
				.getRegistry(RegistryKey.BANNER_PATTERN)
				.getKeyOrThrow(patternType);
		return plugin.getConfig().getString("names." + key.asString(), prettify(key));
	}

	/** Resolves an ordinal written by an older version of the plugin, or null. */
	public PatternType getLegacyPatternByOrdinal(short ordinal) {
		NamespacedKey key = legacyPatternById.get(ordinal);
		if (key == null) {
			return null;
		}
		return RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).get(key);
	}

	/** "minecraft:half_horizontal_bottom" -> "Half Horizontal Bottom" */
	private static String prettify(NamespacedKey key) {
		StringBuilder sb = new StringBuilder();
		for (String word : key.getKey().split("_")) {
			if (word.isEmpty()) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1).toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}
}
