package pvz.graphics.battle;

import java.util.List;
import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.session.GameSession;

/** Non-mutating target checks shared by the battle cursor presentation. */
public final class BattleCellTargeting {
    private BattleCellTargeting() {
    }

    public static boolean canPlant(
            GameSession session,
            String plantName,
            Plant candidate,
            int column,
            int row
    ) {
        Objects.requireNonNull(session, "session cannot be null");
        if (plantName == null || plantName.isBlank() || candidate == null) {
            return false;
        }
        if (!session.isPlantSelected(plantName)) {
            return false;
        }
        if (session.isPlantBoosted(plantName)
                && !candidate.supportsPlantFood()) {
            return false;
        }

        long rechargeTicks = (long) Math.ceil(
                candidate.getSpec().getRecharge() * Game.TICKS_PER_SECOND
        );
        if (session.getRemainingRechargeTicks(
                plantName,
                rechargeTicks
        ) > 0) {
            return false;
        }
        if (!session.resources().sunBank().canAfford(
                candidate.getSpec().getCost()
        )) {
            return false;
        }
        return session.board().canPlant(column, row, candidate);
    }

    public static boolean canShovel(Board board, int column, int row) {
        Objects.requireNonNull(board, "board cannot be null");
        if (!board.inBounds(column, row)) {
            return false;
        }
        Plant topPlant = board.getTopPlant(column, row);
        return topPlant != null
                && topPlant.canBeAffectedBy(PlantThreat.PLUCK);
    }

    public static boolean canUsePlantFood(
            GameSession session,
            int column,
            int row
    ) {
        Objects.requireNonNull(session, "session cannot be null");
        Board board = session.board();
        if (session.resources().getPlantFoodCount() <= 0
                || !board.inBounds(column, row)) {
            return false;
        }

        List<Plant> supportedPlants = board.getTile(column, row)
                .getPlants()
                .stream()
                .filter(Plant::supportsPlantFood)
                .toList();
        if (supportedPlants.isEmpty()) {
            return false;
        }

        long currentTick = session.game().getCurrentTick();
        return supportedPlants.stream().allMatch(plant ->
                !plant.isPlantFoodActive(currentTick)
                        && plant.canApplyPlantFood(currentTick));
    }
}
