package be.esmay.propernametags.common.listeners;

import be.esmay.propernametags.ProperNametags;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

@RequiredArgsConstructor
public final class PlayerSneakListener implements Listener {

    private final ProperNametags properNametags;

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        this.properNametags.getNameTags().stream()
                .filter(nameTag -> nameTag.getPlayer().equals(event.getPlayer().getUniqueId()))
                .forEach(nameTag -> this.properNametags.sendMetaData(nameTag, event.isSneaking()));
    }
}
