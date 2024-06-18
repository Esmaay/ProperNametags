package be.esmay.propernametags.common.tasks;

import be.esmay.propernametags.ProperNametags;
import be.esmay.propernametags.utils.task.ISteppingTask;
import be.esmay.propernametags.utils.task.StepInfo;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


@RequiredArgsConstructor
@StepInfo(pollPeriod = 1L)
public final class UpdateVisibilityTask implements ISteppingTask {

    private final ProperNametags properNametags;

    private Player[] currentlyChecking;
    private int index;

    @Override
    public void start() {
        this.currentlyChecking = new Player[this.properNametags.getServer().getOnlinePlayers().size()];
        this.properNametags.getServer().getOnlinePlayers().toArray(this.currentlyChecking);
        this.index = 0;
    }

    public void step() {
        Player player = this.currentlyChecking[this.index];
        if (player == null) {
            this.currentlyChecking[this.index++] = null;
            return;
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(viewer.getUniqueId())) continue;

            if (!viewer.canSee(player)) {
                if (!this.properNametags.hasNameTag(player, viewer)) continue;

                this.properNametags.removeNameTag(player, viewer);
                continue;
            }

            if (player.getLocation().distanceSquared(viewer.getLocation()) >= 48 * 48) {
                if (!this.properNametags.hasNameTag(player, viewer)) continue;

                this.properNametags.removeNameTag(player, viewer);
                continue;
            }

            if (this.properNametags.hasNameTag(player, viewer)) continue;

            this.properNametags.sendNameTag(player, viewer, false);
        }

        this.currentlyChecking[this.index++] = null;
    }

    @Override
    public boolean shouldStart() {
        return !this.properNametags.getServer().getOnlinePlayers().isEmpty();
    }

    @Override
    public boolean shouldStep() {
        return this.index < this.currentlyChecking.length;
    }

    @Override
    public boolean isDone() {
        return this.index == this.currentlyChecking.length;
    }

    @Override
    public int getMaxMsPerTick() {
        return 3;
    }

}
