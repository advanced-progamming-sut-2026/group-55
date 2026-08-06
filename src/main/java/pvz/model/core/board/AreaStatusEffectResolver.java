package pvz.model.core.board;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import pvz.model.entity.zombie.Zombie;

final class AreaStatusEffectResolver {

    private final BoardGrid grid;

    AreaStatusEffectResolver(BoardGrid grid) {
        this.grid = Objects.requireNonNull(
                grid,
                "grid cannot be null"
        );
    }

    void chillZombies(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            long currentTick,
            long durationTicks
    ) {
        validateTime(currentTick, durationTicks);

        applyToZombies(
                zombies,
                centerX,
                centerY,
                radius,
                zombie -> zombie.applyChill(
                        currentTick,
                        durationTicks
                )
        );
    }

    void freezeZombies(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            long currentTick,
            long durationTicks
    ) {
        validateTime(currentTick, durationTicks);

        applyToZombies(
                zombies,
                centerX,
                centerY,
                radius,
                zombie -> zombie.applyFreeze(
                        currentTick,
                        durationTicks
                )
        );
    }

    private void applyToZombies(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            Consumer<Zombie> effect
    ) {
        Objects.requireNonNull(
                zombies,
                "zombies cannot be null"
        );
        grid.requireInBounds(centerX, centerY);

        if (radius < 0) {
            throw new IllegalArgumentException(
                    "area radius cannot be negative"
            );
        }

        for (Zombie zombie : zombies) {
            if (isInsideSquare(
                    zombie.getTileX(),
                    zombie.getTileY(),
                    centerX,
                    centerY,
                    radius
            )) {
                effect.accept(zombie);
            }
        }
    }

    private void validateTime(
            long currentTick,
            long durationTicks
    ) {
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "effect duration must be positive"
            );
        }
    }

    private boolean isInsideSquare(
            int x,
            int y,
            int centerX,
            int centerY,
            int radius
    ) {
        return Math.abs(x - centerX) <= radius
                && Math.abs(y - centerY) <= radius;
    }
}
