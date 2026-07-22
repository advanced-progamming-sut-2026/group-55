package pvz;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import pvz.controller.game.GameController;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.session.GameSession;
import pvz.model.session.GameSessionConfig;
import pvz.model.session.GameSessionFactory;

public final class Main {
    private static final int STARTING_SUN = 150;

    private Main() {
    }

    public static void main(String[] args) {
        PlantData plantData;
        try {
            plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println(
                    "Error: could not read plants data file: "
                            + exception.getMessage()
            );
            return;
        }

        PlantFactory plantFactory = new PlantFactory(plantData.byName());
        GameSessionFactory sessionFactory = new GameSessionFactory(plantFactory);

        GameSessionConfig config =
                new GameSessionConfig.Builder(
                        "test-level",
                        List.copyOf(plantData.byName().keySet())
                )
                        .startingSun(STARTING_SUN)
                        .tombCoordinates(
                                List.of(
                                        new Point(3, 2),
                                        new Point(6, 4),
                                        new Point(8, 1)
                                )
                        )
                        .build();

        GameSession session = sessionFactory.create(config);
        session.start();

        GameController controller = new GameController(session);

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                System.out.println(controller.handle(scanner.nextLine()));
            }
        }
    }
}
