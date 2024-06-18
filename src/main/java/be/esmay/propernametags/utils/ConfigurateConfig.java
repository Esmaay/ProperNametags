package be.esmay.propernametags.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;

@Getter
public abstract class ConfigurateConfig {

    protected final YamlConfigurationLoader loader;
    protected CommentedConfigurationNode rootNode;

    public ConfigurateConfig(Plugin plugin, String name) {
        this.loader = YamlConfigurationLoader.builder()
                .path(plugin.getDataFolder().toPath().resolve(name))
                .indent(2)
                .nodeStyle(NodeStyle.BLOCK)
                .headerMode(HeaderMode.PRESET)
                .defaultOptions(options -> {
                    options = options
                            .mapFactory(MapFactories.sortedNatural());

                    return options;
                })
                .build();

        try {
            this.rootNode = this.loader.load();
        } catch (IOException e) {
            Bukkit.getLogger().warning("An error occurred while loading this configuration: " + e.getMessage());
        }
    }

    public void saveConfiguration() {
        try {
            this.loader.save(this.rootNode);
        } catch (final ConfigurateException e) {
            Bukkit.getLogger().warning("Unable to save your messages configuration! Sorry! " + e.getMessage());
        }
    }
}