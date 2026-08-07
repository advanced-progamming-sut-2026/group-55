package pvz.model.special;

public final class ConveyorBeltRule implements LevelRule {


    @Override
    public String getName() {
        return "Conveyor Belt";
    }


    @Override
    public void apply() {
        // plants are provided automatically over time
    }
}
