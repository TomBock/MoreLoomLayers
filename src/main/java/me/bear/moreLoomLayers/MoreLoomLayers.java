package me.bear.moreLoomLayers;

import me.bear.moreLoomLayers.banner.ExtendedPatterns;
import me.bear.moreLoomLayers.config.PersistentPatternConfig;
import me.bear.moreLoomLayers.listeners.CraftingListener;
import me.bear.moreLoomLayers.listeners.LoomListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoreLoomLayers extends JavaPlugin {

    private PersistentPatternConfig patternConfig;
    private ExtendedPatterns extendedPatterns;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        patternConfig = new PersistentPatternConfig(this);
        extendedPatterns = new ExtendedPatterns(this, patternConfig);

        getServer().getPluginManager().registerEvents(new LoomListener(this, extendedPatterns), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this, extendedPatterns), this);

        //Objects.requireNonNull(this.getCommand("showbannerlayers")).setExecutor(new BannerLayerViewerCommand());

        getLogger().info("§aMoreLoomLayers enabled. Allowing up to 16 banner patterns!!!");
    }

    @Override
    public void onDisable() {
        getLogger().info("§9MoreLoomLayers disabled.");
    }
}
