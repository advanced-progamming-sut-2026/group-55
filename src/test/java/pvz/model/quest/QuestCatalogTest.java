package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pvz.model.minigame.MinigameCatalog;

class QuestCatalogTest {
    @Test
    void defaultCatalogHasUniqueIdsAndAllTravelLogCategories() {
        QuestCatalog catalog = QuestCatalog.createDefault();
        Set<String> ids = new HashSet<>();

        for (QuestSpec quest : catalog.all()) {
            assertTrue(ids.add(quest.id()));
        }

        assertEquals(13, catalog.size());
        for (QuestCategory category : QuestCategory.values()) {
            assertFalse(catalog.byCategory(category).isEmpty());
        }
    }

    @Test
    void lookupNormalizesIdsAndUnknownQuestFailsClearly() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        assertNotNull(catalog.find("  ADVENTURE-FIRST-CLEAR  "));
        assertEquals(
                QuestCatalog.ADVENTURE_FIRST_CLEAR,
                catalog.require("ADVENTURE-FIRST-CLEAR").id()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.require("missing-quest")
        );
    }

    @Test
    void categoryListsAreOrderedByPriority() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        for (QuestCategory category : QuestCategory.values()) {
            List<QuestSpec> quests = catalog.byCategory(category);
            for (int index = 1; index < quests.size(); index++) {
                assertTrue(
                        quests.get(index - 1).priority().sortOrder()
                                <= quests.get(index).priority().sortOrder()
                );
            }
        }
    }

    @Test
    void futurePhaseQuestsStartUnavailable() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        assertFalse(catalog.require(
                QuestCatalog.ADVENTURE_ANCIENT_EGYPT_COMPLETE
        ).initiallyAvailable());
        assertFalse(catalog.require(
                QuestCatalog.MINIGAME_VASE_BREAKER
        ).initiallyAvailable());
        assertFalse(catalog.require(
                QuestCatalog.MINIGAME_WALL_NUT_BOWLING
        ).initiallyAvailable());
        assertFalse(catalog.require(
                QuestCatalog.MINIGAME_I_ZOMBIE
        ).initiallyAvailable());
    }

    @Test
    void battleTelemetryChallengesAreAvailableNowThatHooksExist() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        assertTrue(catalog.require(
                QuestCatalog.CHALLENGE_ZOMBIE_HUNTER
        ).initiallyAvailable());
        assertTrue(catalog.require(
                QuestCatalog.CHALLENGE_SUN_SPENDER
        ).initiallyAvailable());
    }

    @Test
    void highPriorityChallengesPayDiamonds() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        for (QuestSpec quest : catalog.byCategory(QuestCategory.CHALLENGE)) {
            if (quest.priority() != QuestPriority.HIGH) {
                continue;
            }
            assertTrue(quest.rewards().stream().anyMatch(
                    reward -> reward.type() == QuestRewardType.DIAMONDS
            ));
        }
    }


    @Test
    void minigameQuestSubjectsMatchCentralMinigameIds() {
        QuestCatalog catalog = QuestCatalog.createDefault();

        assertEquals(
                MinigameCatalog.VASE_BREAKER,
                catalog.require(QuestCatalog.MINIGAME_VASE_BREAKER)
                        .objective()
                        .subjectId()
        );
        assertEquals(
                MinigameCatalog.WALL_NUT_BOWLING,
                catalog.require(QuestCatalog.MINIGAME_WALL_NUT_BOWLING)
                        .objective()
                        .subjectId()
        );
        assertEquals(
                MinigameCatalog.I_ZOMBIE,
                catalog.require(QuestCatalog.MINIGAME_I_ZOMBIE)
                        .objective()
                        .subjectId()
        );
    }

    @Test
    void duplicateIdsAreRejectedAfterNormalization() {
        QuestSpec first = simple("Quest-One");
        QuestSpec duplicate = simple(" quest-one ");

        assertThrows(
                IllegalArgumentException.class,
                () -> new QuestCatalog(List.of(first, duplicate))
        );
    }

    private QuestSpec simple(String id) {
        return new QuestSpec(
                id,
                "Quest",
                "Description",
                QuestCategory.DAILY,
                QuestPriority.MEDIUM,
                QuestObjective.global(QuestMetric.GAMES_PLAYED, 1),
                List.of(QuestReward.coins(1)),
                QuestResetPolicy.DAILY
        );
    }
}
