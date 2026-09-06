package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class QuestProgressTest {
    @Test
    void supportsCompleteClaimLifecycle() {
        QuestProgress progress = new QuestProgress("Daily-Battle");

        assertEquals("daily-battle", progress.getQuestId());
        assertEquals(QuestState.AVAILABLE, progress.getState());
        assertFalse(progress.isCompleted());

        progress.addValue(1);
        progress.markCompleted();
        progress.markCompleted();

        assertEquals(1, progress.getLifetimeCompletionCount());
        assertTrue(progress.isCompleted());
        assertFalse(progress.isClaimed());

        progress.markClaimed();

        assertTrue(progress.isCompleted());
        assertTrue(progress.isClaimed());
        assertThrows(IllegalStateException.class, progress::markClaimed);
    }

    @Test
    void dailyResetClearsClaimAndProgressForNewCycle() {
        QuestProgress progress = new QuestProgress("daily-battle");
        progress.setValue(4);
        progress.markCompleted();
        progress.markClaimed();

        LocalDate nextDay = LocalDate.of(2026, 9, 6);
        progress.resetForCycle(nextDay);

        assertEquals(1, progress.getLifetimeCompletionCount());
        assertEquals(0, progress.getValue());
        assertEquals(QuestState.AVAILABLE, progress.getState());
        assertEquals(nextDay, progress.getCycleDate());
    }

    @Test
    void unavailableQuestCannotBeCompleted() {
        QuestProgress progress = new QuestProgress("future-minigame");
        progress.markUnavailable();

        assertEquals(QuestState.UNAVAILABLE, progress.getState());
        assertThrows(IllegalStateException.class, progress::markCompleted);

        progress.activate();
        assertEquals(QuestState.AVAILABLE, progress.getState());
    }

    @Test
    void dailyCompletionCountAccumulatesAcrossCycles() {
        QuestProgress progress = new QuestProgress("daily-battle");

        progress.markCompleted();
        progress.markClaimed();
        progress.resetForCycle(LocalDate.of(2026, 9, 6));
        progress.markCompleted();

        assertEquals(2, progress.getLifetimeCompletionCount());
    }
}
