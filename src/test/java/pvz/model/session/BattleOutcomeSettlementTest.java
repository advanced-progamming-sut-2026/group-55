package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pvz.model.account.User;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelProgressService;
import pvz.model.adventure.LevelSpec;
import pvz.model.adventure.LevelType;
import pvz.model.adventure.ObjectiveType;
import pvz.model.core.BattleResources;

class BattleOutcomeSettlementTest {
    private BattleOutcomeSettlement settlement;
    private User user;

    @BeforeEach
    void setUp() {
        Map<String, ChapterSpec> chapters = new LinkedHashMap<>();
        chapters.put("ancient-egypt", new ChapterSpec(
                "ancient-egypt",
                "Ancient Egypt",
                1
        ));

        Map<String, LevelSpec> levels = new LinkedHashMap<>();
        levels.put("egypt-1", normalLevel("egypt-1", 1));
        levels.put("egypt-2", normalLevel("egypt-2", 2));

        LevelCatalog catalog = new LevelCatalog(chapters, levels);
        settlement = new BattleOutcomeSettlement(
                new LevelProgressService(catalog)
        );
        user = new User(
                "player",
                "hash",
                "Player",
                "p@example.com",
                "x"
        );
    }

    @Test
    void winPersistsRewardsAndProgressOnlyOnceForOneAttempt() {
        BattleResources resources = rewards(75, 2);

        BattleOutcomeSettlement.Result result = settlement.settle(
                GameSessionStatus.WON,
                "egypt-1",
                resources,
                user,
                true
        );

        assertEquals(75, user.getCoins());
        assertEquals(2, user.getDiamonds());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(1, user.getClearedStages());
        assertTrue(user.getAdventureProgress().isLevelCompleted("egypt-1"));
        assertTrue(result.newlyCompleted());
        assertEquals("egypt-2", result.unlockedLevelId());

        assertThrows(
                IllegalStateException.class,
                () -> settlement.settle(
                        GameSessionStatus.WON,
                        "egypt-1",
                        resources,
                        user,
                        true
                )
        );
        assertEquals(75, user.getCoins());
        assertEquals(2, user.getDiamonds());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(1, user.getClearedStages());
    }

    @Test
    void replayCanEarnNewDropsButDoesNotDoubleCountLevelProgress() {
        settlement.settle(
                GameSessionStatus.WON,
                "egypt-1",
                rewards(25, 1),
                user,
                true
        );

        BattleOutcomeSettlement.Result replay = settlement.settle(
                GameSessionStatus.WON,
                "egypt-1",
                rewards(40, 1),
                user,
                true
        );

        assertEquals(65, user.getCoins());
        assertEquals(2, user.getDiamonds());
        assertEquals(2, user.getGamesPlayed());
        assertEquals(1, user.getClearedStages());
        assertFalse(replay.newlyCompleted());
        assertNull(replay.unlockedLevelId());
        assertNull(replay.unlockedChapterId());
    }

    @Test
    void lossAndAbortNeverAdvanceAdventureProgress() {
        settlement.settle(
                GameSessionStatus.LOST,
                "egypt-1",
                rewards(10, 1),
                user,
                false
        );
        settlement.settle(
                GameSessionStatus.ABORTED,
                "egypt-1",
                rewards(15, 0),
                user,
                false
        );

        assertEquals(25, user.getCoins());
        assertEquals(1, user.getDiamonds());
        assertEquals(2, user.getGamesPlayed());
        assertEquals(0, user.getClearedStages());
        assertFalse(user.getAdventureProgress().isLevelCompleted("egypt-1"));
    }

    @Test
    void lossCanSaveDropsBeforeRetryWithoutDuplicatingPlantFood() {
        BattleResources resources = new BattleResources(50, 2);
        resources.battleWallet().addCoins(30);

        settlement.settle(
                GameSessionStatus.LOST,
                "egypt-1",
                resources,
                user,
                false
        );

        assertEquals(30, user.getCoins());
        assertEquals(0, user.getPlantFoodCount());
        assertFalse(resources.isPlantFoodReturned());

        assertEquals(2, settlement.returnRemainingPlantFood(resources, user));
        assertEquals(2, user.getPlantFoodCount());
        assertTrue(resources.isPlantFoodReturned());
        assertEquals(0, settlement.returnRemainingPlantFood(resources, user));
        assertEquals(2, user.getPlantFoodCount());
    }

    @Test
    void runningBattleCannotBeSettled() {
        assertThrows(
                IllegalStateException.class,
                () -> settlement.settle(
                        GameSessionStatus.RUNNING,
                        "egypt-1",
                        rewards(10, 0),
                        user,
                        false
                )
        );
        assertEquals(0, user.getCoins());
        assertEquals(0, user.getGamesPlayed());
    }

    private BattleResources rewards(int coins, int diamonds) {
        BattleResources resources = new BattleResources(50, 0);
        resources.battleWallet().addCoins(coins);
        resources.battleWallet().addDiamonds(diamonds);
        return resources;
    }

    private LevelSpec normalLevel(String id, int number) {
        return new LevelSpec(
                id,
                "ancient-egypt",
                number,
                id,
                LevelType.NORMAL,
                9,
                5,
                50,
                true,
                ObjectiveType.CLEAR_ALL_WAVES
        );
    }
}
