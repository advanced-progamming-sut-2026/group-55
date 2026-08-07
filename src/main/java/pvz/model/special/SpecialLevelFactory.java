package pvz.model.special;

public final class SpecialLevelFactory {


    private SpecialLevelFactory() {
    }


    public static LevelRule createRule(
            SpecialLevelType type
    ) {

        return switch (type) {

            case CONVEYOR_BELT ->
                    new ConveyorBeltRule();

            case LOCKED_PLANTS ->
                    new LockedPlantsRule();

            case SAVE_OUR_SEEDS ->
                    new SaveOurSeedsRule();

            case TIMED_WAR ->
                    new TimedWarRule();

            case NIGHT_OPS ->
                    new NightOpsRule();

            case DEAD_LINE ->
                    new DeadLineRule();

            case LOVE_YOUR_PLANTS ->
                    new LoveYourPlantsRule();

            case PLANT_WHAT_YOU_GET ->
                    new PlantWhatYouGetRule();
        };
    }
}
