package be.esmay.propernametags.commands;

import be.esmay.propernametags.ProperNametags;
import be.esmay.propernametags.api.configuration.DefaultConfiguration;
import be.esmay.propernametags.utils.ChatUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class ProperNametagsCommand implements CommandExecutor {

    private final ProperNametags plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            commandSender.sendMessage(ChatUtils.format("<red>Hey! Gerbuik /propernametags reload jij!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.setDefaultConfiguration(new DefaultConfiguration(this.plugin));
            commandSender.sendMessage(ChatUtils.format("<gray>Reloaded!"));
        }
        return true;
    }

}
