package be.esmay.propernametags.utils;

import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public final class ChatUtils {

    private final static Pattern PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    public static String format(String message) {
        Matcher match = PATTERN.matcher(message);

        while (match.find()) {
            String colour = message.substring(match.start(), match.end());
            message = message.replace(colour, ChatColor.of(colour.substring(1)) + "");
            match = PATTERN.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

}
