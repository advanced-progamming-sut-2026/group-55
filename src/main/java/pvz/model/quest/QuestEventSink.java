package pvz.model.quest;

import java.util.Objects;

/**
 * Minimal phase-boundary publisher used by battle/adventure/minigame systems.
 *
 * <p>Gameplay code only emits {@link QuestEvent} values through this
 * interface; it never depends on Travel Log screens or QuestService.</p>
 */
@FunctionalInterface
public interface QuestEventSink {
    void publish(QuestEvent event);

    static QuestEventSink none() {
        return event -> Objects.requireNonNull(
                event,
                "quest event cannot be null"
        );
    }
}
