package pvz.model.core;

public final class GameStateManager {

    private GameStatus status = GameStatus.PLAYING;

    public GameStatus getStatus() {
        return status;
    }

    public void win() {
        status = GameStatus.WON;
    }

    public void lose() {
        status = GameStatus.LOST;
    }

    public boolean isRunning() {
        return status == GameStatus.PLAYING;
    }
}
