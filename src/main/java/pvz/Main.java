package pvz;

import pvz.controller.game.GameController;
import pvz.data.PlantCsvLoader;
import pvz.model.core.Board;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.PlantSpec;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Map<String, PlantSpec> plantSpecsMap;
        try {
            plantSpecsMap = PlantCsvLoader.load("assets/Data/plants.csv");
        } catch (IOException e) {
            System.out.println("Error: could not read plants data file!");
            return;
        }
        Board board = new Board();
        PlantFactory factory = new PlantFactory(plantSpecsMap);
        GameController controller = new GameController(board, factory);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            System.out.println(controller.handle(scanner.nextLine()));
        }
    }
}