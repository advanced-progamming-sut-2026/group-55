package pvz.model.session.condition;

import java.util.Objects;
import pvz.model.adventure.ObjectiveType;

public final class WinConditionFactory {
    public WinCondition create(ObjectiveType type) {
        Objects.requireNonNull(type, "objective type cannot be null");
        return switch (type) {
            case CLEAR_ALL_WAVES -> new ClearAllWavesWinCondition();
        };
    }
}
