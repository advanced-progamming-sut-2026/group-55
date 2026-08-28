package pvz.model.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.entity.plant.level.PlantLevelCostTable;

class PlantUpgradeServiceTest {
    @Test
    void successfulUpgradeConsumesConfiguredResourcesAndRaisesLevel() {
        User user = new User("user", "hash", "nick", "u@example.com", "male");
        PlayerPlant peashooter = user.getOwnedPlant("Peashooter");
        user.addCoins(1_000);
        peashooter.addSeedPackets(10);

        PlantUpgradeService service = new PlantUpgradeService(
                PlantLevelCostTable.defaults()
        );

        assertEquals(PlantUpgradeService.Result.SUCCESS,
                service.upgrade(user, "peashooter"));
        assertEquals(2, peashooter.getLevel());
        assertEquals(0, user.getCoins());
        assertEquals(0, peashooter.getSeedPackets());
    }

    @Test
    void failedUpgradeDoesNotConsumeAnyResource() {
        User user = new User("user", "hash", "nick", "u@example.com", "male");
        PlayerPlant peashooter = user.getOwnedPlant("Peashooter");
        user.addCoins(5_000);
        peashooter.addSeedPackets(9);

        PlantUpgradeService service = new PlantUpgradeService(
                PlantLevelCostTable.defaults()
        );

        assertEquals(PlantUpgradeService.Result.NOT_ENOUGH_SEEDS,
                service.upgrade(user, "Peashooter"));
        assertEquals(1, peashooter.getLevel());
        assertEquals(5_000, user.getCoins());
        assertEquals(9, peashooter.getSeedPackets());
    }
}
