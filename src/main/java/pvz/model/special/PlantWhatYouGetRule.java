package pvz.model.special;

public final class PlantWhatYouGetRule implements LevelRule {


    @Override
    public String getName() {
        return "Plant What You Get";
    }


    @Override
    public void apply() {
        // plants are randomly given to player
    }
}
