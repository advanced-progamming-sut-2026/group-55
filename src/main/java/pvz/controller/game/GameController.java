package pvz.controller.game;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import pvz.model.core.*;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.BasicZombie;
import pvz.model.entity.zombie.Zombie;

public class GameController {
    private final World world;
    private final Board board;
    private final Game game;
    private final PlantFactory factory;
    private final Map<String, Long> lastPlantedTick = new HashMap<>();

    public GameController(World world, PlantFactory factory) {
        this.world = Objects.requireNonNull(world);
        this.board = world.board();
        this.game = world.game();
        this.factory = Objects.requireNonNull(factory);
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
        if (GameCommand.SHOW_SUN.getMatcher(input) != null) {
            return "you have " + world.sunBank().getBalance() + " sun";
        }
        if ((matcher = GameCommand.COLLECT_SUN.getMatcher(input)) != null) {
            return handleCollectSun(matcher);
        }
        if ((matcher = GameCommand.SPAWN_ZOMBIE.getMatcher(input)) != null) {
            return handleSpawnZombie(matcher);
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
        String key = type.toLowerCase(Locale.ROOT);
        long rechargeTicks = (long) (plant.getSpec().getRecharge() * Game.TICKS_PER_SECOND);
        Long lastTick = lastPlantedTick.get(key);
        if (lastTick != null && game.getCurrentTick() - lastTick < rechargeTicks) {
            long remaining = rechargeTicks - (game.getCurrentTick() - lastTick);
            return plant.getName() + " is recharging; ready in " + remaining + " ticks!";
        }
        int cost = plant.getSpec().getCost();
        if (!world.sunBank().canAfford(cost)) {
            return "not enough sun! (need " + cost + ", have " + world.sunBank().getBalance() + ")";
        }
        String result = board.plant(x, y, plant);
        if (board.inBounds(x, y) && board.getTile(x, y).getPlants().contains(plant)) {
            world.sunBank().spend(cost);
            lastPlantedTick.put(key, game.getCurrentTick());
            plant.place(world, x, y, game.getCurrentTick());
            game.register(plant);
        }
        return result;
    }

    private String handleAdvanceTime(Matcher matcher) {
        long ticks = Long.parseLong(matcher.group("count"));
        game.advance(ticks);
        StringBuilder output = new StringBuilder();
        for (String event : GameEvents.drain()) {
            output.append(event).append('\n');
        }
        output.append("time advanced by ").append(ticks)
                .append(" ticks; current tick: ").append(game.getCurrentTick());
        return output.toString();
    }

    private String handlePluck(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        Plant plucked = board.removeTopPlant(x, y);
        if (plucked == null) {
            return "there is no plant in tile (" + x + ", " + y + ")!";
        }
        game.unregister(plucked);
        return "plucked " + plucked.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    private String handleCollectSun(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        for (Plant plant : board.getTile(x, y).getPlants()) {
            if (plant.hasUncollectedSun()) {
                world.sunBank().add(plant.collectSun());
                return "collected sun; you now have " + world.sunBank().getBalance() + " sun";
            }
        }
        return "there is no sun at (" + x + ", " + y + ")!";
    }

    private String handleSpawnZombie(Matcher matcher) {
        String type = matcher.group("type").toLowerCase(Locale.ROOT);
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        if (!type.equals("normal")) {
            return "unknown zombie type: " + type + "!";
        }
        Zombie zombie = new BasicZombie();
        zombie.spawn(world, x, y);
        return "zombie " + zombie.getName() + " spawned at (" + x + ", " + y + ")";
    }

    private String handleShowMap() {//TODO: bada mishe bein plant zombie tile khali va .... rang entekhab kar behtar beshe
        StringBuilder output = new StringBuilder();
        output.append("tick: ").append(game.getCurrentTick())
                .append(" | sun: ").append(world.sunBank().getBalance()).append('\n');
        for (int y = 1; y <= board.getRows(); y++) {
            for (int x = 1; x <= board.getCols(); x++) {
                output.append('[').append(cellSymbol(x, y)).append(']');
            }
            output.append('\n');
        }
        output.append("Z = zombie, capital letter = plant's first," +
                " little letter = unnormal tile's first letter, . = normal tile's");
        return output.toString();
    }

    private char cellSymbol(int x, int y) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.getRow() == y && (int) Math.floor(zombie.getX()) == x) {
                return 'Z';
            }
        }
        List<Plant> plants = board.getTile(x, y).getPlants();
        if (!plants.isEmpty()) {
            return Character.toUpperCase(plants.get(0).getName().charAt(0));
        }
        Tile tile = board.getTile(x, y);
        if (tile.getType() != TileType.NORMAL) {
            return tile.getType().toString().toLowerCase().charAt(0);
        }
        return '.';
    }
}
