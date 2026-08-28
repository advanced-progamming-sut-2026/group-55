package pvz.view.commandparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import pvz.model.command.Command;
import pvz.model.command.PlantSelectionCommand;

class PlantSelectionMenuParserLevelTest {
    @Test
    void parsesUpgradePlantInsideSelectionMenu() {
        Command parsed = new PlantSelectionMenuParser().parse(
                "upgrade plant -t Peashooter"
        );
        PlantSelectionCommand command = assertInstanceOf(
                PlantSelectionCommand.class,
                parsed
        );

        assertEquals(PlantSelectionCommand.Action.UPGRADE_PLANT, command.getAction());
        assertEquals("Peashooter", command.getTargetName());
    }
}
