package be.esmay.propernametags.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public final class ChatUtils {

    private final static Pattern PATTERN = Pattern.compile("#[a-fA-F0-9]{6}|&#[a-fA-F0-9]{6}");

    public static Component format(String message) {
        MiniMessage extendedInstance = MiniMessage.builder()
                .editTags(tags -> {
                    tags.resolver(TagResolver.resolver("primary", Tag.styling(TextColor.fromHexString("#81ACF1"))));
                    tags.resolver(TagResolver.resolver("secondary", Tag.styling(TextColor.fromHexString("#4888F3"))));
                    tags.resolver(TagResolver.resolver("error", Tag.styling(TextColor.fromHexString("#FC3838"))));
                    tags.resolver(TagResolver.resolver("primary_red", Tag.styling(TextColor.fromHexString("#FF7171"))));
                    tags.resolver(TagResolver.resolver("secondary_red", Tag.styling(TextColor.fromHexString("#FF4343"))));
                }).build();

        return extendedInstance.deserialize(message);
    }

}
