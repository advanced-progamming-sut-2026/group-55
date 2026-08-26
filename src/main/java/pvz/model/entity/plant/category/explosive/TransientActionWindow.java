package pvz.model.entity.plant.category.explosive;

public final class TransientActionWindow {

    public enum State {
        IDLE,
        EFFECT_ACTIVE,
        FINISHED
    }

    private final long displayTicks;

    private State state = State.IDLE;

    private long finishTick;

    public TransientActionWindow(long displayTicks) {
        if (displayTicks <= 0) {
            throw new IllegalArgumentException(
                    "effect display ticks must be positive"
            );
        }

        this.displayTicks = displayTicks;
    }

    public boolean start(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (state != State.IDLE) {
            return false;
        }

        state = State.EFFECT_ACTIVE;
        finishTick = currentTick + displayTicks;
        return true;
    }

    public boolean shouldFinish(long currentTick) {
        return state == State.EFFECT_ACTIVE && currentTick >= finishTick;
    }

    public void finish() {
        if (state == State.EFFECT_ACTIVE) {
            state = State.FINISHED;
        }
    }

    public State getState() {
        return state;
    }

    public boolean isEffectActive() {
        return state == State.EFFECT_ACTIVE;
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public long getFinishTick() {
        return finishTick;
    }

    public long getDisplayTicks() {
        return displayTicks;
    }
}
