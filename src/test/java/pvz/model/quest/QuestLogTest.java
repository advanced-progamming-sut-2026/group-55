package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class QuestLogTest {
    @Test
    void normalizesQuestIdsAndDoesNotCreateDuplicates() {
        QuestLog log = new QuestLog();

        QuestProgress first = log.getOrCreate("  First-Win  ");
        QuestProgress second = log.getOrCreate("first-win");

        assertSame(first, second);
        assertEquals(1, log.getAll().size());
        assertSame(first, log.find("FIRST-WIN"));
    }

    @Test
    void countsCompletedAndClaimedProgress() {
        QuestLog log = new QuestLog();
        QuestProgress completed = log.getOrCreate("completed");
        QuestProgress claimed = log.getOrCreate("claimed");

        completed.markCompleted();
        claimed.markCompleted();
        claimed.markClaimed();

        assertEquals(2, log.completedCount());
        assertEquals(1, log.claimedCount());
    }
}
