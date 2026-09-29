package Engine;

public class MoveGenerator {
    private static TranspositionTable tt = new TranspositionTable(20);
    private static final int[][] MOVES = new int[128][218];
    private static final int[][] SCORES = new int[128][218];

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

        int[] moves = position.generateMoves(MOVES[0]);
        int length = position.getLegalMovesAmount();

        long currentBoardHash = other.getZobristKey();
        BookMove bookMove = OpeningBooks.getBestMove(currentBoardHash);
        if (bookMove != null && position.getPly() < 19) {
            for (int i = 0; i < length; i++) {
                if (moves[i] == bookMove.getMove()) {
                    System.out.println("playing an opening book...");
                    int preEvaluationScore = evaluationScore;
                    evaluationScore = position.evaluate();
                    System.out.println("previous evaluation score: " + preEvaluationScore);
                    System.out.println("evaluation score: " + evaluationScore);
                    operating = false;
                    return bookMove.getMove();
                }
            }
        }
        int preEvaluationScore = evaluationScore;
        int bestMove = 0;
        long startTime = System.currentTimeMillis();
        for (int depth = 1; depth < 12; depth++) {
            System.out.println("depth: " + depth);
            int currentDepthBestMove = findBestMove(position, depth, currentBoardHash, moves, length);
            if (cancel || abort) {
                break;
            }
            bestMove = currentDepthBestMove;
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime >= 1000) {
                break;
            }
        }
        System.out.println("previous evaluation score: " + preEvaluationScore);
        System.out.println("evaluation score: " + evaluationScore);
        operating = false;
        return bestMove;
    }

    public static int findBestMove(Position position, int depth, long currentBoardHash, int[] moves, int length) {
        tt.incrementAge();

        long ttEntry = tt.probe(currentBoardHash);
        int ttMove = (ttEntry != TranspositionTable.INVALID_ENTRY) ? TranspositionTable.extractBestMove(ttEntry) : 0;

        int[] scores = SCORES[0];
        for (int i = 0; i < length; i++) {
            scores[i] = (moves[i] == ttMove) ? 1000000 : position.scoreMove(moves[i]);
        }

        int alpha = -1000000;
        int beta = 1000000;
        int bestScore = -1000000;
        int bestMove = 0;

        for (int i = 0; i < length; i++) {
            if (abort) break;

            int bestMoveScore = -1;
            int bestMoveIndex = i;
            for (int j = i; j < length; j++) {
                if (scores[j] > bestMoveScore) {
                    bestMoveScore = scores[j];
                    bestMoveIndex = j;
                }
            }

            int tempMove = moves[i];
            moves[i] = moves[bestMoveIndex];
            moves[bestMoveIndex] = tempMove;

            int tempScore = scores[i];
            scores[i] = scores[bestMoveIndex];
            scores[bestMoveIndex] = tempScore;

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
            tt.store(currentBoardHash, depth, bestScore, TranspositionTable.EXACT, bestMove);
        }
        return bestMove;
    }

    private static int negamax(Position position, int depth, int ply, int alpha, int beta) {
        if (abort || position.isThreefoldRepetition() || position.isSearchRepetition(ply) || position.isFiftyMoveDraw() || position.hasInsufficientMaterial()) {
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
                if (storedFlag == TranspositionTable.LOWER_BOUND && storedScore >= beta) return storedScore;
                if (storedFlag == TranspositionTable.UPPER_BOUND && storedScore <= alpha) return storedScore;
            }
        }

        if (depth == 0) return quiescence(position, alpha, beta, ply);

        int tacticalCount;
        int checksCount = 0;
        int totalMoves = 0;

        tacticalCount = position.generateTacticalMoves(MOVES[ply]);
        boolean inCheck = position.isInCheck();
        if (inCheck) {
            checksCount = position.generateQuietChecks(MOVES[ply], tacticalCount);
            totalMoves = position.generateQuietMoves(MOVES[ply], checksCount);

            if (totalMoves == 0) {
                return -100000 + ply;
            }
        }

        int bestScore = -1000000;
        int bestMove = 0;
        int[] scores = SCORES[ply];

        for (int i = 0; i < tacticalCount; i++) {
            scores[i] = (MOVES[ply][i] == ttMove) ? 1000000 : position.scoreMove(MOVES[ply][i]);
        }

        for (int i = 0; i < tacticalCount; i++) {
            if (abort) return 0;

            int bestMoveScore = -1;
            int bestMoveIndex = i;
            for (int j = i; j < tacticalCount; j++) {
                if (scores[j] > bestMoveScore) {
                    bestMoveScore = scores[j];
                    bestMoveIndex = j;
                }
            }

            int move = MOVES[ply][bestMoveIndex];
            MOVES[ply][bestMoveIndex] = MOVES[ply][i];
            MOVES[ply][i] = move;
            scores[bestMoveIndex] = scores[i];
            scores[i] = bestMoveScore;

            position.move(move);
            int extension = (move < 0 && ply < 60) ? 1 : 0;
            int value = -negamax(position, depth - 1 + extension, ply + 1, -beta, -alpha);
            position.undo(move);

            if (value > bestScore) { bestScore = value; bestMove = move; }
            if (bestScore > alpha) alpha = bestScore;
            if (alpha >= beta) {
                storeTT(zobristKey, depth, originalAlpha, beta, bestScore, bestMove, ply);
                return bestScore;
            }
        }

        if (!inCheck) {
            checksCount = position.generateQuietChecks(MOVES[ply], tacticalCount);
        }

        for (int i = tacticalCount; i < checksCount; i++) {
            scores[i] = (MOVES[ply][i] == ttMove) ? 1000000 : position.scoreMove(MOVES[ply][i]);
        }

        for (int i = tacticalCount; i < checksCount; i++) {
            if (abort) return 0;

            int bestMoveScore = -1;
            int bestMoveIndex = i;
            for (int j = i; j < checksCount; j++) {
                if (scores[j] > bestMoveScore) {
                    bestMoveScore = scores[j];
                    bestMoveIndex = j;
                }
            }

            int move = MOVES[ply][bestMoveIndex];
            MOVES[ply][bestMoveIndex] = MOVES[ply][i];
            MOVES[ply][i] = move;
            scores[bestMoveIndex] = scores[i];
            scores[i] = bestMoveScore;

            position.move(move);
            int extension = (move < 0 && ply < 60) ? 1 : 0;
            int value = -negamax(position, depth - 1 + extension, ply + 1, -beta, -alpha);
            position.undo(move);

            if (value > bestScore) { bestScore = value; bestMove = move; }
            if (bestScore > alpha) alpha = bestScore;
            if (alpha >= beta) {
                storeTT(zobristKey, depth, originalAlpha, beta, bestScore, bestMove, ply);
                return bestScore;
            }
        }

        boolean futilityPruning = false;

        if (!inCheck) {
            totalMoves = position.generateQuietMoves(MOVES[ply], checksCount);

            int staticEval = position.evaluate();
            int futilityMargin = 150 * depth;

            if (depth <= 2 && Math.abs(alpha) < 90000) {
                if (staticEval + futilityMargin <= alpha) {
                    futilityPruning = true;
                    if (bestScore < staticEval) bestScore = staticEval;
                }
            }
        }

        for (int i = checksCount; i < totalMoves; i++) {
            scores[i] = (MOVES[ply][i] == ttMove) ? 1000000 : 0;
        }

        for (int i = checksCount; i < totalMoves; i++) {
            if (abort) return 0;

            int bestMoveScore = -1;
            int bestMoveIndex = i;
            for (int j = i; j < totalMoves; j++) {
                if (scores[j] > bestMoveScore) {
                    bestMoveScore = scores[j];
                    bestMoveIndex = j;
                }
            }

            int move = MOVES[ply][bestMoveIndex];
            MOVES[ply][bestMoveIndex] = MOVES[ply][i];
            MOVES[ply][i] = move;
            scores[bestMoveIndex] = scores[i];
            scores[i] = bestMoveScore;

            if (futilityPruning && move > 0) continue;

            position.move(move);
            int value = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
            position.undo(move);

            if (value > bestScore) { bestScore = value; bestMove = move; }
            if (bestScore > alpha) alpha = bestScore;
            if (alpha >= beta) break;
        }

        if (!inCheck && totalMoves == 0) {
            return 0;
        }

        if (!abort) {
            storeTT(zobristKey, depth, originalAlpha, beta, bestScore, bestMove, ply);
        }
        return bestScore;
    }

    private static int quiescence(Position position, int alpha, int beta, int ply) {
        if (abort) return 0;
        if (ply > 126) return position.evaluate();

        int totalMoves = position.generateTacticalMoves(MOVES[ply]);

        boolean inCheck = position.isInCheck();

        if (!inCheck) {
            int standPat = position.evaluate();
            if (standPat >= beta) return beta;
            if (standPat > alpha) alpha = standPat;
            if (standPat + 900 < alpha) return alpha;
        }

        if (inCheck) {
            totalMoves = position.generateQuietChecks(MOVES[ply], totalMoves);
            totalMoves = position.generateQuietMoves(MOVES[ply], totalMoves);
        }

        if (inCheck && totalMoves == 0) {
            return -100000 + ply;
        }

        int[] scores = SCORES[ply];
        for (int i = 0; i < totalMoves; i++) {
            scores[i] = position.scoreMove(MOVES[ply][i]);
        }

        for (int i = 0; i < totalMoves; i++) {
            if (abort) return 0;

            int bestMoveScore = -1;
            int bestMoveIndex = i;
            for (int j = i; j < totalMoves; j++) {
                if (scores[j] > bestMoveScore) {
                    bestMoveScore = scores[j];
                    bestMoveIndex = j;
                }
            }

            int move = MOVES[ply][bestMoveIndex];
            MOVES[ply][bestMoveIndex] = MOVES[ply][i];
            MOVES[ply][i] = move;
            scores[bestMoveIndex] = scores[i];
            scores[i] = bestMoveScore;

            int captured = (move >> 16) & 0xF;
            int promotion = (move >> 20) & 0xF;
            if (!inCheck && captured == 0 && promotion == 0) {
                continue;
            }

            position.move(move);
            int score = -quiescence(position, -beta, -alpha, ply + 1);
            position.undo(move);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private static void storeTT(long zobristKey, int depth, int originalAlpha, int beta, int bestScore, int bestMove, int ply) {
        int flag;
        if (bestScore <= originalAlpha) flag = TranspositionTable.UPPER_BOUND;
        else if (bestScore >= beta) flag = TranspositionTable.LOWER_BOUND;
        else flag = TranspositionTable.EXACT;

        int ttScore = bestScore;
        if (ttScore > 90000) ttScore += ply;
        else if (ttScore < -90000) ttScore -= ply;
        tt.store(zobristKey, depth, ttScore, flag, bestMove);
    }
}