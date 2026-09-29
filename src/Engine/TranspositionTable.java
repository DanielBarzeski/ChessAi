package Engine;

public class TranspositionTable {

    public static final int EXACT = 0;
    public static final int UPPER_BOUND = 1;
    public static final int LOWER_BOUND = 2;

    public static final long INVALID_ENTRY = 0L;
    private static final int SCORE_OFFSET = 32000;

    private final long[] keys;
    private final long[] data;
    private final int sizeMask;
    private int currentAge;

    public TranspositionTable(int sizeInBits) {
        int entries = 1 << sizeInBits;
        sizeMask = entries - 1;
        keys = new long[entries * 2];
        data = new long[entries * 2];
        currentAge = 0;
    }

    public void incrementAge() {
        currentAge++;
    }

    public void store(long zobristKey, int depth, int score, int flag, int bestMove) {
        int index = ((int) zobristKey & sizeMask) * 2;
        long packedData = pack(depth, score, flag, bestMove, currentAge);

        if (keys[index] == zobristKey) {
            if (depth >= extractDepth(data[index]) || extractAge(data[index]) != currentAge) {
                data[index] = packedData;
            }
            return;
        }

        if (keys[index + 1] == zobristKey) {
            data[index + 1] = packedData;
            return;
        }

        int storedDepth = extractDepth(data[index]);
        int storedAge = extractAge(data[index]);

        if (keys[index] == 0L || depth >= storedDepth || storedAge != currentAge) {
            keys[index + 1] = keys[index];
            data[index + 1] = data[index];

            keys[index] = zobristKey;
            data[index] = packedData;
            return;
        }

        keys[index + 1] = zobristKey;
        data[index + 1] = packedData;
    }

    public long probe(long zobristKey) {
        int index = ((int) zobristKey & sizeMask) * 2;

        if (keys[index] == zobristKey) {
            data[index] = updateAge(data[index], currentAge);
            return data[index];
        }

        if (keys[index + 1] == zobristKey) {
            return data[index + 1];
        }

        return INVALID_ENTRY;
    }

    private long pack(int depth, int score, int flag, int bestMove, int age) {
        int positiveScore = score + SCORE_OFFSET;
        long packed = 0L;
        packed |=  (positiveScore & 0xFFFF);            // 16 bits (0-15)
        packed |= ((long) (depth & 0xFF) << 16);        // 8 bits  (16-23)
        packed |= ((long) (flag & 0x3) << 24);          // 2 bits  (24-25)
        packed |= ((long) (bestMove & 0x1FFFFF) << 26); // 21 bits (26-46)
        packed |= ((long) (age & 0xFF) << 47);          // 8 bits  (47-54)
        return packed;
    }

    public static int extractScore(long packedData) {
        return (int) (packedData & 0xFFFF) - SCORE_OFFSET;
    }

    public static int extractDepth(long packedData) {
        return (int) ((packedData >>> 16) & 0xFF);
    }

    public static int extractFlag(long packedData) {
        return (int) ((packedData >>> 24) & 0x3);
    }

    public static int extractBestMove(long packedData) {
        return (int) ((packedData >>> 26) & 0x1FFFFF);
    }

    private static int extractAge(long packedData) {
        return (int) ((packedData >>> 47) & 0xFF);
    }

    private long updateAge(long packedData, int newAge) {
        long mask = ~(((long) 0xFF) << 47);
        return (packedData & mask) | ((long) (newAge & 0xFF) << 47);
    }
}