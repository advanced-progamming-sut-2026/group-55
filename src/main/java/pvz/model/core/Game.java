package pvz.model.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Game {

    public static final int TICKS_PER_SECOND = 10;

    private final Set<Updatable> updatables = new LinkedHashSet<>();
    private final Set<Updatable> pendingRegistrations = new LinkedHashSet<>();
    private final Set<Updatable> pendingRemovals = new LinkedHashSet<>();

    private final GameStateManager stateManager = new GameStateManager();

    private long currentTick;
    private boolean updating;


    public void register(Updatable updatable) {

        Objects.requireNonNull(
                updatable,
                "updatable cannot be null"
        );

        if (updating) {
            pendingRemovals.remove(updatable);
            pendingRegistrations.add(updatable);
            return;
        }

        updatables.add(updatable);
    }


    public void unregister(Updatable updatable) {

        if (updatable == null) {
            return;
        }

        if (updating) {
            pendingRegistrations.remove(updatable);
            pendingRemovals.add(updatable);
            return;
        }

        updatables.remove(updatable);
    }


    public void advance(long ticks) {

        if (ticks < 0) {
            throw new IllegalArgumentException(
                    "tick count cannot be negative"
            );
        }

        for (long i = 0; i < ticks; i++) {
            advanceOneTick();
        }
    }


    private void advanceOneTick() {

        if (!stateManager.isRunning()) {
            return;
        }

        currentTick++;

        updating = true;

        try {

            for (Updatable updatable : updatables) {

                if (!pendingRemovals.contains(updatable)) {
                    updatable.update(currentTick);
                }
            }

        } finally {

            updating = false;

            flushPendingChanges();
        }
    }


    private void flushPendingChanges() {

        updatables.removeAll(pendingRemovals);

        updatables.addAll(pendingRegistrations);

        pendingRemovals.clear();

        pendingRegistrations.clear();
    }


    public long getCurrentTick() {
        return currentTick;
    }


    public double getElapsedSeconds() {
        return currentTick / (double) TICKS_PER_SECOND;
    }


    public int getRegisteredObjectCount() {
        return updatables.size();
    }


    /**
     * Returns an immutable, type-filtered snapshot of the objects currently
     * driven by the game loop. This is intentionally read-only so presentation
     * code can observe short-lived runtime entities such as projectiles without
     * gaining access to the loop's mutable registration set.
     */
    public <T extends Updatable> List<T> getRegisteredObjects(
            Class<T> type
    ) {
        Objects.requireNonNull(type, "type cannot be null");

        Set<T> snapshot = new LinkedHashSet<>();
        for (Updatable updatable : updatables) {
            if (!pendingRemovals.contains(updatable)
                    && type.isInstance(updatable)) {
                snapshot.add(type.cast(updatable));
            }
        }
        for (Updatable updatable : pendingRegistrations) {
            if (type.isInstance(updatable)) {
                snapshot.add(type.cast(updatable));
            }
        }
        return List.copyOf(snapshot);
    }


    public GameStateManager getStateManager() {
        return stateManager;
    }
}
