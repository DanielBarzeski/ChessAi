package LookUpTables;

public class Evaluation {
    public static final int[] PIECE_VALUES_MG = {0, 100, 320, 330, 500, 900, 0, 0, 0, 100, 320, 330, 500, 900, 0};
    public static final int[] PIECE_VALUES_EG = {0, 100, 300, 300, 500, 900, 0, 0, 0, 100, 300, 300, 500, 900, 0};
    public static final int[] PHASE_WEIGHTS = new int[]{0, 0, 1, 1, 2, 4, 0, 0, 0, 0, 1, 1, 2, 4, 0};
    public static final long[][] KING_SHIELD_MASKS = new long[2][64];
    public static final int KING_SHIELD_BONUS_MG = 25;
    public static final int ISOLATED_PAWN_PENALTY_MG = 12;
    public static final int ISOLATED_PAWN_PENALTY_EG = 18;
    public static final int DOUBLED_PAWN_PENALTY_MG = 10;
    public static final int DOUBLED_PAWN_PENALTY_EG = 15;
    public static final int OPEN_FILE_KING_PENALTY = 20;      // עונש על מלך בטור פתוח לחלוטין
    public static final int SEMI_OPEN_FILE_KING_PENALTY = 10;  // עונש על מלך בטור חצי פתוח (אין רגלי שלך)
    public static final int BISHOP_PAIR_BONUS_MG = 20; // זוג רצים במציע המשחק
    public static final int BISHOP_PAIR_BONUS_EG = 35; // זוג רצים חזק עוד יותר בסיום!
    public static final int KNIGHT_MOBILITY_MG = 4;
    public static final int KNIGHT_MOBILITY_EG = 4;
    public static final int BISHOP_MOBILITY_MG = 3;
    public static final int BISHOP_MOBILITY_EG = 4;
    public static final int ROOK_MOBILITY_MG = 2;
    public static final int ROOK_MOBILITY_EG = 4;
    public static final int QUEEN_MOBILITY_MG = 1;
    public static final int QUEEN_MOBILITY_EG = 2;

    // יתרון התור (Tempo)
    public static final int TEMPO_BONUS = 15;
    public static final int ROOK_OPEN_FILE_BONUS_MG = 15; // צריח בטור פתוח (ללא רגלים)
    public static final int ROOK_OPEN_FILE_BONUS_EG = 10;
    public static final int ROOK_SEMI_OPEN_FILE_BONUS_MG = 8; // צריח בטור חצי פתוח (אין רגלי שלך)
    public static final int ROOK_SEMI_OPEN_FILE_BONUS_EG = 8;

    public static final int ROOK_ON_7TH_BONUS_MG = 15; // צריח בשורה שביעית (שורת הרגלים של היריב)
    public static final int ROOK_ON_7TH_BONUS_EG = 30;
    public static final int UNCASTLED_KING_PENALTY = 10;       // עונש רך על מלך תקוע במרכז ללא הצרחה
    // בונוסים לפי דרגה יחסית (1 עד 6)
    public static final int[] PASSED_PAWN_BONUS_MG = {0, 5, 10, 20, 35, 60, 100, 0};
    public static final int[] PASSED_PAWN_BONUS_EG = {0, 10, 20, 40, 75, 130, 210, 0};
    public static final int[] WHITE_PASSED_BONUS_MG = new int[64];
    public static final int[] WHITE_PASSED_BONUS_EG = new int[64];
    public static final int[] BLACK_PASSED_BONUS_MG = new int[64];
    public static final int[] BLACK_PASSED_BONUS_EG = new int[64];
    public static final int CASTLING_RIGHTS_BONUS = 10;
    static {
        // --- 1. אתחול KING_SHIELD_MASKS (התיקון הקריטי!) ---
        for (int sq = 0; sq < 64; sq++) {
            int file = sq & 7;
            int rank = sq >> 3;

            // לבן: רגלים מגנים שורה אחת צפונה (rank - 1 -> sq - 8)
            if (rank > 0) {
                long mask = 0L;
                int north = sq - 8;
                mask |= (1L << north);
                if (file > 0) mask |= (1L << (north - 1));
                if (file < 7) mask |= (1L << (north + 1));
                KING_SHIELD_MASKS[0][sq] = mask;
            }

            // שחור: רגלים מגנים שורה אחת דרומה (rank + 1 -> sq + 8)
            if (rank < 7) {
                long mask = 0L;
                int south = sq + 8;
                mask |= (1L << south);
                if (file > 0) mask |= (1L << (south - 1));
                if (file < 7) mask |= (1L << (south + 1));
                KING_SHIELD_MASKS[1][sq] = mask;
            }

            // אתחול רגלים עוברים
            int relRankBlack = sq >> 3;
            int relRankWhite = 7 - relRankBlack;
            WHITE_PASSED_BONUS_MG[sq] = PASSED_PAWN_BONUS_MG[relRankWhite];
            WHITE_PASSED_BONUS_EG[sq] = PASSED_PAWN_BONUS_EG[relRankWhite];
            BLACK_PASSED_BONUS_MG[sq] = PASSED_PAWN_BONUS_MG[relRankBlack];
            BLACK_PASSED_BONUS_EG[sq] = PASSED_PAWN_BONUS_EG[relRankBlack];
        }
    }
    // מסיכות דרגות (0 = Rank 8, 7 = Rank 1)
    public static final long[] RANK_MASKS = new long[8];

    static {
        for (int r = 0; r < 8; r++) {
            RANK_MASKS[r] = 0xFFL << (r * 8);
        }
    }

    public static final long[] FILE_MASKS = new long[8];


    static {
        // אתחול מסיכות טורים (File A = 0 עד File H = 7)
        for (int f = 0; f < 8; f++) {
            FILE_MASKS[f] = 0x0101010101010101L << f;
        }
    }
    // --- פונקציות עזר Bitwise (A8 = 0) ---

    // צפון = לכיוון דרגה 8 (אינדקס קטן -> shift ימינה)
    public static long northFill(long b) {
        b |= (b >>> 8);
        b |= (b >>> 16);
        return b | (b >>> 32);
    }

    // דרום = לכיוון דרגה 1 (אינדקס גדל -> shift שמאלה)
    public static long southFill(long b) {
        b |= (b << 8);
        b |= (b << 16);
        return b | (b << 32);
    }

    public static long adjacentFiles(long b) {
        return ((b & 0xFEFEFEFEFEFEFEFEL) >>> 1) | ((b & 0x7F7F7F7F7F7F7F7FL) << 1);
    }

    // --- PAWNS (Middlegame) ---
    public static final int[] WHITE_PAWN_PST_MG = {
            0,   0,   0,   0,   0,   0,   0,   0, // Rank 8 (Promoted)
            50,  50,  50,  50,  50,  50,  50,  50, // Rank 7
            10,  10,  20,  30,  30,  20,  10,  10, // Rank 6
            5,   5,  10,  25,  25,  10,   5,   5, // Rank 5
            0,   0,   0,  20,  20,   0,   0,   0, // Rank 4
            5,  -5, -10,   0,   0, -10,  -5,   5, // Rank 3
            5,  10,  10, -20, -20,  10,  10,   5, // Rank 2
            0,   0,   0,   0,   0,   0,   0,   0  // Rank 1
    };

    public static final int[] BLACK_PAWN_PST_MG = {
            0,   0,   0,   0,   0,   0,   0,   0, // Rank 8
            5,  10,  10, -20, -20,  10,  10,   5, // Rank 7
            5,  -5, -10,   0,   0, -10,  -5,   5, // Rank 6
            0,   0,   0,  20,  20,   0,   0,   0, // Rank 5
            5,   5,  10,  25,  25,  10,   5,   5, // Rank 4
            10,  10,  20,  30,  30,  20,  10,  10, // Rank 3
            50,  50,  50,  50,  50,  50,  50,  50, // Rank 2
            0,   0,   0,   0,   0,   0,   0,   0  // Rank 1
    };

    // --- PAWNS (Endgame) ---
    public static final int[] WHITE_PAWN_PST_EG = {
            0,   0,   0,   0,   0,   0,   0,   0,
            80,  80,  80,  80,  80,  80,  80,  80,
            50,  50,  50,  50,  50,  50,  50,  50,
            30,  30,  30,  30,  30,  30,  30,  30,
            20,  20,  20,  20,  20,  20,  20,  20,
            10,  10,  10,  10,  10,  10,  10,  10,
            10,  10,  10,  10,  10,  10,  10,  10,
            0,   0,   0,   0,   0,   0,   0,   0
    };

    public static final int[] BLACK_PAWN_PST_EG = {
            0,   0,   0,   0,   0,   0,   0,   0,
            10,  10,  10,  10,  10,  10,  10,  10,
            10,  10,  10,  10,  10,  10,  10,  10,
            20,  20,  20,  20,  20,  20,  20,  20,
            30,  30,  30,  30,  30,  30,  30,  30,
            50,  50,  50,  50,  50,  50,  50,  50,
            80,  80,  80,  80,  80,  80,  80,  80,
            0,   0,   0,   0,   0,   0,   0,   0
    };


    // --- KNIGHTS (Middlegame) ---
    public static final int[] WHITE_KNIGHT_PST_MG = {
            -50, -40, -30, -30, -30, -30, -40, -50, // Rank 8
            -40, -20,   0,   0,   0,   0, -20, -40, // Rank 7
            -30,   0,  10,  15,  15,  10,   0, -30, // Rank 6
            -30,   5,  15,  20,  20,  15,   5, -30, // Rank 5
            -30,   0,  15,  20,  20,  15,   0, -30, // Rank 4
            -30,   5,  10,  15,  15,  10,   5, -30, // Rank 3
            -40, -20,   0,   5,   5,   0, -20, -40, // Rank 2
            -50, -40, -30, -30, -30, -30, -40, -50  // Rank 1
    };

    public static final int[] BLACK_KNIGHT_PST_MG = {
            -50, -40, -30, -30, -30, -30, -40, -50, // Rank 8
            -40, -20,   0,   5,   5,   0, -20, -40, // Rank 7
            -30,   5,  10,  15,  15,  10,   5, -30, // Rank 6
            -30,   0,  15,  20,  20,  15,   0, -30, // Rank 5
            -30,   5,  15,  20,  20,  15,   5, -30, // Rank 4
            -30,   0,  10,  15,  15,  10,   0, -30, // Rank 3
            -40, -20,   0,   0,   0,   0, -20, -40, // Rank 2
            -50, -40, -30, -30, -30, -30, -40, -50  // Rank 1
    };

    public static final int[] WHITE_KNIGHT_PST_EG = {
            -50, -40, -30, -30, -30, -30, -40, -50, // Rank 8
            -40, -20,   0,   5,   5,   0, -20, -40, // Rank 7
            -30,   5,  10,  15,  15,  10,   5, -30, // Rank 6
            -30,   0,  15,  20,  20,  15,   0, -30, // Rank 5
            -30,   5,  15,  20,  20,  15,   5, -30, // Rank 4
            -30,   0,  10,  15,  15,  10,   0, -30, // Rank 3
            -40, -20,   0,   0,   0,   0, -20, -40, // Rank 2
            -50, -40, -30, -30, -30, -30, -40, -50  // Rank 1
    };

    public static final int[] BLACK_KNIGHT_PST_EG = {
            -50, -40, -30, -30, -30, -30, -40, -50, // Rank 8
            -40, -20,   0,   0,   0,   0, -20, -40, // Rank 7
            -30,   0,  10,  15,  15,  10,   0, -30, // Rank 6
            -30,   5,  15,  20,  20,  15,   5, -30, // Rank 5
            -30,   0,  15,  20,  20,  15,   0, -30, // Rank 4
            -30,   5,  10,  15,  15,  10,   5, -30, // Rank 3
            -40, -20,   0,   5,   5,   0, -20, -40, // Rank 2
            -50, -40, -30, -30, -30, -30, -40, -50  // Rank 1
    };

    // --- BISHOPS ---
    // --- BISHOPS (Middlegame & Endgame) ---
    public static final int[] WHITE_BISHOP_PST_MG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 7
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 6
            -10,   5,   5,  10,  10,   5,   5, -10, // Rank 5
            -10,   0,  10,  10,  10,  10,   0, -10, // Rank 4
            -10,  10,  10,  10,  10,  10,  10, -10, // Rank 3
            -10,   5,   0,   0,   0,   0,   5, -10, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };

    public static final int[] BLACK_BISHOP_PST_MG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -10,   5,   0,   0,   0,   0,   5, -10, // Rank 7
            -10,  10,  10,  10,  10,  10,  10, -10, // Rank 6
            -10,   0,  10,  10,  10,  10,   0, -10, // Rank 5
            -10,   5,   5,  10,  10,   5,   5, -10, // Rank 4
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 3
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };

    public static final int[] WHITE_BISHOP_PST_EG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 7
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 6
            -10,   5,   5,  10,  10,   5,   5, -10, // Rank 5
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 4
            -10,   5,   5,   5,   5,   5,   5, -10, // Rank 3
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };

    public static final int[] BLACK_BISHOP_PST_EG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 7
            -10,   5,   5,   5,   5,   5,   5, -10, // Rank 6
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 5
            -10,   5,   5,  10,  10,   5,   5, -10, // Rank 4
            -10,   0,   5,  10,  10,   5,   0, -10, // Rank 3
            -10,   0,   0,   0,   0,   0,   0, -10, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };

    // --- ROOKS ---
    // --- ROOKS (Middlegame) ---
    public static final int[] WHITE_ROOK_PST_MG = {
            0,   0,   0,   0,   0,   0,   0,   0, // Rank 8
            5,  10,  10,  10,  10,  10,  10,   5, // Rank 7 (7th rank activity)
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 6
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 5
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 4
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 3
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 2
            0,   0,   0,   5,   5,   0,   0,   0  // Rank 1 (d1/e1 centralization)
    };

    public static final int[] BLACK_ROOK_PST_MG = {
            0,   0,   0,   5,   5,   0,   0,   0, // Rank 8 (d8/e8 centralization)
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 7
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 6
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 5
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 4
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 3
            5,  10,  10,  10,  10,  10,  10,   5, // Rank 2 (2nd rank activity)
            0,   0,   0,   0,   0,   0,   0,   0  // Rank 1
    };

    // --- ROOKS (Endgame) ---
    public static final int[] WHITE_ROOK_PST_EG = {
            0,   0,   0,   0,   0,   0,   0,   0, // Rank 8
            10,  15,  15,  15,  15,  15,  15,  10, // Rank 7 (High 7th rank pressure)
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 6
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 5
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 4
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 3
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 2
            0,   0,   5,  10,  10,   5,   0,   0  // Rank 1 (Active base)
    };

    public static final int[] BLACK_ROOK_PST_EG = {
            0,   0,   5,  10,  10,   5,   0,   0, // Rank 8 (Active base)
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 7
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 6
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 5
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 4
            -5,   0,   0,   0,   0,   0,   0,  -5, // Rank 3
            10,  15,  15,  15,  15,  15,  15,  10, // Rank 2 (High 2nd rank pressure)
            0,   0,   0,   0,   0,   0,   0,   0  // Rank 1
    };

    // --- QUEENS ---
    // --- QUEENS (Middlegame) ---
    public static final int[] WHITE_QUEEN_PST_MG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -10,  -5,  -5,  -5,  -5,  -5,  -5, -10, // Rank 7
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 6
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 5
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 4
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 3
            -10,  -5,   5,   5,   5,   5,  -5, -10, // Rank 2
            -20, -10,  -5,   5,   0,  -5, -10, -20  // Rank 1 (d1 home square bonus)
    };

    public static final int[] BLACK_QUEEN_PST_MG = {
            -20, -10,  -5,   5,   0,  -5, -10, -20, // Rank 8 (d8 home square bonus)
            -10,  -5,   5,   5,   5,   5,  -5, -10, // Rank 7
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 6
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 5
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 4
            -10,  -5,   0,   0,   0,   0,  -5, -10, // Rank 3
            -10,  -5,  -5,  -5,  -5,  -5,  -5, -10, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };

    // --- QUEENS (Endgame) ---
    public static final int[] WHITE_QUEEN_PST_EG = {
            -20, -10, -10,  -5,  -5, -10, -10, -20, // Rank 8
            -10,   0,   5,   5,   5,   5,   0, -10, // Rank 7
            -10,   5,  10,  10,  10,  10,   5, -10, // Rank 6
            -5,   5,  10,  15,  15,  10,   5,  -5, // Rank 5
            -5,   5,  10,  15,  15,  10,   5,  -5, // Rank 4
            -10,   5,  10,  10,  10,  10,   5, -10, // Rank 3
            -10,   0,   5,   5,   5,   5,   0, -10, // Rank 2
            -20, -10, -10,  -5,  -5, -10, -10, -20  // Rank 1
    };

    public static final int[] BLACK_QUEEN_PST_EG = {
            -20, -10, -10,  -5,  -5, -10, -10, -20, // Rank 8
            -10,   0,   5,   5,   5,   5,   0, -10, // Rank 7
            -10,   5,  10,  10,  10,  10,   5, -10, // Rank 6
            -5,   5,  10,  15,  15,  10,   5,  -5, // Rank 5
            -5,   5,  10,  15,  15,  10,   5,  -5, // Rank 4
            -10,   5,  10,  10,  10,  10,   5, -10, // Rank 3
            -10,   0,   5,   5,   5,   5,   0, -10, // Rank 2
            -20, -10, -10,  -5,  -5, -10, -10, -20  // Rank 1
    };

    // --- KINGS (MG) ---
    // --- KINGS (Middlegame) ---
    public static final int[] WHITE_KING_PST_MG = {
            -80, -70, -70, -70, -70, -70, -70, -80, // Rank 8
            -60, -60, -60, -60, -60, -60, -60, -60, // Rank 7
            -40, -50, -50, -60, -60, -50, -50, -40, // Rank 6
            -30, -40, -40, -50, -50, -40, -40, -30, // Rank 5
            -20, -30, -30, -40, -40, -30, -30, -20, // Rank 4
            -10, -20, -20, -20, -20, -20, -20, -10, // Rank 3
            20,  20,  -5,  -5,  -5,  -5,  20,  20, // Rank 2
            20,  30,  10, -10,   0, -10,  30,  20  // Rank 1 (d1/f1 step-out penalty)
    };

    public static final int[] BLACK_KING_PST_MG = {
            20,  30,  10, -10,   0, -10,  30,  20, // Rank 8 (d8/f8 step-out penalty)
            20,  20,  -5,  -5,  -5,  -5,  20,  20, // Rank 7
            -10, -20, -20, -20, -20, -20, -20, -10, // Rank 6
            -20, -30, -30, -40, -40, -30, -30, -20, // Rank 5
            -30, -40, -40, -50, -50, -40, -40, -30, // Rank 4
            -40, -50, -50, -60, -60, -50, -50, -40, // Rank 3
            -60, -60, -60, -60, -60, -60, -60, -60, // Rank 2
            -80, -70, -70, -70, -70, -70, -70, -80  // Rank 1
    };

    // --- KINGS (Endgame) ---
    public static final int[] WHITE_KING_PST_EG = {
            -20, -10, -10, -10, -10, -10, -10, -20, // Rank 8
            -5,   0,   5,   5,   5,   5,   0,  -5, // Rank 7
            -10,  -5,  20,  30,  30,  20,  -5, -10, // Rank 6
            -15, -10,  35,  45,  45,  35, -10, -15, // Rank 5
            -20, -15,  30,  40,  40,  30, -15, -20, // Rank 4
            -25, -20,  20,  25,  25,  20, -20, -25, // Rank 3
            -30, -25,   0,   0,   0,   0, -25, -30, // Rank 2
            -50, -30, -30, -30, -30, -30, -30, -50  // Rank 1
    };

    public static final int[] BLACK_KING_PST_EG = {
            -50, -30, -30, -30, -30, -30, -30, -50, // Rank 8
            -30, -25,   0,   0,   0,   0, -25, -30, // Rank 7
            -25, -20,  20,  25,  25,  20, -20, -25, // Rank 6
            -20, -15,  30,  40,  40,  30, -15, -20, // Rank 5
            -15, -10,  35,  45,  45,  35, -10, -15, // Rank 4
            -10,  -5,  20,  30,  30,  20,  -5, -10, // Rank 3
            -5,   0,   5,   5,   5,   5,   0,  -5, // Rank 2
            -20, -10, -10, -10, -10, -10, -10, -20  // Rank 1
    };
    private static final int[] EMPTY_PST = new int[64];

    public static final int[][] PST_MG = new int[][] {
            EMPTY_PST,            // 0 = NONE
            WHITE_PAWN_PST_MG,    // 1 = WP
            WHITE_KNIGHT_PST_MG,  // 2 = WN
            WHITE_BISHOP_PST_MG,  // 3 = WB
            WHITE_ROOK_PST_MG,    // 4 = WR
            WHITE_QUEEN_PST_MG,   // 5 = WQ
            WHITE_KING_PST_MG,    // 6 = WK
            EMPTY_PST,            // 7 = unused
            EMPTY_PST,            // 8 = unused
            BLACK_PAWN_PST_MG,    // 9 = BP
            BLACK_KNIGHT_PST_MG,  // 10 = BN
            BLACK_BISHOP_PST_MG,  // 11 = BB
            BLACK_ROOK_PST_MG,    // 12 = BR
            BLACK_QUEEN_PST_MG,   // 13 = BQ
            BLACK_KING_PST_MG     // 14 = BK
    };

    public static final int[][] PST_EG = new int[][] {
            EMPTY_PST,            // 0 = NONE
            WHITE_PAWN_PST_EG,    // 1 = WP
            WHITE_KNIGHT_PST_EG,  // 2 = WN
            WHITE_BISHOP_PST_EG,  // 3 = WB
            WHITE_ROOK_PST_EG,    // 4 = WR
            WHITE_QUEEN_PST_EG,   // 5 = WQ
            WHITE_KING_PST_EG,    // 6 = WK
            EMPTY_PST,            // 7 = unused
            EMPTY_PST,            // 8 = unused
            BLACK_PAWN_PST_EG,    // 9 = BP
            BLACK_KNIGHT_PST_EG,  // 10 = BN
            BLACK_BISHOP_PST_EG,  // 11 = BB
            BLACK_ROOK_PST_EG,    // 12 = BR
            BLACK_QUEEN_PST_EG,   // 13 = BQ
            BLACK_KING_PST_EG     // 14 = BK
    };
    public static final int[][] PIECE_PST_MG = new int[15][64];
    public static final int[][] PIECE_PST_EG = new int[15][64];

    static {
        for (int piece = 0; piece < 15; piece++) {
            int sign = piece >= 1 && piece <= 6 ? 1 : piece >= 9 ? -1 : 0;
            for (int sq = 0; sq < 64; sq++) {
                PIECE_PST_MG[piece][sq] = (PIECE_VALUES_MG[piece] + PST_MG[piece][sq]) * sign;
                PIECE_PST_EG[piece][sq] = (PIECE_VALUES_EG[piece] + PST_EG[piece][sq]) * sign;
            }
        }
    }

}