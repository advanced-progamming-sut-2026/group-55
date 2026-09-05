package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestEventBufferTest {
    @Test
    void snapshotDoesNotConsumeAndDrainDoes() {
        QuestEventBuffer buffer = new QuestEventBuffer();
        buffer.publish(QuestEvent.zombieKilled("default"));
        buffer.publish(QuestEvent.sunSpent(100));

        List<QuestEvent> snapshot = buffer.snapshot();

        assertEquals(2, snapshot.size());
        assertEquals(2, buffer.size());
        assertEquals(snapshot, buffer.drain());
        assertTrue(buffer.isEmpty());
    }
}
