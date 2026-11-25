package info.mudbourn.mmsdungeons.mobs.kobolds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

// Runs actions after a number of server ticks.
public final class KoboldWork {

    private static final List<Task> TASKS = new ArrayList<>();

    private KoboldWork() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TASKS.clear());
    }

    public static void queue(int ticks, Runnable action) {
        TASKS.add(new Task(ticks, action));
    }

    private static void tick() {
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            task.ticks--;
            if (task.ticks <= 0) {
                due.add(task.action);
                iterator.remove();
            }
        }
        due.forEach(Runnable::run);
    }

    private static final class Task {

        private int ticks;
        private final Runnable action;

        private Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}
