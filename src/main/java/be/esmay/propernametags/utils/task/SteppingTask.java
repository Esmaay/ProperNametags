package be.esmay.propernametags.utils.task;

import lombok.Getter;

public final class SteppingTask implements Runnable {

    private final ISteppingTask task;
    @Getter private boolean running = false;

    public SteppingTask(ISteppingTask task) {
        this.task = task;
    }

    @Override
    public void run() {
        if (!this.running) {
            if (this.task.shouldStart()) {
                this.task.start();
                this.running = true;
            }

            return;
        }

        long startMs = System.currentTimeMillis();
        while (startMs + this.task.getMaxMsPerTick() > System.currentTimeMillis()) {
            if (this.task.shouldStep()) this.task.step();
            if (this.task.isDone()) {
                this.running = false;
                return;
            }
        }
    }

}
