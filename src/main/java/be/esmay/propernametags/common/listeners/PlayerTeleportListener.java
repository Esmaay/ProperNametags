package be.esmay.propernametags.common.listeners;

import be.esmay.propernametags.ProperNametags;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

@RequiredArgsConstructor
public final class PlayerTeleportListener implements Listener {

    private final ProperNametags properNametags;

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!this.properNametags.isNameTagVisible()) return;

        this.properNametags.getTeleportingPlayers().asMap().put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
