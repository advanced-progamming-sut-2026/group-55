package pvz;

import pvz.model.core.Board;
import pvz.model.core.Tile;
import pvz.model.core.TileType;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        Tile test = new Tile(TileType.FROZEN);
        test.takeDamage(400);
        System.out.println(test.getType());
        test.takeDamage(400);
        System.out.println(test.getType());
    }
}