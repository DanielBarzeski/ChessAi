package stockfish;

public class EngineAnalysis {
    private final int fromSquare;
    private final int toSquare;
    private final int promotion;
    private final int centipawns;
    private final int mateIn;
    private final boolean isMate;
    private final boolean isCheckmate;
    private final boolean isStalemate;

    public EngineAnalysis(int fromSquare, int toSquare, int promotion, int centipawns,
                          int mateIn, boolean isMate, boolean isCheckmate, boolean isStalemate) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotion = promotion;
        this.centipawns = centipawns;
        this.mateIn = mateIn;
        this.isMate = isMate;
        this.isCheckmate = isCheckmate;
        this.isStalemate = isStalemate;
    }

    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public int getPromotion() { return promotion; }
    public int getCentipawns() { return centipawns; }
    public int getMateIn() { return mateIn; }
    public boolean isMate() { return isMate; }
    public boolean isCheckmate() { return isCheckmate; }
    public boolean isStalemate() { return isStalemate; }
}