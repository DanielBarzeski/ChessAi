package Engine;

import App.GamePanel;

public class MoveGenerator {
    private static TranspositionTable tt = new TranspositionTable(20);
    private static final int[][] MOVES = new int[128][218];

    private static volatile boolean cancel = false;
    private static volatile boolean abort = false;
    private static volatile boolean operating = false;
    private static volatile int evaluationScore;

    public static void reset() {
        evaluationScore = 0;
        tt = new TranspositionTable(20);
    }

    public static boolean isOperating() {
        return operating;
    }

    public static boolean isCancelled() {
        return cancel;
    }

    public static void cancel() {
        cancel = true;
    }

    public static void abort() {
        abort = true;
    }

    public static int getBestMove(Position other) {
        cancel = false;
        abort = false;
        operating = true;
        Position position = new Position(other);
        long currentBoardHash = position.getZobristKey();
       // BookMove bookMove = OpeningBooks.getBestMove(currentBoardHash);
        int[] moves = position.generateMoves(MOVES[0]);
        int length = position.getLegalMovesAmount();
//        if (bookMove != null) {
//            for (int i = 0; i < length; i++) {
//                if (moves[i] == bookMove.getMove()) {
//                    System.out.println("playing an opening book...");
//                    operating = false;
//                    return bookMove.getMove();
//                }
//            }
//        }
        int preEvaluationScore = evaluationScore;
        int bestMove = 0;
        for (int depth = 1; depth < 12; depth++) {
            System.out.println("depth: " + depth);
            int currentDepthBestMove = findBestMove(position, depth, currentBoardHash, moves, length);
            if (cancel || abort) {
                break;
            }
            bestMove = currentDepthBestMove;
            if (GamePanel.timeCounter > 0 && depth > 6) {
                break;
            }
        }
        System.out.println("previous evaluation score: " + preEvaluationScore);
        System.out.println("evaluation score: " + evaluationScore);
        operating = false;
        GamePanel.timeCounter = 0;
        return bestMove;
    }

    public static int findBestMove(Position position,int depth,long currentBoardHash,int[] moves,int length) {
        tt.incrementAge();

        long ttEntry = tt.probe(currentBoardHash);
        int ttMove = (ttEntry != TranspositionTable.INVALID_ENTRY) ? TranspositionTable.extractBestMove(ttEntry) : 0;

        position.sortMoves(moves, length, ttMove);

        int alpha = -1000000;
        int beta = 1000000;
        int bestScore = -1000000;
        int bestMove = 0;

        for (int i = 0; i < length; i++) {
            if (abort) break;
            int move = moves[i];
            position.move(move);
            int currentValue = -negamax(position, depth - 1, 1, -beta, -alpha);
            position.undo(move);
            if (abort) break;
            if (currentValue > bestScore) {
                bestMove = move;
                bestScore = currentValue;
            }
            if (bestScore > alpha) {
                alpha = bestScore;
            }
            if (alpha >= beta) {
                break;
            }
        }
        if (!abort) {
            evaluationScore = bestScore;
        }
        return bestMove;
    }

    private static int negamax(Position position, int depth, int ply, int alpha, int beta) {
        if (abort || position.isSearchRepetition() || position.isFiftyMoveDraw() || position.hasInsufficientMaterial()) {
            return 0;
        }
        int originalAlpha = alpha;

        long zobristKey = position.getZobristKey();
        long ttEntry = tt.probe(zobristKey);
        int ttMove = 0;
        if (ttEntry != TranspositionTable.INVALID_ENTRY) {
            int storedDepth = TranspositionTable.extractDepth(ttEntry);
            ttMove = TranspositionTable.extractBestMove(ttEntry);
            if (storedDepth >= depth) {
                int storedFlag = TranspositionTable.extractFlag(ttEntry);
                int storedScore = TranspositionTable.extractScore(ttEntry);
                if (storedScore > 90000) storedScore -= ply;
                else if (storedScore < -90000) storedScore += ply;
                if (storedFlag == TranspositionTable.EXACT) return storedScore;
                if (storedFlag == TranspositionTable.BETA && storedScore >= beta) return storedScore;
                if (storedFlag == TranspositionTable.ALPHA && storedScore <= alpha) return storedScore;
            }
        }

        if (depth == 0) return quiescence(position, alpha, beta, ply);

        int[] moves = position.generateMoves(MOVES[ply]);
        int length = position.getLegalMovesAmount();

        if (length == 0) {
            if (position.isInCheck()) {
                return -100000 + ply;
            } else {
               return 0;
            }
        }
        // new:
        int staticEval = position.evaluate();

        // --- Futility Pruning ---
        boolean futilityPruning = false;
        // מרווח בטיחות: 150 נקודות (רגלי וחצי) בעומק 1, 300 נקודות בעומק 2
        int futilityMargin = 150 * depth;

        // מפעילים רק בעומקים 1-2, כשהמלך לא בשח, וכשאנחנו לא במצבי מט
        if (depth <= 2 && !position.isInCheck() && Math.abs(alpha) < 90000) {
            // אם ההערכה הסטטית + המרווח עדיין נמוכים מ-alpha - המצב חסר סיכוי
            if (staticEval + futilityMargin <= alpha) {
                futilityPruning = true;
            }
        }
        //

        position.sortMoves(moves, length, ttMove);

        // תיקון קריטי: אם Futility Pruning פעיל, מתחילים מ-staticEval ולא מ--1000000
        int bestScore = futilityPruning ? staticEval : -1000000;
        int bestMove = 0;

        for (int i = 0; i < length; i++) {
            if (abort) return 0;
            int move = moves[i];
            // לא מדלגים על הכאות, וגם לא מדלגים על שחים (move < 0)!
            if (futilityPruning && ((move >> 16) & 0xF) == 0 && move > 0 && ((move >> 20) & 0xF) == 0) {
                continue;
            }

            position.move(move);
            int value = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
            position.undo(move);
            if (value > bestScore) {
                bestMove = move;
                bestScore = value;
            }
            if (bestScore > alpha) {
                alpha = bestScore;
            }
            if (alpha >= beta) {
                break;
            }
        }
        if (!abort) {
            int flag;
            if (bestScore <= originalAlpha) flag = TranspositionTable.ALPHA;
            else if (bestScore >= beta) flag = TranspositionTable.BETA;
            else flag = TranspositionTable.EXACT;
            int ttScore = bestScore;
            if (ttScore > 90000) ttScore += ply;
            else if (ttScore < -90000) ttScore -= ply;
            tt.store(zobristKey, depth, ttScore, flag, bestMove);
        }
        return bestScore;
    }

    private static int quiescence(Position position, int alpha, int beta, int ply) {
        if (abort) return 0;
        if (ply > 126) {
            return position.evaluate();
        }

        boolean inCheck = position.isInCheck();

        // 1. Stand-Pat
        if (!inCheck) {
            int standPat = position.evaluate();

            if (standPat >= beta) {
                return beta;
            }
            if (standPat > alpha) {
                alpha = standPat;
            }

            // --- Delta Pruning ---
            // אם המצב שלנו + ערך המלכה (900) עדיין נמוך מ-alpha, אין שום הכאה שתוכל להציל אותנו
            if (standPat + 900 < alpha) {
                return alpha;
            }
        }

        int[] moves = position.generateMoves(MOVES[ply]);
        int length = position.getLegalMovesAmount();

        if (inCheck && length == 0) {
            return -100000 + ply;
        }

        // --- קריטי ביותר: מיון מהלכים בתוך QS! ---
        // ממיין את ההכאות כך שהכאת הכלים היקרים ביותר תיבדק ראשונה ותיתן Beta-Cutoff מיידי
        position.sortMoves(moves, length, 0);

        for (int i = 0; i < length; i++) {
            if (abort) return 0;
            int move = moves[i];
            int captured = (move >> 16) & 0xF;
            int promotion = (move >> 20) & 0xF;

            // ממשיכים לחקור את הענף גם אם המהלך הזה נותן שח ליריב!
            if (!inCheck && captured == 0 && promotion == 0) {
                continue;
            }

            position.move(move);
            int score = -quiescence(position, -beta, -alpha, ply + 1);
            position.undo(move);

            if (score >= beta) {
                return beta; // Cutoff!
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }
}
