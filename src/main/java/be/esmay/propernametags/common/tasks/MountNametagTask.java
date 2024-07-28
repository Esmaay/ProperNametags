package be.esmay.propernametags.common.tasks;

import be.esmay.propernametags.ProperNametags;
import be.esmay.propernametags.api.objects.ProperNameTag;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public final class MountNametagTask extends BukkitRunnable {

    private final ProperNametags properNametags;

    @Override
    public void run() {
        if (!this.properNametags.isNameTagVisible()) return;

        for (ProperNameTag nameTag : this.properNametags.getNameTags()) {
            Player player = Bukkit.getPlayer(nameTag.getPlayer());

            if (player == null) continue;
            if (player.hasMetadata("NPC")) continue;

            this.properNametags.sendMountNameTag(nameTag);
        }
    }
}
