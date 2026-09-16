package me.bear.moreLoomLayers.banner;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.bear.moreLoomLayers.config.PersistentPatternConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hides the layers beyond the loom limit inside the banner persistent data and
 * merges them back once the banner leaves the loom.
 *
 * <p>Layers are stored by registry key ("RED|minecraft:stripe_bottom"), so the data
 * stays valid across config edits, Minecraft updates and datapack patterns.</p>
 */
public final class ExtendedPatterns {

	/** Hard cap on layers per banner; the client only renders this many. */
	public static final int MAX_PATTERNS = 16;
	/** Number of layers the loom is allowed to see, so it still offers to add one more. */
	public static final int VISIBLE_PATTERNS = 5;
	/** Layers the vanilla tooltip renders by itself; everything above gets lore. */
	private static final int VANILLA_TOOLTIP_LAYERS = 6;

	private static final String SEPARATOR = "|";

	private final JavaPlugin plugin;
	private final PersistentPatternConfig patternConfig;
	private final NamespacedKey patternDataKey;
	private final NamespacedKey loreLineCountKey;

	public ExtendedPatterns(JavaPlugin plugin, PersistentPatternConfig patternConfig) {
		this.plugin = plugin;
		this.patternConfig = patternConfig;
		this.patternDataKey = new NamespacedKey(plugin, "extended_patterns");
		this.loreLineCountKey = new NamespacedKey(plugin, "extended_lore_lines");
	}

	public static boolean isBanner(ItemStack item) {
		return item != null && Tag.ITEMS_BANNERS.isTagged(item.getType());
	}

	/**
	 * Every layer of the banner: the hidden ones plus whatever the loom added on top.
	 * Works for banners with and without stored data.
	 */
	public List<Pattern> allPatterns(BannerMeta meta) {
		List<Pattern> visible = meta.getPatterns();
		List<Pattern> stored = readStored(meta.getPersistentDataContainer());
		if (stored.isEmpty()) {
			return new ArrayList<>(visible);
		}

		List<Pattern> all = new ArrayList<>(stored);
		// Anything past the hidden prefix was appended by the loom while the layer
		// count was masked, so it belongs at the end.
		for (int i = VISIBLE_PATTERNS; i < visible.size(); i++) {
			all.add(visible.get(i));
		}
		return all;
	}

	/**
	 * Masks the banner down to {@link #VISIBLE_PATTERNS} layers so the loom accepts
	 * another one. Safe to call repeatedly; already hidden layers are kept.
	 */
	public void hide(ItemStack banner) {
		if (!isBanner(banner) || !(banner.getItemMeta() instanceof BannerMeta meta)) {
			return;
		}

		List<Pattern> all = capped(allPatterns(meta));
		if (all.size() <= VISIBLE_PATTERNS) {
			// Nothing to hide - drop leftover data so the banner stays a plain item.
			if (clearStoredData(meta)) {
				removeGeneratedLore(meta);
				meta.setPatterns(all);
				banner.setItemMeta(meta);
			}
			return;
		}

		writeStored(meta, all);
		removeGeneratedLore(meta);
		meta.setPatterns(new ArrayList<>(all.subList(0, VISIBLE_PATTERNS)));
		banner.setItemMeta(meta);
	}

	/**
	 * Puts the hidden layers back on the banner and rebuilds the lore that emulates
	 * the vanilla tooltip.
	 */
	public void restore(ItemStack banner) {
		if (!isBanner(banner) || !(banner.getItemMeta() instanceof BannerMeta meta)) {
			return;
		}
		if (!hasStoredData(meta.getPersistentDataContainer())) {
			return;
		}

		List<Pattern> all = capped(allPatterns(meta));
		clearStoredData(meta);
		meta.setPatterns(all);
		applyGeneratedLore(meta, all);
		banner.setItemMeta(meta);
	}

	private static List<Pattern> capped(List<Pattern> patterns) {
		return patterns.size() > MAX_PATTERNS
				? new ArrayList<>(patterns.subList(0, MAX_PATTERNS))
				: patterns;
	}

	// ---------------------------------------------------------------- persistence

	private boolean hasStoredData(PersistentDataContainer pdc) {
		return pdc.has(patternDataKey);
	}

	private void writeStored(BannerMeta meta, List<Pattern> patterns) {
		Registry<PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
		List<String> encoded = new ArrayList<>(patterns.size());
		for (Pattern pattern : patterns) {
			encoded.add(pattern.getColor().name() + SEPARATOR + registry.getKeyOrThrow(pattern.getPattern()).asString());
		}
		meta.getPersistentDataContainer().set(patternDataKey, PersistentDataType.LIST.strings(), encoded);
	}

	private List<Pattern> readStored(PersistentDataContainer pdc) {
		if (!pdc.has(patternDataKey, PersistentDataType.LIST.strings())) {
			return readLegacy(pdc);
		}

		List<String> encoded = pdc.get(patternDataKey, PersistentDataType.LIST.strings());
		if (encoded == null) {
			return List.of();
		}

		Registry<PatternType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
		List<Pattern> patterns = new ArrayList<>(encoded.size());
		for (String entry : encoded) {
			int split = entry.indexOf(SEPARATOR);
			if (split < 0) {
				plugin.getLogger().warning("Dropping malformed stored banner layer: " + entry);
				continue;
			}

			DyeColor color = parseColor(entry.substring(0, split));
			NamespacedKey key = NamespacedKey.fromString(entry.substring(split + 1));
			PatternType type = key == null ? null : registry.get(key);
			if (color == null || type == null) {
				plugin.getLogger().warning("Dropping unknown banner layer: " + entry);
				continue;
			}
			patterns.add(new Pattern(color, type));
		}
		return patterns;
	}

	private static DyeColor parseColor(String name) {
		try {
			return DyeColor.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Reads the pre-2.0 format: an int count followed by one colour byte and a
	 * big-endian ordinal short per layer, with the ordinals defined in config.yml.
	 */
	private List<Pattern> readLegacy(PersistentDataContainer pdc) {
		byte[] data = pdc.get(patternDataKey, PersistentDataType.BYTE_ARRAY);
		if (data == null || data.length < 4) {
			return List.of();
		}

		int count = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
		DyeColor[] colors = DyeColor.values();
		List<Pattern> patterns = new ArrayList<>();
		int index = 4;
		for (int i = 0; i < count && index + 3 <= data.length; i++) {
			int colorOrdinal = data[index++] & 0xFF;
			short patternOrdinal = (short) (((data[index++] & 0xFF) << 8) | (data[index++] & 0xFF));

			PatternType type = patternConfig.getLegacyPatternByOrdinal(patternOrdinal);
			if (colorOrdinal >= colors.length || type == null) {
				plugin.getLogger().warning("Dropping unreadable legacy banner layer (ordinal " + patternOrdinal + ")");
				continue;
			}
			patterns.add(new Pattern(colors[colorOrdinal], type));
		}
		return patterns;
	}

	private boolean clearStoredData(BannerMeta meta) {
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		if (!pdc.has(patternDataKey)) {
			return false;
		}
		pdc.remove(patternDataKey);
		return true;
	}

	// ---------------------------------------------------------------------- lore

	/**
	 * Appends one gray line per layer the vanilla tooltip leaves out and remembers how
	 * many lines belong to the plugin, so lore from other plugins survives.
	 */
	private void applyGeneratedLore(BannerMeta meta, List<Pattern> patterns) {
		removeGeneratedLore(meta);
		if (patterns.size() <= VANILLA_TOOLTIP_LAYERS) {
			return;
		}

		List<Component> existing = meta.lore();
		List<Component> lore = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
		for (int i = VANILLA_TOOLTIP_LAYERS; i < patterns.size(); i++) {
			Pattern pattern = patterns.get(i);
			lore.add(Component.text(prettifyColor(pattern.getColor()) + " " + patternConfig.getName(pattern.getPattern()))
					.color(NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);
		meta.getPersistentDataContainer().set(loreLineCountKey, PersistentDataType.INTEGER,
				patterns.size() - VANILLA_TOOLTIP_LAYERS);
	}

	private void removeGeneratedLore(BannerMeta meta) {
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		Integer generated = pdc.get(loreLineCountKey, PersistentDataType.INTEGER);
		pdc.remove(loreLineCountKey);
		if (generated == null || generated <= 0) {
			return;
		}

		List<Component> lore = meta.lore();
		if (lore == null || lore.isEmpty()) {
			return;
		}

		// The generated lines are always the tail of the lore.
		int keep = Math.max(0, lore.size() - generated);
		meta.lore(keep == 0 ? null : new ArrayList<>(lore.subList(0, keep)));
	}

	/** "LIGHT_BLUE" to "Light Blue" */
	private static String prettifyColor(DyeColor color) {
		StringBuilder sb = new StringBuilder();
		for (String word : color.name().split("_")) {
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
