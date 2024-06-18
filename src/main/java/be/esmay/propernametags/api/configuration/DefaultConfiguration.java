package be.esmay.propernametags.api.configuration;

import be.esmay.propernametags.ProperNametags;
import be.esmay.propernametags.utils.ConfigurateConfig;
import lombok.Getter;

@Getter
public final class DefaultConfiguration extends ConfigurateConfig {

    private final String prefix;
    private final String suffix;
    private final String name;

    public DefaultConfiguration(ProperNametags plugin) {
        super(plugin, "config.yml");

        this.prefix = this.rootNode.node("tag", "prefix").getString("Hey!");
        this.suffix = this.rootNode.node("tag", "suffix").getString("");
        this.name = this.rootNode.node("tag", "name").getString("%player%");

        this.saveConfiguration();
    }
}
