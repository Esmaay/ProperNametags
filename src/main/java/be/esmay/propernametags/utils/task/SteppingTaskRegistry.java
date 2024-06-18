package be.esmay.propernametags.utils.task;

import be.esmay.propernametags.ProperNametags;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@UtilityClass
public final class SteppingTaskRegistry {

    private static final Map<ISteppingTask, BukkitTask> REGISTERED_TASKS = new HashMap<>();
    private static int totalMsPerTickMax = 0;

    public static void register(ISteppingTask iSteppingTask) {
        StepInfo stepInfo = iSteppingTask.getClass().getAnnotation(StepInfo.class);
        if (stepInfo == null) throw new IllegalStateException("SteppingTask " + iSteppingTask.getClass().getName() + " does not have a StepInfo annotation");

        SteppingTask steppingTask = new SteppingTask(iSteppingTask);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(ProperNametags.getInstance(), steppingTask, stepInfo.pollDelay(), stepInfo.pollPeriod());
        REGISTERED_TASKS.put(iSteppingTask, bukkitTask);
        totalMsPerTickMax += iSteppingTask.getMaxMsPerTick();

        ProperNametags.getInstance().getLogger().log(Level.INFO, () -> "Registered SteppingTask " + iSteppingTask.getClass()
                .getSimpleName() + ". Total MS-per-tick: " + totalMsPerTickMax + ".");
    }

    public static void unregister(ISteppingTask iSteppingTask) {
        BukkitTask task = REGISTERED_TASKS.remove(iSteppingTask);
        if (task != null) task.cancel();
    }
}

