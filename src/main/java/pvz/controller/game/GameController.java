package pvz.controller.game;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;

import pvz.model.core.board.Board;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileType;
import pvz.model.core.World;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.BasicZombie;
import pvz.model.entity.zombie.Zombie;
import pvz.model.session.GameSession;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.lifecycle.PlantThreat;

public final class GameController {
    private final GameSession session;
    private final World world;
    private final Board board;
    private final Game game;

    public GameController(GameSession session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.world = session.world();
        this.board = session.board();
        this.game = session.game();
    }

    public String handle(String input) {
        if (!session.isRunning()) {
            return "game session is not running!";
        }

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
        if ((GameCommand.REMOVE_COOLDOWN.getMatcher(input)) != null) {
            return handleRemoveCooldown();
        }
        if (GameCommand.ADD_PLANT_FOOD.getMatcher(input) != null) {
            return handleAddPlantFood();
        }
        if ((matcher = GameCommand.FEED_PLANT.getMatcher(input)) != null) {
            return handleFeedPlant(matcher);
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

        Plant plant = session.createPlant(type);
        if (plant == null) {
            return "unknown plant type: " + type + "!";
        }
        if (!session.isPlantSelected(type)) {
            return type + " was not selected for this level!";
        }

        boolean boosted = session.isPlantBoosted(type);

        if (boosted && !plant.supportsPlantFood()) {
            return "plant food effect for "
                    + plant.getName()
                    + " is not implemented yet!";
        }

        long rechargeTicks = (long) Math.ceil(
                plant.getSpec().getRecharge() * Game.TICKS_PER_SECOND
        );

        long remainingTicks = session.getRemainingRechargeTicks(type, rechargeTicks);
        if (remainingTicks > 0) {
            return plant.getName()
                    + " is recharging; ready in "
                    + remainingTicks
                    + " ticks!";
        }

        int cost = plant.getSpec().getCost();
        if (!world.sunBank().canAfford(cost)) {
            return "not enough sun! (need "
                    + cost
                    + ", have "
                    + world.sunBank().getBalance()
                    + ")";
        }

        String result = board.plant(x, y, plant);
        if (!wasPlantPlaced(x, y, plant)) {
            return result;
        }

        world.sunBank().spend(cost);
        session.recordPlanting(type);
        plant.place(world, x, y, game.getCurrentTick());
        game.register(plant);

        if (boosted) {
            boolean activated = plant.tryApplyPlantFood(game.getCurrentTick());

            if (activated) {
                result += "\nBoost activated for "
                        + plant.getName()
                        + ": plant food effect applied automatically.";
            }
        }

        return result;
    }

    private boolean wasPlantPlaced(int x, int y, Plant plant) {
        return board.inBounds(x, y)
                && board.getTile(x, y).getPlants().contains(plant);
    }

    private String handleAdvanceTime(Matcher matcher) {
        long ticks = Long.parseLong(matcher.group("count"));
        session.advance(ticks);

        StringBuilder output = new StringBuilder();
        for (String event : GameEvents.drain()) {
            output.append(event).append('\n');
        }

        output.append("time advanced by ")
                .append(ticks)
                .append(" ticks; current tick: ")
                .append(game.getCurrentTick());

        return output.toString();
    }

    private String handlePluck(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }

        Plant plant = board.getTopPlant(x, y);

        if (plant == null) {
            return "there is no plant in tile (" + x + ", " + y + ")!";
        }

        PlantRemovalResult removalResult = plant.tryRemove(PlantThreat.PLUCK);

        return switch (removalResult) {

            case REMOVED ->
                    "plucked "
                            + plant.getName()
                            + " at ("
                            + x
                            + ", "
                            + y
                            + ") successfully!";

            case BLOCKED_BY_PLANT_FOOD ->
                    plant.getName() + " cannot be plucked while plant food is active!";

            case ALREADY_REMOVED ->
                    "the plant at ("
                            + x
                            + ", "
                            + y
                            + ") has already been removed!";
        };
    }

    private String handleCollectSun(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }

        for (Collectible collectible : world.getCollectibles()) {
            if (!(collectible instanceof Sun sun)) {
                continue;
            }
            if (sun.isRemoved()) {
                continue;
            }
            if (sun.getTileX() != x || sun.getTileY() != y) {
                continue;
            }

            return collectSun(sun, x, y);
        }

        return "there is no sun at (" + x + ", " + y + ")!";
    }

    private String collectSun(Sun sun, int x, int y) {
        SunCollectionOutcome outcome = world.collectSun(sun);

        if (outcome == SunCollectionOutcome.EXPLODED) {
            return "radioactive sun exploded at ("
                    + x
                    + ", "
                    + y
                    + "); no sun was added";
        }

        return "collected "
                + sun.getValue()
                + " sun; you now have "
                + world.sunBank().getBalance()
                + " sun";
    }

    private String handleRemoveCooldown() {
        if (session.resources().isCooldownCheatEnabled()) {
            return "plant cooldowns are already removed!";
        }

        session.resources().enableCooldownCheat();
        return "all plant cooldowns have been removed!";
    }

    private String handleAddPlantFood() {
        if (!session.resources().tryAddPlantFood()) {
            return "plant food storage is full! you already have "
                    + session.resources().getPlantFoodCount()
                    + " plant foods.";
        }

        return "plant food added! you now have "
                + session.resources().getPlantFoodCount()
                + " plant food(s).";
    }

    private String handleFeedPlant(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));

        int y = Integer.parseInt(matcher.group("y"));

        if (!board.inBounds(x, y)) {
            return "location ("
                    + x
                    + ", "
                    + y
                    + ") is out of bounds!";
        }

        List<Plant> plants = board.getTile(x, y).getPlants();

        if (plants.isEmpty()) {
            return "there is no plant at ("
                    + x
                    + ", "
                    + y
                    + ")!";
        }

        List<Plant> supportedPlants = plants.stream()
                .filter(Plant::supportsPlantFood)
                .toList();

        if (supportedPlants.isEmpty()) {
            return "none of the plants at ("
                    + x
                    + ", "
                    + y
                    + ") has an implemented plant food effect!";
        }

        long currentTick = game.getCurrentTick();

        for (Plant plant : supportedPlants) {
            if (plant.isPlantFoodActive(currentTick)) {
                return plant.getName() + " is already using plant food!";
            }
        }

        if (!session.resources().tryConsumePlantFood()) {
            return "you don't have any plant food!";
        }

        for (Plant plant : supportedPlants) {
            boolean activated = plant.tryApplyPlantFood(currentTick);

            if (!activated) {
                throw new IllegalStateException(
                        "plant food activation changed "
                                + "after validation for "
                                + plant.getName()
                );
            }
        }

        return "plant food applied to "
                + supportedPlants.size()
                + " compatible plant(s) at ("
                + x
                + ", "
                + y
                + ")! "
                + session.resources()
                .getPlantFoodCount()
                + " plant food(s) left.";
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

        return "zombie "
                + zombie.getName()
                + " spawned at ("
                + x
                + ", "
                + y
                + ")";
    }

    private String handleShowMap() {//TODO: bada mishe bein plant zombie tile khali va .... rang entekhab kard behtar beshe
        StringBuilder output = new StringBuilder();
        output.append("tick: ")
                .append(game.getCurrentTick())
                .append(" | sun: ")
                .append(world.sunBank().getBalance())
                .append(" | plant food: ")
                .append(session.resources().getPlantFoodCount())
                .append('\n');

        for (int y = 1; y <= board.getRows(); y++) {
            for (int x = 1; x <= board.getCols(); x++) {
                output.append('[').append(cellSymbol(x, y)).append(']');
            }
            output.append('\n');
        }

        output.append("Z = zombie, capital letter = plant's first, ")
                .append("lowercase letter = non-normal tile's first, ")
                .append(". = normal tile");

        return output.toString();
    }

    private char cellSymbol(int x, int y) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.getTileY() == y && zombie.getTileX() == x) {
                return 'Z';
            }
        }

        List<Plant> plants = board.getTile(x, y).getPlants();
        if (!plants.isEmpty()) {
            return Character.toUpperCase(plants.getFirst().getName().charAt(0));
        }

        Tile tile = board.getTile(x, y);
        if (tile.getType() != TileType.NORMAL) {
            return tile.getType().name().toLowerCase(Locale.ROOT).charAt(0);
        }

        return '.';
    }
}
