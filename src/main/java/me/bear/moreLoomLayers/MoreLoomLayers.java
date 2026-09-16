package me.bear.moreLoomLayers;

import me.bear.moreLoomLayers.config.PersistentPatternConfig;
import me.bear.moreLoomLayers.listeners.CraftingListener;
import me.bear.moreLoomLayers.listeners.LoomListener;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;


public final class MoreLoomLayers extends JavaPlugin {

    private static MoreLoomLayers instance;
    private NamespacedKey patternDataKey;

    @Override
    public void onEnable() {
        instance = this;
        patternDataKey = new NamespacedKey(this, "extended_patterns");

        // Configuration management
        new PersistentPatternConfig(this);

        getServer().getPluginManager().registerEvents(new LoomListener(this, patternDataKey), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(), this);

        //Objects.requireNonNull(this.getCommand("showbannerlayers")).setExecutor(new BannerLayerViewerCommand());

        getLogger().info("Â§aMoreLoomLayers enabled. Allowing up to 16 banner patterns!!!");
    }

    public static MoreLoomLayers getInstance() {
        return instance;
    }

    public NamespacedKey getPatternDataKey() {
        return patternDataKey;
    }

    @Override
    public void onDisable() {
        getLogger().info("Â§9MoreLoomLayers disabled.");
    }

}
