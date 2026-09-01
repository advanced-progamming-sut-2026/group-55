package pvz.graphics.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import pvz.data.PlantCsvLoader;

class GraphicsAssetPathTest {

    @Test
    void greenhousePlantDataPathMatchesRepositoryCase() throws IOException {
        Path plantDataPath = Path.of(GameMenuScreen.PLANT_DATA_PATH);

        assertTrue(Files.isRegularFile(plantDataPath));
        assertFalse(
                PlantCsvLoader.load(GameMenuScreen.PLANT_DATA_PATH)
                        .byName()
                        .isEmpty()
        );
    }
}
