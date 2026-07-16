package pvz.controller.game;

import pvz.model.core.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;

import java.util.regex.Matcher;

public class GameController {
    private final Board board;
    private final PlantFactory factory;

    public GameController(Board board, PlantFactory factory) {
        this.board = board;
        this.factory = factory;
    }

    public String handle(String input) {
        Matcher m;
        if ((m = GameCommand.PLANT.getMatcher(input)) != null) {
            return handlePlant(m);
        }
        if ((m = GameCommand.SHOW_MAP.getMatcher(input)) != null) {
            //return handleShowMap();TODO:I will do it.
        }
        return "invalid command!";
    }

    private String handlePlant(Matcher m) {
        String type = m.group("type");
        int x = Integer.parseInt(m.group("x"));
        int y = Integer.parseInt(m.group("y"));
        Plant plant = factory.create(type);
        if( plant == null ) return "unknown plant type: " + type + "!";

        return board.plant(x, y, plant);
    }
}