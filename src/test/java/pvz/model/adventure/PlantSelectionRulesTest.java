package pvz.model.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pvz.model.selection.PlantSelectionRules;

class PlantSelectionRulesTest {
    @Test
    void normalRulesUseEightUnrestrictedSlots() {
        PlantSelectionRules rules = PlantSelectionRules.normal();

        assertEquals(8, rules.maxSlots());
        assertEquals(8, rules.selectableCapacity());
        assertEquals(0, rules.lockedSlots());
    }

    @Test
    void forcedPlantsMustFitInsideSelectableCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new PlantSelectionRules(
                2,
                1,
                List.of("Peashooter", "Sunflower"),
                Set.of(),
                Set.of(),
                Set.of()
        ));
    }
}
