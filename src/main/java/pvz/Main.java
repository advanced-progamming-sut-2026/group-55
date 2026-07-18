package pvz;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import pvz.controller.game.GameController;
import pvz.data.PlantCsvLoader;
import pvz.model.core.Board;
import pvz.model.core.Game;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.PlantSpec;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Map<String, PlantSpec> plantSpecs;
        try {
            plantSpecs = PlantCsvLoader.load("assets/Data/plants.csv");
        } catch (IOException | IllegalArgumentException exception) {
            System.out.println("Error: could not read plants data file: " + exception.getMessage());
            return;
        }

        Game game = new Game();
        Board board = new Board();
        game.register(board);

        PlantFactory factory = new PlantFactory(plantSpecs);
        GameController controller = new GameController(board, factory, game);
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                System.out.println(controller.handle(scanner.nextLine()));
            }
        }
    }
}
