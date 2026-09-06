package pvz.model.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;

class UserQuestProgressSourceTest {
    private final UserQuestProgressSource source =
            new UserQuestProgressSource();

    @Test
    void readsPersistentUserSnapshotMetrics() {
        User user = user();
        user.setGamesPlayed(7);
        user.setClearedStages(2);
        user.getAdventureProgress().completeLevel("egypt-1");
        user.getAdventureProgress().completeLevel("egypt-2");
        user.getAdventureProgress().completeLevel("egypt-3");
        user.getOwnedPlant("Peashooter").upgrade();
        user.addSeenZombie("default");
        user.addSeenZombie("conehead");

        assertEquals(7, value(user, QuestMetric.GAMES_PLAYED));
        assertEquals(3, value(user, QuestMetric.CLEARED_STAGES));
        assertTrue(value(user, QuestMetric.OWNED_PLANTS) > 0);
        assertEquals(1, value(user, QuestMetric.UPGRADED_PLANTS));
        assertEquals(2, value(user, QuestMetric.SEEN_ZOMBIES));
    }

    @Test
    void supportsSubjectSpecificOwnedUpgradedAndSeenChecks() {
        User user = user();
        PlayerPlant peashooter = user.getOwnedPlant("Peashooter");
        peashooter.upgrade();
        user.addSeenZombie("ConeHead");

        assertEquals(1, source.currentValue(
                user,
                QuestObjective.forSubject(
                        QuestMetric.OWNED_PLANTS,
                        "peashooter",
                        1
                )
        ));
        assertEquals(1, source.currentValue(
                user,
                QuestObjective.forSubject(
                        QuestMetric.UPGRADED_PLANTS,
                        "PEASHOOTER",
                        1
                )
        ));
        assertEquals(1, source.currentValue(
                user,
                QuestObjective.forSubject(
                        QuestMetric.SEEN_ZOMBIES,
                        "conehead",
                        1
                )
        ));
    }

    private int value(User user, QuestMetric metric) {
        return source.currentValue(
                user,
                QuestObjective.global(metric, 1)
        );
    }

    private User user() {
        return new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
    }
}
