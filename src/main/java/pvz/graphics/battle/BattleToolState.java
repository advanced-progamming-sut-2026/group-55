package pvz.graphics.battle;

import java.util.Locale;

/** Pure presentation state for the tool controls in the battle HUD. */
public final class BattleToolState {
    private BattleToolState() {
    }

    public static View resolve(
            Selection selection,
            String selectedPlant,
            int plantFoodCount
    ) {
        if (selection == null) {
            throw new IllegalArgumentException("selection cannot be null");
        }
        if (plantFoodCount < 0) {
            throw new IllegalArgumentException(
                    "plant food count cannot be negative"
            );
        }

        String selectionText = switch (selection) {
            case NONE -> "SELECTED TOOL: NONE";
            case PLANT -> "SELECTED PLANT: " + normalizedPlant(selectedPlant);
            case SHOVEL -> "SELECTED TOOL: SHOVEL";
            case PLANT_FOOD -> plantFoodCount > 0
                    ? "SELECTED TOOL: PLANT FOOD"
                    : "SELECTED TOOL: NONE";
        };

        ButtonView shovel = new ButtonView(
                selection == Selection.SHOVEL,
                true,
                selection == Selection.SHOVEL ? "ACTIVE" : "READY"
        );
        boolean plantFoodAvailable = plantFoodCount > 0;
        boolean plantFoodSelected = selection == Selection.PLANT_FOOD
                && plantFoodAvailable;
        ButtonView plantFood = new ButtonView(
                plantFoodSelected,
                plantFoodAvailable,
                plantFoodSelected
                        ? "ACTIVE  x" + plantFoodCount
                        : plantFoodAvailable
                        ? "READY  x" + plantFoodCount
                        : "EMPTY"
        );
        return new View(selectionText, shovel, plantFood);
    }

    private static String normalizedPlant(String selectedPlant) {
        if (selectedPlant == null || selectedPlant.isBlank()) {
            return "NONE";
        }
        return selectedPlant.toUpperCase(Locale.ROOT);
    }

    public enum Selection {
        NONE,
        PLANT,
        SHOVEL,
        PLANT_FOOD
    }

    public record ButtonView(
            boolean selected,
            boolean available,
            String statusText
    ) {
    }

    public record View(
            String selectionText,
            ButtonView shovel,
            ButtonView plantFood
    ) {
    }
}
