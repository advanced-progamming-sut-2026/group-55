package pvz.graphics;

import java.io.IOException;
import java.util.Objects;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.adventure.LevelProgressService;
import pvz.model.minigame.MinigameCatalog;
import pvz.model.minigame.MinigameProgressService;
import pvz.model.quest.QuestCatalog;
import pvz.model.service.GreenhouseService;

public final class GameDataContext {
    static final String PLANTS_PATH = "assets/Data/plants.csv";
    static final String ZOMBIES_PATH = "assets/Data/zombies.csv";
    static final String CHAPTERS_PATH = "assets/Data/chapters.csv";
    static final String LEVELS_PATH = "assets/Data/levels.csv";
    static final String LEVEL_ZOMBIES_PATH =
            "assets/Data/level_zombies.csv";
    static final String WAVES_PATH = "assets/Data/waves.csv";

    private final PlantData plantData;
    private final ZombieData zombieData;
    private final AdventureData adventureData;
    private final LevelProgressService levelProgressService;
    private final GreenhouseService greenhouseService;
    private final QuestCatalog questCatalog;
    private final MinigameCatalog minigameCatalog;
    private final MinigameProgressService minigameProgressService;

    private GameDataContext(
            PlantData plantData,
            ZombieData zombieData,
            AdventureData adventureData
    ) {
        this.plantData = Objects.requireNonNull(
                plantData,
                "plant data cannot be null"
        );
        this.zombieData = Objects.requireNonNull(
                zombieData,
                "zombie data cannot be null"
        );
        this.adventureData = Objects.requireNonNull(
                adventureData,
                "adventure data cannot be null"
        );
        this.levelProgressService = new LevelProgressService(
                adventureData.catalog()
        );
        this.greenhouseService = new GreenhouseService(plantData);
        this.questCatalog = QuestCatalog.createDefault();
        this.minigameCatalog = MinigameCatalog.createDefault();
        this.minigameProgressService = new MinigameProgressService(
                minigameCatalog
        );
    }

    public static GameDataContext loadDefault() throws IOException {
        PlantData plantData = PlantCsvLoader.load(PLANTS_PATH);
        ZombieData zombieData = ZombieCsvLoader.load(ZOMBIES_PATH);
        AdventureData adventureData = AdventureCsvLoader.load(
                CHAPTERS_PATH,
                LEVELS_PATH,
                LEVEL_ZOMBIES_PATH,
                WAVES_PATH,
                zombieData
        );
        return new GameDataContext(
                plantData,
                zombieData,
                adventureData
        );
    }

    public PlantData plantData() {
        return plantData;
    }

    public ZombieData zombieData() {
        return zombieData;
    }

    public AdventureData adventureData() {
        return adventureData;
    }

    public LevelProgressService levelProgressService() {
        return levelProgressService;
    }

    public GreenhouseService greenhouseService() {
        return greenhouseService;
    }

    public QuestCatalog questCatalog() {
        return questCatalog;
    }

    public MinigameCatalog minigameCatalog() {
        return minigameCatalog;
    }

    public MinigameProgressService minigameProgressService() {
        return minigameProgressService;
    }
}
