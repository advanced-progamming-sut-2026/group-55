package pvz.model.entity.plant.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pvz.data.PlantCsvLoader;
import pvz.model.entity.plant.PlantSpec;

class PlantLevelRuntimeStatsTest {
    private static java.util.Map<String, PlantSpec> plants;

    @BeforeAll
    static void loadPlants() throws IOException {
        plants = PlantCsvLoader.load("assets/Data/plants.csv").byName();
    }

    @Test
    void genericUpgradesAreCumulativeWithoutMutatingBaseSpec() {
        PlantSpec base = plants.get("peashooter");
        PlantSpec levelFour = base.withLevel(4);

        assertEquals(1, base.getLevel());
        assertEquals(20, Double.parseDouble(base.getDamage()));
        assertEquals(300, base.getBaseHp());
        assertEquals(100, base.getCost());

        assertEquals(4, levelFour.getLevel());
        assertEquals(30, Double.parseDouble(levelFour.getDamage()));
        assertEquals(450, levelFour.getBaseHp());
        assertEquals(75, levelFour.getCost());
    }

    @Test
    void damageExpressionsPreserveShotCountsAndUpgradeDamageOnly() {
        PlantSpec repeater = plants.get("repeater").withLevel(2);
        PlantSpec kiwibeast = plants.get("kiwibeast").withLevel(3);

        assertEquals("30x2", repeater.getDamage());
        assertEquals("30/45/60", kiwibeast.getDamage());
    }

    @Test
    void attackSpeedUpgradeChangesActionIntervalNotCardRecharge() {
        PlantSpec base = plants.get("cabbage-pult");
        PlantSpec levelThree = base.withLevel(3);

        assertEquals(base.getRecharge(), levelThree.getRecharge(), 1e-9);
        assertEquals(base.getActionInterval() / 1.15,
                levelThree.getActionInterval(), 1e-9);
    }

    @Test
    void cooldownUpgradeChangesCardRechargeNotHomingActionInterval() {
        PlantSpec caulipower = plants.get("caulipower");
        PlantSpec caulipowerLevelTwo = caulipower.withLevel(2);
        PlantSpec magnet = plants.get("magnet-shroom");
        PlantSpec magnetLevelThree = magnet.withLevel(3);

        assertEquals(caulipower.getActionInterval(),
                caulipowerLevelTwo.getActionInterval(), 1e-9);
        assertEquals(caulipower.getRecharge() - 2,
                caulipowerLevelTwo.getRecharge(), 1e-9);
        assertEquals(magnet.getActionInterval(),
                magnetLevelThree.getActionInterval(), 1e-9);
        assertEquals(magnet.getRecharge() - 5,
                magnetLevelThree.getRecharge(), 1e-9);
    }
}
