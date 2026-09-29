package Board;


import static Data.Game.CELL_SIZE;

public class Piece {
    private int currX, currY;
    private final int prevX, prevY;
    private final int value;
    private final boolean white;

    public Piece(int x, int y, int value) {
        resetCurrLocation();
        this.prevX = x;
        this.prevY = y;
        this.white = value < 7;
        this.value = value;
    }

    public boolean inPrevCell(int col, int row) {
        return col == getPrevCol() && row == getPrevRow();
    }

    public int getCurrSquare() {
        return getCurrRow() * 8 + getCurrCol();
    }

    public int getPrevSquare() {
        return getPrevRow() * 8 + getPrevCol();
    }

    public int getCurrX() {
        return currX;
    }

    public int getCurrCol() {
        return currX / CELL_SIZE;
    }

    public int getCurrY() {
        return currY;
    }

    public int getCurrRow() {
        return currY / CELL_SIZE;
    }

    public int getPrevCol() {
        return prevX / CELL_SIZE;
    }

    public int getPrevRow() {
        return prevY / CELL_SIZE;
    }

    public void setCurrLocation(int currX, int currY) {
        this.currX = currX;
        this.currY = currY;
    }

    public void resetCurrLocation() {
        this.currX = -1 * CELL_SIZE;
        this.currY = -1 * CELL_SIZE;
    }

    public boolean isWhite() {
        return white;
    }

    public int getValue() {
        return value;
    }
}
