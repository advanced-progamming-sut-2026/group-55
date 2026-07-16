package pvz.model.core;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private long currentTick = 0;
    private final List<Updatable> updatables = new ArrayList<>();

    public void register(Updatable u) {
        updatables.add(u);
    }

    public void advance(long ticks) {
        for (long i = 0; i < ticks; i++) {
            currentTick++;
            for (Updatable u : updatables) {
                u.update(currentTick);
            }
        }
    }

    public long getCurrentTick() {
        return currentTick;
    }
}