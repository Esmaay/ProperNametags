package be.esmay.propernametags.common.listeners;

import be.esmay.propernametags.ProperNametags;
import be.esmay.propernametags.api.objects.ProperNameTag;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.api.TabAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

@RequiredArgsConstructor
public final class PlayerJoinListener implements Listener {

    private final ProperNametags properNametags;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!this.properNametags.isNameTagVisible()) return;

        for (Player player : event.getPlayer().getWorld().getNearbyEntities(event.getPlayer().getLocation(), 48, 48, 48).stream().filter(entity -> entity instanceof Player).map(entity -> (Player) entity).toList()) {
            if (player.getUniqueId().toString().equalsIgnoreCase(event.getPlayer().getUniqueId().toString())) continue;

            this.properNametags.sendNameTag(event.getPlayer(), player, true);
            this.properNametags.sendNameTag(player, event.getPlayer(), true);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            Bukkit.getScheduler().runTaskLater(this.properNametags, () -> {
                TabAPI.getInstance().getNameTagManager().hideNameTag(TabAPI.getInstance().getPlayer(event.getPlayer().getUniqueId()));
            }, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!this.properNametags.isNameTagVisible()) return;
        this.properNametags.getQuittingPlayers().asMap().put(event.getPlayer().getUniqueId(), System.currentTimeMillis());

        List<ProperNameTag> nameTagsToRemove = this.properNametags.getNameTags().stream()
                .filter(nameTag -> nameTag.getPlayer().equals(event.getPlayer().getUniqueId()) ||
                        nameTag.getViewer().equals(event.getPlayer().getUniqueId()))
                .toList();

        nameTagsToRemove.forEach(this.properNametags::removeNameTag);
    }
}
