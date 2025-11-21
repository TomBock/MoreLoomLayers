package me.bear.moreLoomLayers.config;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages persistent storage of pattern ordinals for banners in the config.yml.
 */
public class PersistentPatternConfig {

	private static JavaPlugin plugin;
	// Lookup map of pattern ordinals to their corresponding NamespacedKey
	private static Map<Short, NamespacedKey> patternById = new HashMap<>();

	public PersistentPatternConfig(JavaPlugin plugin) {
		PersistentPatternConfig.plugin = plugin;

		plugin.reloadConfig();
		updatePatternConfig();
		buildPatternMap();
	}

	/**
	 * Updates the configuration file to ensure all pattern types have an assigned ordinal.
	 * New pattern types are added with the next available ordinal.
	 */
	private void updatePatternConfig() {
		ConfigurationSection section = plugin.getConfig().getConfigurationSection("patterns");
		AtomicInteger count = new AtomicInteger(section == null ? 0 : section.getKeys(false).size());

		Registry<@org.jetbrains.annotations.NotNull PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);

		registry.keyStream()
				.sorted(Comparator.comparing(NamespacedKey::asString))
				.forEach(key -> {
			String configPath = "patterns." + key.asString();
			if (!plugin.getConfig().isSet(configPath)) {
				plugin.getConfig().set(configPath, count.getAndIncrement());

				// also adds the translation entry which must be edited manually later
				plugin.getConfig().set("names." + key.asString(), key.asString());
			}
		});

		plugin.saveConfig();
	}

	/**
	 * Builds a map of pattern ordinals to their corresponding PatternType.
	 */
	private void buildPatternMap() {
		ConfigurationSection section = plugin.getConfig().getConfigurationSection("patterns");
		assert section != null;
		section.getKeys(false).forEach(key -> {
			short ordinal = (short) section.getInt(key);
			patternById.put(ordinal, NamespacedKey.fromString(key));
		});
	}

	public static short getOrdinal(PatternType patternType) {
		NamespacedKey key = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getKeyOrThrow(patternType);
		return (short) plugin.getConfig().getInt("patterns." + key, -1);
	}

	public static PatternType getPatternByOrdinal(short ordinal) {
		Registry<@org.jetbrains.annotations.NotNull PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
		NamespacedKey key = patternById.get(ordinal);
		return registry.get(key);
	}

	public static String getName(PatternType patternType) {
		Registry<@org.jetbrains.annotations.NotNull PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
		return plugin.getConfig().getString("names." + registry.getKeyOrThrow(patternType).asString(), "Unknown");
	}
}
