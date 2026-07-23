package MagicBitboards;

public class Tests {

    public static void printLoadingStats(long[][] ATTACKS) {
        int totalEntries = 0;
        int loadedSquares = 0;

        for (int i = 0; i < 64; i++) {
            if (ATTACKS[i] != null) {
                loadedSquares++;
                totalEntries += ATTACKS[i].length;
            }
        }

        System.out.println("\n=== Statistics ===");
        System.out.println("Loaded squares: " + loadedSquares + " / 64");
        System.out.println("Total entries" + totalEntries);
        System.out.println("Total memory" + (totalEntries * 8 / 1024) + " KB");
    }

    public static void testLoadedAttacks(long[][] ATTACKS) {
        System.out.println("\n=== Checking array ===\n");

        for (int square = 0; square < 64; square++) {
            if (ATTACKS[square] != null) {
                String squareName = getSquareName(square);
                System.out.println("Square " + square + " (" + squareName + "): "
                        + ATTACKS[square].length + " entries");

                for (int i = 0; i < Math.min(3, ATTACKS[square].length); i++) {
                    System.out.println("  [" + i + "] = " + formatHex(ATTACKS[square][i]));
                }
            }
        }
    }

    private static String formatHex(long value) {
        return String.format("0x%016XL", value);
    }
    public static void printBoard(int[] piecesValues) {
        final String[] PIECE_SYMBOLS = {
                "X", "P", "N", "B", "R", "Q", "K",  // לבן (0-5)
                "?", "?", "p", "n", "b", "r", "q", "k",  // שחור (6-11)
        };

        System.out.println("\n  a b c d e f g h");
        System.out.println(" +---------------+");

        for (int rank = 0; rank < 8; rank++) {  // מ-7 ל-0 (שורה 8 עד 1)
            System.out.print((rank + 1) + "|");

            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                int piece = piecesValues[square];

                String symbol = getPieceSymbol(piece, PIECE_SYMBOLS);
                System.out.print(symbol + " ");
            }

            System.out.println("|" + (rank + 1));
        }

        System.out.println(" +---------------+");
        System.out.println("  a b c d e f g h\n");
    }
    public static void printMove(int move) {
        // Decode fields using shifts and masks
        int from           = move & 0x3F;                 // First 6 bits (0x3F = 0b111111)
        int to             = (move >> 6) & 0x3F;          // Next 6 bits
        int movingPiece    = (move >> 12) & 0x0F;         // 4 bits (0x0F = 0b1111)
        int captured       = (move >> 16) & 0x0F;         // 4 bits
        int promotion      = (move >> 20) & 0x0F;         // 4 bits
        int castling       = (move >> 24) & 0x03;         // 2 bits (0x03 = 0b11)
        int castlingRights = (move >> 26) & 0x0F;         // 4 bits
        int enPassant      = (move >> 30) & 0x01;         // Last 1 bit

        // Print move details
        System.out.println("=== Move Details ===");
        System.out.printf("From square:     %d (%s)%n", from, getSquareNotation(from));
        System.out.printf("To square:       %d (%s)%n", to, getSquareNotation(to));
        System.out.printf("Moving piece:    %d%n", movingPiece);
        System.out.printf("Captured piece:  %s%n", (captured == 0 ? "None" : String.valueOf(captured)));
        System.out.printf("Promotion:       %s%n", (promotion == 0 ? "None" : String.valueOf(promotion)));
        System.out.printf("Castling:        %s%n", getCastlingType(castling));
        System.out.printf("Castling Rights: %s%n", getCastlingRightsString(castlingRights));
        System.out.printf("En Passant:      %s%n", (enPassant == 1 ? "Yes" : "No"));
        System.out.println("====================");
    }

    // Converts index 0-63 to standard chess coordinate notation (e.g., 0 -> a1, 8 -> a2)
    private static String getSquareNotation(int square) {
        if (square < 0 || square > 63) return "Invalid";
        char file = (char) ('a' + (square % 8)); // Column (a-h)
        int rank = (square / 8) + 1;             // Row (1-8)
        return "" + file + rank;
    }

    // Identifies the castling type
    private static String getCastlingType(int castling) {
        return switch (castling) {
            case 1 -> "Queenside";
            case 2 -> "Kingside";
            default -> "None";
        };
    }

    // Displays remaining castling rights as a KQkq string
    private static String getCastlingRightsString(int rights) {
        String sb = ((rights & 1) != 0 ? "K" : "-") + // White King
                ((rights & 2) != 0 ? "Q" : "-") + // White Queen
                ((rights & 4) != 0 ? "k" : "-") + // Black King
                ((rights & 8) != 0 ? "q" : "-"); // Black Queen
        return sb + " (0b" + Integer.toBinaryString(rights) + ")";
    }
    private static String getPieceSymbol(int piece, String[] symbols) {
        if (piece >= 0 && piece < symbols.length) {
            return symbols[piece];
        }
        return "?";
    }
    public static String getSquareName(int square) {
        int file = square % 8;
        int rank = square / 8;
        return "" + (char)('a' + file) + (rank + 1);
    }
    public static void printBitboard(long bitboard) {
        System.out.println("  a b c d e f g h");

        int startRank = 0;
        int endRank = 8;
        int stepRank = 1;

        for (int rank = startRank; rank != endRank; rank += stepRank) {
            System.out.print((rank + 1) + " ");
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                boolean isSet = ((bitboard >>> square) & 1L) == 1L;
                System.out.print(isSet ? "X " : ". ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
