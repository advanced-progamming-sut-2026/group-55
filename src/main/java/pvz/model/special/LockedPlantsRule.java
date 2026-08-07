package pvz.model.special;

public final class LockedPlantsRule implements LevelRule {


    @Override
    public String getName() {
        return "Locked Plants";
    }


    @Override
    public void apply() {
        // selected plants are locked for this level
    }
}
