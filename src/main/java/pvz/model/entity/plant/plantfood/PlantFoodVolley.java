package pvz.model.entity.plant.plantfood;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import pvz.model.core.Game;
import pvz.model.core.Updatable;

public final class PlantFoodVolley implements Updatable {

    private final Game game;
    private final int totalSteps;
    private final long gapTicks;
    private final BooleanSupplier canContinue;
    private final IntConsumer fireStep;

    private int nextStep;
    private long nextShotTick;

    private PlantFoodVolley(
            Game game,
            int totalSteps,
            long gapTicks,
            BooleanSupplier canContinue,
            IntConsumer fireStep
    ) {
        this.game = Objects.requireNonNull(game, "game cannot be null");

        this.canContinue = Objects.requireNonNull(canContinue,
                "continue condition cannot be null");

        this.fireStep = Objects.requireNonNull(fireStep, "fire step cannot be null");

        if (totalSteps <= 0) {
            throw new IllegalArgumentException("total volley steps must be positive");
        }

        if (totalSteps > 1 && gapTicks <= 0) {
            throw new IllegalArgumentException("multi-step volley needs a positive shot gap");
        }

        this.totalSteps = totalSteps;
        this.gapTicks = gapTicks;
    }

    public static void start(
            Game game,
            long currentTick,
            int totalSteps,
            long gapTicks,
            BooleanSupplier canContinue,
            IntConsumer fireStep
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        PlantFoodVolley volley = new PlantFoodVolley(
                game,
                totalSteps,
                gapTicks,
                canContinue,
                fireStep
        );

        if (!volley.canContinue.getAsBoolean()) {
            return;
        }

        volley.fireCurrentStep();

        if (volley.hasMoreSteps()) {
            volley.nextShotTick = currentTick + gapTicks;
            game.register(volley);
        }
    }

    @Override
    public void update(long tick) {
        if (!canContinue.getAsBoolean()) {
            game.unregister(this);
            return;
        }

        if (tick < nextShotTick) {
            return;
        }

        fireCurrentStep();

        if (!hasMoreSteps()) {
            game.unregister(this);
            return;
        }

        nextShotTick = tick + gapTicks;
    }

    private void fireCurrentStep() {
        fireStep.accept(nextStep);
        nextStep++;
    }

    private boolean hasMoreSteps() {
        return nextStep < totalSteps;
    }
}
