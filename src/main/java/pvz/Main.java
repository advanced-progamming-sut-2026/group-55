package pvz;

import pvz.data.PlantCsvLoader;
import pvz.model.core.Board;
import pvz.model.core.Tile;
import pvz.model.core.TileType;
import pvz.model.entity.plant.PlantSpec;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args){
        Board board = new Board();
        try {
            Map<String, PlantSpec> plantSpecsMap = PlantCsvLoader.load("assets/Data/plants.csv");
            System.out.println(plantSpecsMap.size());
            System.out.println(plantSpecsMap.get("Peashooter").getCost());
        } catch (IOException e) {
            System.out.println("Error: could not read plants data file!");
            System.out.println(e.getMessage());
        }
    }
}