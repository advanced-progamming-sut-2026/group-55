package pvz.model.greenhouse;

public class Pot {
    private final int x;
    private final int y;
    private PotState state;
    private GreenhousePlant plant;

    public Pot(int x, int y, boolean isLocked) {
        this.x = x;
        this.y = y;
        this.state = isLocked ? PotState.LOCKED : PotState.EMPTY;
        this.plant = null;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public PotState getState() { return state; }
    public GreenhousePlant getPlant() { return plant; }

    public boolean isLocked() { return state == PotState.LOCKED; }
    public boolean isEmpty() { return state == PotState.EMPTY; }
    public boolean isReady() { return state == PotState.READY; }

    public void unlock() {
        if (this.state == PotState.LOCKED) {
            this.state = PotState.EMPTY;
        }
    }

    public void setPlant(GreenhousePlant plant) {
        this.plant = plant;
        this.state = PotState.GROWING;
    }

    public void clear() {
        this.plant = null;
        this.state = PotState.EMPTY;
    }

    public void updateState() {
        if (state == PotState.GROWING && plant != null && plant.isReady()) {
            state = PotState.READY;
        }
    }
}