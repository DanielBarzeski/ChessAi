package LookUpTables;

public class QueenMoves {
    public static long getPossibleMoves(int square, long occupancy) {
        return RookMoves.getPossibleMoves(square, occupancy) | BishopMoves.getPossibleMoves(square, occupancy);
    }
}
