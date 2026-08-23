package pvz.model.session.condition;

import java.util.Objects;

public final class ClearAllWavesWinCondition implements WinCondition {
    @Override
    public boolean isSatisfied(WinConditionContext context) {
        Objects.requireNonNull(context, "win condition context cannot be null");
        return context.waveManager().isCompleted()
                && context.aliveZombieCount() == 0;
    }
}
