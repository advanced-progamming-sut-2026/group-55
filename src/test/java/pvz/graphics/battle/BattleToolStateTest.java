package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BattleToolStateTest {
    @Test
    void neutralSelectionLeavesShovelReadyAndPlantFoodAvailable() {
        BattleToolState.View view = BattleToolState.resolve(
                BattleToolState.Selection.NONE,
                null,
                2
        );

        assertEquals("SELECTED TOOL: NONE", view.selectionText());
        assertFalse(view.shovel().selected());
        assertEquals("READY", view.shovel().statusText());
        assertFalse(view.plantFood().selected());
        assertTrue(view.plantFood().available());
        assertEquals("READY  x2", view.plantFood().statusText());
    }

    @Test
    void selectedPlantIsReportedWithoutSelectingAUtilityTool() {
        BattleToolState.View view = BattleToolState.resolve(
                BattleToolState.Selection.PLANT,
                "Peashooter",
                1
        );

        assertEquals("SELECTED PLANT: PEASHOOTER", view.selectionText());
        assertFalse(view.shovel().selected());
        assertFalse(view.plantFood().selected());
    }

    @Test
    void selectedUtilityToolGetsAnExplicitActiveState() {
        BattleToolState.View shovel = BattleToolState.resolve(
                BattleToolState.Selection.SHOVEL,
                null,
                1
        );
        BattleToolState.View food = BattleToolState.resolve(
                BattleToolState.Selection.PLANT_FOOD,
                null,
                3
        );

        assertTrue(shovel.shovel().selected());
        assertEquals("ACTIVE", shovel.shovel().statusText());
        assertTrue(food.plantFood().selected());
        assertEquals("ACTIVE  x3", food.plantFood().statusText());
    }

    @Test
    void emptyPlantFoodIsUnavailableAndCannotLookSelected() {
        BattleToolState.View view = BattleToolState.resolve(
                BattleToolState.Selection.PLANT_FOOD,
                null,
                0
        );

        assertEquals("SELECTED TOOL: NONE", view.selectionText());
        assertFalse(view.plantFood().available());
        assertFalse(view.plantFood().selected());
        assertEquals("EMPTY", view.plantFood().statusText());
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                BattleToolState.resolve(null, null, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BattleToolState.resolve(
                        BattleToolState.Selection.NONE,
                        null,
                        -1
                ));
    }
}
