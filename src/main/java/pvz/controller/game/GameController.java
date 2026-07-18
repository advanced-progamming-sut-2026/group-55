package pvz.controller.game;

import java.util.Objects;
import java.util.regex.Matcher;
import pvz.model.core.Board;
import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;

public class GameController {
    private final Board board;
    private final PlantFactory factory;
    private final Game game;

    public GameController(Board board, PlantFactory factory, Game game) {
        this.board = Objects.requireNonNull(board);
        this.factory = Objects.requireNonNull(factory);
        this.game = Objects.requireNonNull(game);
    }

    public String handle(String input) {
        Matcher matcher;
        if ((matcher = GameCommand.PLANT.getMatcher(input)) != null) {
            return handlePlant(matcher);
        }
        if ((matcher = GameCommand.ADVANCE_TIME.getMatcher(input)) != null) {
            return handleAdvanceTime(matcher);
        }
        if (GameCommand.SHOW_MAP.getMatcher(input) != null) {
            return handleShowMap();
        }
        if ((matcher = GameCommand.PLUCK.getMatcher(input)) != null) {
            return handlePluck(matcher);
        }
        return "invalid command!";
    }

    private String handlePlant(Matcher matcher) {
        String type = matcher.group("type");
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        Plant plant = factory.create(type);
        if (plant == null) {
            return "unknown plant type: " + type + "!";
        }
        return board.plant(x, y, plant);
    }

    private String handleAdvanceTime(Matcher matcher) {
        long ticks = Long.parseLong(matcher.group("count"));
        game.advance(ticks);
        return "time advanced by " + ticks + " ticks; current tick: " + game.getCurrentTick();
    }

    private String handlePluck(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        return board.pluck(x, y);
    }

    private String handleShowMap() {
        return "map rendering is implemented in the gameplay-status section";
    }
}
