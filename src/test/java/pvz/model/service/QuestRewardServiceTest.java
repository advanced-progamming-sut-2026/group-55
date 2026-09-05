package pvz.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import pvz.model.account.User;
import pvz.model.quest.QuestReward;

class QuestRewardServiceTest {
    private final QuestRewardService service = new QuestRewardService();

    @Test
    void appliesAllCurrentRewardFamilies() {
        User user = user();
        int coinsBefore = user.getCoins();
        int diamondsBefore = user.getDiamonds();

        List<QuestReward> rewards = List.of(
                QuestReward.coins(500),
                QuestReward.diamonds(7),
                QuestReward.plantUnlock("Reward Plant"),
                QuestReward.seedPackets("Reward Plant", 12),
                QuestReward.levelUnlock("egypt-special-1")
        );

        assertTrue(service.validate(user, rewards).valid());
        service.apply(user, rewards);

        assertEquals(coinsBefore + 500, user.getCoins());
        assertEquals(diamondsBefore + 7, user.getDiamonds());
        assertNotNull(user.getOwnedPlant("Reward Plant"));
        assertEquals(
                12,
                user.getOwnedPlant("Reward Plant").getSeedPackets()
        );
        assertTrue(user.getAdventureProgress()
                .isLevelRewardUnlocked("EGYPT-SPECIAL-1"));
    }

    @Test
    void refusesSeedRewardForPlantThatWillRemainLocked() {
        User user = user();

        QuestRewardService.Validation validation = service.validate(
                user,
                List.of(QuestReward.seedPackets("Not Owned", 5))
        );

        assertFalse(validation.valid());
    }

    @Test
    void repeatedUnlockRewardDoesNotDuplicateOwnedPlant() {
        User user = user();
        int before = user.getUnlockedPlants().size();

        service.apply(
                user,
                List.of(QuestReward.plantUnlock("Peashooter"))
        );

        assertEquals(before, user.getUnlockedPlants().size());
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
