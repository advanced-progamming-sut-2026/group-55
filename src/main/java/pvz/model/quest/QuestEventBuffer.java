package pvz.model.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Per-session quest event buffer.
 *
 * <p>Battle telemetry is kept in memory until the attempt is settled. This
 * avoids saving the user on every zombie kill or plant placement and lets the
 * existing battle settlement perform one persistent save for rewards,
 * progress and quest state together.</p>
 */
public final class QuestEventBuffer implements QuestEventSink {
    private final List<QuestEvent> events = new ArrayList<>();

    @Override
    public void publish(QuestEvent event) {
        events.add(Objects.requireNonNull(
                event,
                "quest event cannot be null"
        ));
    }

    public List<QuestEvent> snapshot() {
        return List.copyOf(events);
    }

    public List<QuestEvent> drain() {
        List<QuestEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public void clear() {
        events.clear();
    }
}
