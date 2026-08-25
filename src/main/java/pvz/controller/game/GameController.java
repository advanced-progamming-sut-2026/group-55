package pvz.controller.game;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;

import pvz.model.core.board.Board;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileOverlay;
import pvz.model.core.board.TileType;
import pvz.model.core.World;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.plantfood.PlantFood;
import pvz.model.entity.collectible.sun.SunCollectionOutcome;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ArmorInstance;
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
        if (GameCommand.SHOW_PLANTS_STATUS.getMatcher(input) != null) {
            return handleShowPlantsStatus();
        }
        if ((matcher = GameCommand.SHOW_TILE_STATUS.getMatcher(input)) != null) {
            return handleShowTileStatus(matcher);
        }
        if (GameCommand.SHOW_ZOMBIES.getMatcher(input) != null) {
            return handleShowZombies();
        }
        if ((matcher = GameCommand.PLUCK.getMatcher(input)) != null) {
            return handlePluck(matcher);
        }
        if (GameCommand.SHOW_SUN.getMatcher(input) != null) {
            return "you have " + world.sunBank().getBalance() + " sun";
        }
        if ((matcher = GameCommand.ADD_SUN.getMatcher(input)) != null) {
            return handleAddSun(matcher);
        }
        if ((matcher = GameCommand.COLLECT_SUN.getMatcher(input)) != null) {
            return handleCollectSun(matcher);
        }
        if ((matcher = GameCommand.COLLECT_PLANT_FOOD.getMatcher(input)) != null) {
            return handleCollectPlantFood(matcher);
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
        if (GameCommand.RELEASE_NUKE.getMatcher(input) != null) {
            return handleReleaseNuke();
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

    private String handleAddSun(Matcher matcher) {
        int count = Integer.parseInt(matcher.group("count"));

        world.sunBank().add(count);

        return "sun added! you now have "
                + world.sunBank().getBalance()
                + " sun.";
    }

    private String handleCollectPlantFood(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }

        for (Collectible collectible : world.getCollectibles()) {
            if (!(collectible instanceof PlantFood plantFood)) {
                continue;
            }

            if (plantFood.getTileX() != x || plantFood.getTileY() != y) {
                continue;
            }

            world.collectPlantFood(plantFood);

            return "collected plant food; you now have "
                    + session.resources().getPlantFoodCount()
                    + " plant food(s)";
        }

        return "there is no plant food at ("
                + x
                + ", "
                + y
                + ")!";
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
            if (!plant.canApplyPlantFood(currentTick)) {
                return "plant food cannot be applied to "
                        + plant.getName()
                        + " while it is frozen or covered by an octopus!";
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

        Zombie zombie;
        try {
            zombie = session.createZombie(type);
        } catch (UnsupportedOperationException exception) {
            return exception.getMessage() + "!";
        }

        if (zombie == null) {
            return "unknown zombie type: " + type + "!";
        }

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
                .append(" | wave: ")
                .append(session.waveManager().getCurrentWaveNumber())
                .append("/")
                .append(session.waveManager().getTotalWaves())
                .append(" (")
                .append(session.waveManager().getState())
                .append(")")
                .append(" | sun: ")
                .append(world.sunBank().getBalance())
                .append(" | plant food: ")
                .append(session.resources().getPlantFoodCount())
                .append('\n');

        output.append("lawn mowers: ");
        for (int row = 1; row <= board.getRows(); row++) {
            if (row > 1) {
                output.append(", ");
            }
            output.append("row ")
                    .append(row)
                    .append('=')
                    .append(world.isLawnMowerAvailable(row)
                            ? "ready"
                            : "used");
        }
        output.append('\n');

        for (int y = 1; y <= board.getRows(); y++) {
            for (int x = 1; x <= board.getCols(); x++) {
                output.append('[').append(cellSymbol(x, y)).append(']');
            }
            output.append('\n');
        }

        output.append("Z = zombie, O = pushed obstacle, ")
                .append("capital letter = plant's first, ")
                .append("lowercase letter = non-normal tile's first, ")
                .append(". = normal tile");

        return output.toString();
    }

    private String handleShowPlantsStatus() {
        List<String> selectedPlants = session.config().selectedPlants();

        if (selectedPlants.isEmpty()) {
            return "there are no selected plants for this level!";
        }

        StringBuilder output = new StringBuilder();
        long currentSun = world.sunBank().getBalance();

        for (String selectedPlant : selectedPlants) {
            Plant plant = session.createPlant(selectedPlant);
            if (plant == null) {
                output.append(selectedPlant)
                        .append(": unavailable (unknown plant data)\n");
                continue;
            }

            long rechargeTicks = (long) Math.ceil(
                    plant.getSpec().getRecharge()
                            * Game.TICKS_PER_SECOND
            );
            long remainingTicks = session.getRemainingRechargeTicks(
                    selectedPlant,
                    rechargeTicks
            );
            boolean affordable = currentSun >= plant.getSpec().getCost();

            output.append(plant.getName())
                    .append(": cost=")
                    .append(plant.getSpec().getCost())
                    .append(", recharge=")
                    .append(formatDuration(remainingTicks))
                    .append(", affordable=")
                    .append(affordable ? "yes" : "no")
                    .append(", status=")
                    .append(remainingTicks == 0 && affordable
                            ? "ready"
                            : "unavailable")
                    .append('\n');
        }

        return output.toString().stripTrailing();
    }

    private String handleShowTileStatus(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));

        if (!board.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }

        Tile tile = board.getTile(x, y);
        StringBuilder output = new StringBuilder();
        output.append("tile (")
                .append(x)
                .append(", ")
                .append(y)
                .append("):\n")
                .append("\ttype: ")
                .append(tile.getType().name().toLowerCase(Locale.ROOT))
                .append("\n\thealth: ")
                .append(formatNumber(tile.getHealth()))
                .append("\n\toverlays:\n");

        List<TileOverlay> overlays = tile.getOverlays();
        if (overlays.isEmpty()) {
            output.append("\t\tnone\n");
        } else {
            for (TileOverlay overlay : overlays) {
                output.append("\t\t")
                        .append(overlay.getType().name()
                                .toLowerCase(Locale.ROOT))
                        .append(": health=")
                        .append(formatNumber(overlay.getRemainingHealth()))
                        .append('/')
                        .append(formatNumber(
                                overlay.getType().getInitialHealth()
                        ))
                        .append(", plant=")
                        .append(overlay.getCoveredPlant().getName())
                        .append("\n");
            }
        }

        output.append("\tplants:\n");

        List<Plant> plants = tile.getPlants();
        if (plants.isEmpty()) {
            output.append("\t\tnone\n");
        } else {
            for (Plant plant : plants) {
                output.append("\t\t")
                        .append(plant.getName())
                        .append(": health=")
                        .append(formatNumber(plant.getHealth()))
                        .append('/')
                        .append(plant.getSpec().getBaseHp())
                        .append(", category=")
                        .append(plant.getSpec().getCategory().name()
                                .toLowerCase(Locale.ROOT))
                        .append(", freezeLevel=")
                        .append(plant.getFreezeLevel())
                        .append("\n");
            }
        }

        List<Zombie> zombies = world.getZombies().stream()
                .filter(zombie -> zombie.getTileX() == x
                        && zombie.getTileY() == y)
                .toList();
        List<PushedObstacle> obstacles = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getTileX() == x
                        && obstacle.getTileY() == y)
                .toList();
        output.append("\tpushed obstacles:\n");
        if (obstacles.isEmpty()) {
            output.append("\t\tnone\n");
        } else {
            for (PushedObstacle obstacle : obstacles) {
                output.append("\t\t")
                        .append(obstacle.getName())
                        .append(": health=")
                        .append(formatNumber(obstacle.getHealth()))
                        .append('/')
                        .append(formatNumber(obstacle.getMaximumHealth()))
                        .append("\n");
            }
        }
        output.append("\tzombies:\n");
        if (zombies.isEmpty()) {
            output.append("\t\tnone");
        } else {
            for (Zombie zombie : zombies) {
                output.append("\t\t")
                        .append(zombie.getName())
                        .append(": position=")
                        .append(formatNumber(zombie.getX()))
                        .append(", ")
                        .append(formatNumber(zombie.getY()))
                        .append(", health=")
                        .append(formatNumber(zombie.getHealth()))
                        .append(", armor=")
                        .append(formatNumber(zombie.getArmorHealth()))
                        .append(", speed=")
                        .append(formatNumber(zombie.getRuntimeStats()
                                .tilesPerSecond()))
                        .append(", eatDps=")
                        .append(formatNumber(zombie.getRuntimeStats()
                                .eatDamagePerSecond()))
                        .append("\n");
            }
        }

        return output.toString().stripTrailing();
    }

    private String handleShowZombies() {
        List<Zombie> zombies = world.getZombies();

        if (zombies.isEmpty()) {
            return "there are no zombies on the board!";
        }

        StringBuilder output = new StringBuilder();

        long currentTick = game.getCurrentTick();

        for (Zombie zombie : zombies) {

            output.append(zombie.getName())
                    .append(":\n");

            output.append("\tposition: ")
                    .append(zombie.getTileX())
                    .append(", ")
                    .append(zombie.getTileY())
                    .append("\n");

            output.append("\thealth: ")
                    .append(zombie.getHealth())
                    .append("\n");

            output.append("\tarmor:\n");

            List<ArmorInstance> intactArmors = zombie.getArmorSet()
                    .layers()
                    .stream()
                    .filter(armor -> !armor.isBroken())
                    .toList();

            if (intactArmors.isEmpty()) {
                output.append("\t\tnone\n");
            }

            for (ArmorInstance armor : intactArmors) {
                output.append("\t\t")
                        .append(armor.spec().name().toLowerCase())
                        .append(": ")
                        .append(armor.remainingHealth())
                        .append("/")
                        .append(armor.spec().maxHealth())
                        .append("\n");
            }

            output.append("\teffects:\n");

            if (zombie.isGlowing()) {
                output.append("\t\tglowing: plant-food carrier\n");
            }

            if (zombie.isChilled(currentTick)) {
                appendEffect(
                        output,
                        "chilled",
                        zombie.getRemainingChillTicks(currentTick)
                );
            }

            if (zombie.isFrozen(currentTick)) {
                appendEffect(
                        output,
                        "frozen",
                        zombie.getRemainingFreezeTicks(currentTick)
                );
            }

            if (zombie.isButtered(currentTick)) {
                appendEffect(
                        output,
                        "buttered",
                        zombie.getRemainingButterTicks(currentTick)
                );
            }

            if (zombie.isPoisoned(currentTick)) {
                appendEffect(
                        output,
                        "poisoned",
                        zombie.getRemainingPoisonTicks(currentTick)
                );
            }

            output.append("\n");
        }

        return output.toString();
    }

    private String handleReleaseNuke() {
        int killedZombies = world.eliminateAllZombies();
        session.advance(0);

        StringBuilder output = new StringBuilder();
        for (String event : GameEvents.drain()) {
            output.append(event).append('\n');
        }
        output.append("nuke released; killed ")
                .append(killedZombies)
                .append(" zombie(s).");

        return output.toString();
    }

    private void appendEffect(
            StringBuilder output,
            String effectName,
            long remainingTicks
    ) {
        output.append("\t\t")
                .append(effectName)
                .append(": ")
                .append(formatDuration(remainingTicks))
                .append(" remaining\n");
    }

    private String formatDuration(long ticks) {
        double seconds = ticks / (double) Game.TICKS_PER_SECOND;

        if (seconds == Math.rint(seconds)) {
            return (long) seconds + "s";
        }

        return String.format(Locale.ROOT, "%.1fs", seconds);
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }

        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private char cellSymbol(int x, int y) {
        for (Zombie zombie : world.getZombies()) {
            if (zombie.getTileY() == y && zombie.getTileX() == x) {
                return 'Z';
            }
        }

        for (PushedObstacle obstacle : world.getPushedObstacles()) {
            if (obstacle.getTileY() == y && obstacle.getTileX() == x) {
                return 'O';
            }
        }

        Tile tile = board.getTile(x, y);
        List<TileOverlay> overlays = tile.getOverlays();
        if (!overlays.isEmpty()) {
            return overlays.getLast().getType().name()
                    .toLowerCase(Locale.ROOT).charAt(0);
        }

        List<Plant> plants = tile.getPlants();
        if (!plants.isEmpty()) {
            return Character.toUpperCase(plants.getFirst().getName().charAt(0));
        }

        if (tile.getType() != TileType.NORMAL) {
            return tile.getType().name().toLowerCase(Locale.ROOT).charAt(0);
        }

        return '.';
    }
}
