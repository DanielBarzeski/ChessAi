package Logic;

public class BookMove {
    private final int moveBits;
    private final int weight;

    public BookMove(int moveBits, int weight) {
        this.moveBits = moveBits;
        this.weight = weight;
    }

    public int getMove() {
        return moveBits;
    }

    public int getWeight() {
        return weight;
    }
}
