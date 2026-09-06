package pvz.model.adventure;

/**
 * High-level route used after a level is selected in the Adventure menu.
 *
 * <p>Only the normal-level route is executable in the current vertical
 * slice. The unsupported route is intentional: future special or boss data
 * must not silently run through the normal plant-selection flow.</p>
 */
public enum LevelEntryRoute {
    PLANT_SELECTION,
    UNSUPPORTED_SETUP
}
