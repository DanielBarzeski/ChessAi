package stockfish;

public record EngineAnalysis(int fromSquare, int toSquare, int promotion, int centipawns, int mateIn, boolean isMate,
                             boolean isCheckmate, boolean isStalemate) {
}