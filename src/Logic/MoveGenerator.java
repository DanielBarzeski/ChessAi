package Logic;

import App.GamePanel;
import Ziobrist.TranspositionTable;

public class MoveGenerator {
    private static volatile boolean abort = false, cancel = false, operating;
    private static int modifiedDepth;

    // אתחול ה-TT עם 20 ביטים (כמיליון כניסות לכל Tier, סה"כ לוקח סביב ה-32MB זיכרון)
    public static final TranspositionTable tt = new TranspositionTable(20);

    public static void cancel() {
        cancel = true;
        GamePanel.timeCounter = 0;
    }

    public static void smallAbort() {
        modifiedDepth = 7;
        abort = true;
    }

    public static void abort() {
        modifiedDepth = 6;
        abort = true;
    }

    public static void extremeAbort() {
        modifiedDepth = 2;
    }

    public static boolean isOperating() {
        return operating;
    }

    public static boolean isNotCancelled() {
        return !cancel;
    }

    public static int findBestMove(Position other, int depth) {
        cancel = false;
        abort = false;
        operating = true;

        tt.incrementAge();
        long currentBoardHash = other.getZobristKey();

        Position position = new Position(other);

        // חישוב מהלכים פעם אחת בלבד מחוץ ללולאת העומק (חיסכון עצום ב-GC וחישובים)
        int[] moves = position.getAllClearedMoves();
        int length = position.getMovesLength();

        int bestMove = 0;

        // [שימוש במימוש ה-TT האמיתי שלך]
        long ttData = tt.probe(currentBoardHash);
        if (ttData != TranspositionTable.INVALID_ENTRY) {
            // מחלצים את המהלך הכי טוב כדי לזרוע אותו לסידור המסעים
            bestMove = TranspositionTable.extractBestMove(ttData);
            int storedDepth = TranspositionTable.extractDepth(ttData);
            int storedFlag = TranspositionTable.extractFlag(ttData);

            // אם מצאנו תוצאה מדויקת (EXACT) בעומק הנדרש או עמוק יותר - נחזיר את המהלך מיד
            if (storedDepth >= depth && storedFlag == TranspositionTable.EXACT) {
                operating = false;
                return bestMove;
            }
        }

        BookMove bookMove = OpeningBooks.getBestMove(currentBoardHash);
        if (bookMove != null && position.getPly() <= 12) {
            operating = false;
            System.out.println("playing an opening book...");
            return bookMove.getMove();
        }

        // --- Iterative Deepening ---
        for (int currentDepth = 1; currentDepth <= depth; currentDepth++) {
            if (cancel) break;

            int bestValue = -1000000;
            int alpha = -1000000;
            int beta = 1000000;

            // סידור המסעים משתמש ב-bestMove של האיטרציה הקודמת (או זה ששלפנו מה-TT)
            position.sortMoves(moves, length, bestMove);

            int currentBestMove = 0;

            for (int i = 0; i < length; i++) {
                if (cancel) break;

                int searchDepth = currentDepth - 1;

                // [מנגנון ה-Soft Abort בזמן אמת]
                // אם הטיימר החליט שנגמר הזמן, הוא מקטין מיידית את עומק החיפוש עבור שאר המהלכים
                if (abort) {
                    depth = modifiedDepth;
                    searchDepth = Math.min(searchDepth, modifiedDepth - 1);
                }

                position.move(moves[i]);
                int value = -negamax(position, searchDepth, -beta, -alpha);
                position.undoMove(moves[i]);

                if (value > bestValue) {
                    bestValue = value;
                    currentBestMove = moves[i];
                }

                // אלפא-בטא פרונינג בשורש (Beta Cutoff)
                if (value >= beta) {
                    bestValue = beta;
                    currentBestMove = moves[i];
                    break;
                }

                if (value > alpha) {
                    alpha = value;
                }
            }

            if (!cancel) {
                bestMove = currentBestMove;
                tt.store(currentBoardHash, currentDepth, bestValue, TranspositionTable.EXACT, bestMove);

                // [בדיקת המט המדויקת שלך]
                // אם מצאנו מט כפוי לטובתנו (ערך מעל 3000), אין טעם לחפש עמוק יותר
                if (bestValue > 3000) {
                    break;
                }
            }
        }

        operating = false;
        GamePanel.timeCounter = 0;
        return bestMove;
    }

    public static int negamax(Position position, int depth, int alpha, int beta) {
        // --- תיקון: בדיקת חזרה משולשת (תיקו) ---
        if (position.getDepthPly() > 0 && position.isThreefoldRepetition()) {
            return 0;
        }

        int originalAlpha = alpha;
        long zobristKey = position.getZobristKey();
        long ttData = tt.probe(zobristKey);
        int ttMove = 0;

        if (ttData != TranspositionTable.INVALID_ENTRY) {
            int storedDepth = TranspositionTable.extractDepth(ttData);
            int storedScore = TranspositionTable.extractScore(ttData);
            int storedFlag = TranspositionTable.extractFlag(ttData);
            ttMove = TranspositionTable.extractBestMove(ttData);

            // --- תיקון: התאמת ניקוד מט מה-TT ל-Ply הנוכחי ---
            if (storedScore > 3000) storedScore -= position.getDepthPly();
            else if (storedScore < -3000) storedScore += position.getDepthPly();

            if (storedDepth >= depth) {
                if (storedFlag == TranspositionTable.EXACT) return storedScore;
                if (storedFlag == TranspositionTable.ALPHA && storedScore <= alpha) return alpha;
                if (storedFlag == TranspositionTable.BETA && storedScore >= beta) return beta;
            }
        }

        int[] moves = position.getAllMoves();
        int length = position.getMovesLength();

        if (length == 0) {
            if (position.isInCheck()) return -4000 + position.getDepthPly();
            return 0;
        }

        if (depth == 0 || cancel) {
            return quiescenceSearch(position, alpha, beta);
        }

        position.sortMoves(moves, length, ttMove);

        int bestScore = -1000000;
        int bestMoveFound = 0;

        for (int i = 0; i < length; i++) {
            position.move(moves[i]);
            int value = -negamax(position, depth - 1, -beta, -alpha);
            position.undoMove(moves[i]);

            if (value > bestScore) {
                bestScore = value;
                bestMoveFound = moves[i];

                if (value > alpha) {
                    alpha = value;
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
        }

        int flag;
        if (bestScore <= originalAlpha) flag = TranspositionTable.ALPHA;
        else if (bestScore >= beta) flag = TranspositionTable.BETA;
        else flag = TranspositionTable.EXACT;

        // --- תיקון: הפיכת ניקוד המט לעצמאי מ-Ply לפני השמירה ---
        int scoreToStore = bestScore;
        if (scoreToStore > 3000) scoreToStore += position.getDepthPly();
        else if (scoreToStore < -3000) scoreToStore -= position.getDepthPly();

        tt.store(zobristKey, depth, scoreToStore, flag, bestMoveFound);

        return bestScore;
    }

    public static int quiescenceSearch(Position position, int alpha, int beta) {
        int standPat = position.evaluate();
        if (standPat >= beta) return beta;
        if (alpha < standPat) alpha = standPat;

        int[] moves = position.getAllMoves();
        int length = position.getMovesLength();

        // 3. שיפור: ננסה לקרוא מה-TT כדי לקבל רמז למהלך הכי טוב בעמדה הזו
        long zobristKey = position.getZobristKey();
        long ttData = tt.probe(zobristKey);
        int ttMove = 0; // ברירת מחדל: אין מהלך מועדף

        if (ttData != TranspositionTable.INVALID_ENTRY) {
            ttMove = TranspositionTable.extractBestMove(ttData);
        }

        // 4. נמיין את המהלכים כאשר ה-ttMove (אם נמצא) יקבל עדיפות עליונה
        position.sortMoves(moves, length, ttMove);

        // 5. לולאת חיפוש האכילות
        for (int i = 0; i < length; i++) {
            // *** הקסם כאן: בחיפוש שקט אנחנו מתעלמים ממהלכים רגילים! ***
            // תצטרך לוודא שיש לך מתודה שמזהה אם מהלך הוא אכילה
            int move = moves[i];

            if (((move >>> 16) & 0xF) == 0) {
                continue;
            }

            position.move(move);
            int score = -quiescenceSearch(position, -beta, -alpha);
            position.undoMove(move);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }

        return alpha;
    }

}
