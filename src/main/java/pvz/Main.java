package pvz;

import pvz.model.core.Board;
import pvz.model.core.Game;

public class Main {
    public static void main(String[] args) {
        Board board = new Board();
        System.out.println(board.getRows() + " x " + board.getCols());
        System.out.println(board.getTile(2, 4).isPlantable());
        System.out.println(board.inBounds(7, 3));
    }
}