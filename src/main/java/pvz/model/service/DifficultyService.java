package pvz.model.service;

public class DifficultyService {

    public static double getIncreaseMultiplier(int dl) {
        return (double) dl / 3.0;
    }

    public static double getDecreaseMultiplier(int dl) {return 3.0 / dl;}
}