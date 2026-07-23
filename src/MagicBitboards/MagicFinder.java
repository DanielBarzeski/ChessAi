package MagicBitboards;

import java.util.Arrays;
import java.util.Random;

public class MagicFinder {
    private static final Random random = new Random();

    private static final int BOARD_SIZE = 64;
    private static final int BOARD_DIMENSION = 8;
    private static final int MIN_HIGH_BITS = 6;
    private static final int MAX_ATTEMPTS = 100_000_000;
    private static final long HIGH_BITS_MASK = 0xFF00_0000_0000_0000L;

    public static long[] findRookMagics(long[] masks) {
        System.out.println("=== Searching Magic Numbers ===\n");

        long[] magics = new long[BOARD_SIZE];

        for (int square = 0; square < BOARD_SIZE; square++) {
            magics[square] = findMagicForSquare(square, masks[square]);
        }

        System.out.println("\nFound Magic Numbers!\n");
        return magics;
    }

    private static long findMagicForSquare(int square, long mask) {
        int bits = Long.bitCount(mask);

        System.out.print("square " + square + " (" + squareName(square) + "): ");
        System.out.print("search bits: "+ bits);

        long startTime = System.currentTimeMillis();
        long magic = findMagicNumber(square, mask, bits);
        long endTime = System.currentTimeMillis();

        if (magic != 0L) {
            System.out.println("Found in "+ (endTime - startTime) + " ms)");
        } else {
            System.out.println("Failed");
        }
        return magic;
    }

    public static void printMagics(long[] magics) {
        System.out.println("=== Compact Format of Magic Numbers  ===\n");

        for (int rank = 0; rank < BOARD_DIMENSION; rank++) {
            System.out.print("    ");
            for (int file = 0; file < BOARD_DIMENSION; file++) {
                int square = rank * BOARD_DIMENSION + file;
                System.out.print(formatMagicHex(magics[square]));

                if (square < BOARD_SIZE - 1) {
                    System.out.print(", ");
                }
                if (file == BOARD_DIMENSION - 1 && rank < BOARD_DIMENSION - 1) {
                    System.out.println();
                }
            }
        }

        System.out.println("\n};");
        System.out.println("\n=== Magic Numbers ===\n");
        System.out.println("public static final long[] ROOK_MAGICS = {");

        for (int rank = 0; rank < BOARD_DIMENSION; rank++) {
            System.out.println("        // Rank " + (rank + 1));

            for (int file = 0; file < BOARD_DIMENSION; file++) {
                int square = rank * BOARD_DIMENSION + file;
                System.out.print("        " + formatMagicHex(magics[square]));

                if (square < BOARD_SIZE - 1) {
                    System.out.print(",");
                }
                System.out.println(" // " + squareName(square) + " (" + square + ")");
            }

            if (rank < BOARD_DIMENSION - 1) {
                System.out.println();
            }
        }

        System.out.println("};");
    }

    private static long findMagicNumber(int square, long mask, int bits) {
        int occupancyCount = 1 << bits;
        long[] occupancies = new long[occupancyCount];
        long[] attacks = new long[occupancyCount];
        long[] used = new long[occupancyCount];

        initializeOccupanciesAndAttacks(square, mask, occupancyCount, occupancies, attacks);

        return searchForValidMagic(occupancyCount, occupancies, attacks, used, bits, mask);
    }

    private static void initializeOccupanciesAndAttacks(int square, long mask, int occupancyCount,
                                                        long[] occupancies, long[] attacks) {
        for (int i = 0; i < occupancyCount; i++) {
            occupancies[i] = getOccupancyVariation(i, mask);
            attacks[i] = generateRookAttacks(square, occupancies[i]);
        }
    }

    private static long searchForValidMagic(int occupancyCount, long[] occupancies,
                                            long[] attacks, long[] used, int bits, long mask) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long candidateMagic = randomLong();

            if (!hasEnoughHighBits(candidateMagic, mask)) {
                continue;
            }

            Arrays.fill(used, 0L);

            if (isValidMagic(candidateMagic, occupancyCount, occupancies, attacks, used, bits)) {
                return candidateMagic;
            }
        }

        return 0L;
    }

    private static boolean hasEnoughHighBits(long magic, long mask) {
        long highBits = (mask * magic) & HIGH_BITS_MASK;
        return Long.bitCount(highBits) >= MIN_HIGH_BITS;
    }

    private static boolean isValidMagic(long magic, int occupancyCount, long[] occupancies,
                                        long[] attacks, long[] used, int bits) {
        for (int i = 0; i < occupancyCount; i++) {
            int index = computeMagicIndex(occupancies[i], magic, bits);

            if (used[index] == 0L) {
                used[index] = attacks[i];
            } else if (used[index] != attacks[i]) {
                return false;
            }
        }
        return true;
    }

    private static int computeMagicIndex(long occupancy, long magic, int bits) {
        return (int) ((occupancy * magic) >>> (BOARD_SIZE - bits));
    }

    private static long randomLong() {
        return random.nextLong() & random.nextLong() & random.nextLong();
    }

    private static long getOccupancyVariation(int index, long mask) {
        long occupancy = 0L;
        int[] activeBits = extractActiveBits(mask);

        for (int i = 0; i < activeBits.length; i++) {
            if ((index & (1 << i)) != 0) {
                occupancy |= (1L << activeBits[i]);
            }
        }

        return occupancy;
    }

    private static int[] extractActiveBits(long mask) {
        int[] bits = new int[BOARD_SIZE];
        int bitCount = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            if ((mask & (1L << i)) != 0) {
                bits[bitCount++] = i;
            }
        }

        return Arrays.copyOf(bits, bitCount);
    }

    private static long generateRookAttacks(int square, long occupancy) {
        int rank = square / BOARD_DIMENSION;
        int file = square % BOARD_DIMENSION;

        long attacks = 0L;
        attacks |= attacksInDirection(rank, file, occupancy, 1, 0);    // למעלה
        attacks |= attacksInDirection(rank, file, occupancy, -1, 0);   // למטה
        attacks |= attacksInDirection(rank, file, occupancy, 0, 1);    // ימינה
        attacks |= attacksInDirection(rank, file, occupancy, 0, -1);   // שמאלה

        return attacks;
    }

    private static long attacksInDirection(int startRank, int startFile, long occupancy,
                                           int rankDelta, int fileDelta) {
        long attacks = 0L;
        int currentRank = startRank + rankDelta;
        int currentFile = startFile + fileDelta;

        while (isOnBoard(currentRank, currentFile)) {
            int targetSquare = currentRank * BOARD_DIMENSION + currentFile;
            attacks |= (1L << targetSquare);

            if ((occupancy & (1L << targetSquare)) != 0) {
                break;
            }

            currentRank += rankDelta;
            currentFile += fileDelta;
        }

        return attacks;
    }

    private static boolean isOnBoard(int rank, int file) {
        return rank >= 0 && rank < BOARD_DIMENSION && file >= 0 && file < BOARD_DIMENSION;
    }

    private static String squareName(int square) {
        int file = square % BOARD_DIMENSION;
        int rank = square / BOARD_DIMENSION;
        return "" + (char) ('a' + file) + (rank + 1);
    }

    private static String formatMagicHex(long magic) {
        return "0x" + String.format("%016X", magic) + "L";
    }
}