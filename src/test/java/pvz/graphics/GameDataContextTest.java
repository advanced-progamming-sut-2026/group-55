package pvz.graphics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class GameDataContextTest {
    @Test
    void loadsSharedGameplayDataAndServices() throws IOException {
        GameDataContext context = GameDataContext.loadDefault();

        assertFalse(context.plantData().byName().isEmpty());
        assertFalse(context.zombieData().byId().isEmpty());
        assertNotNull(
                context.adventureData()
                        .catalog()
                        .findChapter("ancient-egypt")
        );
        assertNotNull(
                context.adventureData()
                        .catalog()
                        .findLevel("egypt-1")
        );
        assertNotNull(context.levelProgressService());
        assertNotNull(context.greenhouseService());
    }
}
