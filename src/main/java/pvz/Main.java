package pvz;

import pvz.model.core.Game;

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.register(tick -> System.out.println("tick: " + tick));
        game.advance(5);
    }
}