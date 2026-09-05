package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestModelTest {
    @Test
    void questSpecNormalizesIdAndPreservesPriorityOrder() {
        QuestSpec spec = new QuestSpec(
                "  Adventure-First-Win  ",
                "First Win",
                "Complete one adventure level.",
                QuestCategory.ADVENTURE,
                QuestPriority.CRITICAL,
                QuestObjective.global(QuestMetric.CLEARED_STAGES, 1),
                List.of(QuestReward.coins(500)),
                QuestResetPolicy.NEVER
        );

        assertEquals("adventure-first-win", spec.id());
        assertEquals(0, QuestPriority.CRITICAL.sortOrder());
        assertTrue(
                QuestPriority.CRITICAL.sortOrder()
                        < QuestPriority.HIGH.sortOrder()
        );
    }

    @Test
    void rewardsKeepTheThreeSpecificationFamilies() {
        QuestReward currency = QuestReward.diamonds(10);
        QuestReward unlockable = QuestReward.plantUnlock("Snow Pea");
        QuestReward inventory = QuestReward.seedPackets("Peashooter", 5);

        assertEquals(QuestRewardCategory.CURRENCY, currency.category());
        assertEquals(QuestRewardCategory.UNLOCKABLE, unlockable.category());
        assertEquals(QuestRewardCategory.INVENTORY, inventory.category());
        assertEquals("Snow Pea", unlockable.targetId());
        assertEquals(5, inventory.amount());
    }

    @Test
    void invalidQuestDefinitionsFailFast() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QuestObjective.global(QuestMetric.CLEARED_STAGES, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> QuestReward.coins(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestSpec(
                        "q",
                        "Quest",
                        "Description",
                        QuestCategory.DAILY,
                        QuestPriority.MEDIUM,
                        QuestObjective.global(QuestMetric.GAMES_PLAYED, 1),
                        List.of(),
                        QuestResetPolicy.DAILY
                )
        );
    }

    @Test
    void phaseBoundaryEventsCarryOnlyQuestData() {
        QuestEvent level = QuestEvent.levelCompleted("egypt-1");
        QuestEvent sun = QuestEvent.sunSpent(125);
        QuestEvent minigame = QuestEvent.minigameCompleted("vase-breaker");

        assertEquals(QuestMetric.LEVEL_COMPLETED, level.metric());
        assertEquals("egypt-1", level.subjectId());
        assertEquals(125, sun.amount());
        assertEquals(QuestMetric.MINIGAME_COMPLETED, minigame.metric());
    }
}
