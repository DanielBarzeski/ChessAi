package stockfish;

import Logic.Position;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockfishService {
    private static boolean osFound;
    private static Process process;
    private static BufferedReader reader;
    private static BufferedWriter writer;

    private static String getStockfishBinaryPath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "assets/stockfish/stockfish-11-win-modern.exe";
        } else if (os.contains("mac")) {
            return "assets/stockfish/stockfish-11-mac-modern";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "assets/stockfish/stockfish-11-linux-modern";
        }
        System.out.println("couldn't figure operating system: " + os);
        return null;
    }

    public static void startEngine() {
        String path = getStockfishBinaryPath();
        if (path != null) {
            try {
                process = new ProcessBuilder(path).start();
            } catch (IOException e) {
                System.out.println("couldn't start process: " + e.getMessage());
                return;
            }
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            osFound = true;
        }
    }

    public static EngineAnalysis getPositionAnalyzation(String fen, int depth) {
        if (!osFound) {
            return null;
        }
        if (sendingCommand("position fen " + fen) && sendingCommand("go depth " + depth)) {
            int latestEvaluation = 0;
            int mateIn = 0;
            boolean isMate = false;
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("info depth") && line.contains("score")) {
                        int cpIdx = line.indexOf(" cp ");
                        if (cpIdx != -1) {
                            latestEvaluation = parseValueAfterToken(line, cpIdx + 4);
                            isMate = false;
                        } else {
                            int mateIdx = line.indexOf(" mate ");
                            if (mateIdx != -1) {
                                mateIn = parseValueAfterToken(line, mateIdx + 6);
                                isMate = true;
                            }
                        }
                    }

                    if (line.startsWith("bestmove")) {
                        String[] parts = line.split(" ", 3);

                        if (parts.length < 2 || "(none)".equals(parts[1])) {
                            boolean checkmate = isMate && mateIn == 0;
                            boolean stalemate = !isMate;
                            return new EngineAnalysis(-1, -1, 0, latestEvaluation, mateIn, isMate, checkmate, stalemate);
                        }

                        String moveStr = parts[1];
                        if (moveStr.length() < 4) {
                            return null;
                        }

                        int fromSquare = getSquare(moveStr.substring(0, 2));
                        int toSquare = getSquare(moveStr.substring(2, 4));

                        int promotion = 0;
                        if (moveStr.length() > 4) {
                            promotion = switch (moveStr.charAt(4)) {
                                case 'q' -> 1;
                                case 'r' -> 2;
                                case 'b' -> 3;
                                case 'n' -> 4;
                                default -> promotion;
                            };
                        }

                        return new EngineAnalysis(fromSquare, toSquare, promotion, latestEvaluation, mateIn, isMate, false, false);
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error reading or parsing engine output: " + e.getMessage());
            }
        }
        return null;
    }

    private static int parseValueAfterToken(String line, int startIndex) {
        int endIndex = line.indexOf(' ', startIndex);
        if (endIndex == -1) {
            endIndex = line.length();
        }
        return Integer.parseInt(line.substring(startIndex, endIndex).trim());
    }

    public static int getSquare(String squareName) {
        int file = squareName.charAt(0) - 'a';
        int rank = '8' - squareName.charAt(1);
        return (rank * 8) + file;
    }

    public static void debugPerft(Position other, int depth) {
        if (!osFound) return;

        Position position = new Position(other);
        System.out.println("🔍 Building Stockfish trusted tree for FEN up to depth " + depth + "...");
        Map<String, Object> moveTree = buildStockfishTreeRecursive(other.generateFEN(), "", 0, depth);

        List<String> history = new ArrayList<>();
        System.out.println("🔍 Searching for discrepancies in your engine...");
        boolean bugFound = findBugRecursive(position, moveTree, history, depth);

        if (bugFound) {
            System.out.println("\n========================================================================");
            System.out.println("🚨 REPRODUCTION STEPS (Follow these exact move/undo steps on your board):");
            System.out.println("========================================================================");
            for (String step : history) {
                System.out.println(step);
            }
            System.out.println("========================================================================\n");
        } else {
            System.out.println("✅ SUCCESS! Your move generator matches Stockfish perfectly up to depth " + depth);
        }
    }

    // חיפוש רקורסיבי המשווה בין עץ המהלכים של המנוע שלך לעץ האמת של סטוקפיש
    private static boolean findBugRecursive(Position pos, Map<String, Object> currentTree, List<String> history, int depth) {
        if (depth == 0) return false;

        int[] moves = pos.getAllClearedMoves();
        int length = pos.getMovesLength();

        // מיפוי מהלכי המנוע שלך מ-UCI למהלך המקודד (int)
        Map<String, Integer> localMovesMap = new HashMap<>();
        for (int i = 0; i < length; i++) {
            localMovesMap.put(moveToUciString(moves[i]), moves[i]);
        }

        // 1. זיהוי מהלך חסר (סטוקפיש מצא מהלך חוקי שהמנוע שלך לא ייצר)
        for (String sfMove : currentTree.keySet()) {
            if (!localMovesMap.containsKey(sfMove)) {
                history.add("--- הפסק כאן! המנוע שלך פספס מהלך ---");
                history.add("❌ MISSING MOVE: Stockfish has legal move [" + sfMove + "], but your engine missed it!");
                return true;
            }
        }

        // 2. זיהוי מהלך לא חוקי (המנוע שלך ייצר מהלך שאינו קיים בסטוקפיש)
        for (String localMove : localMovesMap.keySet()) {
            if (!currentTree.containsKey(localMove)) {
                history.add("--- הפסק כאן! המנוע שלך ייצר מהלך לא חוקי ---");
                history.add("❌ ILLEGAL MOVE: Your engine generated [" + localMove + "], but it is illegal!");
                return true;
            }
        }

        // 3. במידה ואין באג מיידי ברמה הזו, נמשיך לבדוק לעומק את המהלכים הבאים
        if (depth > 1) {
            for (Map.Entry<String, Integer> entry : localMovesMap.entrySet()) {
                String uci = entry.getKey();
                int move = entry.getValue();

                history.add("move: " + uci);
                pos.move(move);

                @SuppressWarnings("unchecked")
                Map<String, Object> subTree = (Map<String, Object>) currentTree.get(uci);
                if (subTree != null) {
                    boolean bugFound = findBugRecursive(pos, subTree, history, depth - 1);
                    if (bugFound) {
                        return true; // עוצרים הכל ומבעבעים למעלה ללא קריאת undo נוספת
                    }
                }

                pos.undoMove(move);
                history.add("undo move: " + uci);
            }
        }

        return false;
    }

    // בניית עץ המהלכים של סטוקפיש כפי שביקשת
    public static Map<String, Object> getStockfishMoveTree(String rootFen, int maxDepth) {
        if (!osFound) return new HashMap<>();
        return buildStockfishTreeRecursive(rootFen, "", 0, maxDepth);
    }

    private static Map<String, Object> buildStockfishTreeRecursive(String rootFen, String movePath, int currentDepth, int maxDepth) {
        Map<String, Object> tree = new HashMap<>();
        if (currentDepth >= maxDepth) {
            return tree;
        }

        List<String> legalMoves = getStockfishLegalMovesForPath(rootFen, movePath);
        for (String uci : legalMoves) {
            String nextPath = movePath.isEmpty() ? uci : movePath + " " + uci;
            Map<String, Object> subTree = buildStockfishTreeRecursive(rootFen, nextPath, currentDepth + 1, maxDepth);
            tree.put(uci, subTree);
        }
        return tree;
    }

    private static List<String> getStockfishLegalMovesForPath(String rootFen, String movePath) {
        String command = "position fen " + rootFen;
        if (movePath != null && !movePath.trim().isEmpty()) {
            command += " moves " + movePath;
        }

        List<String> moves = new ArrayList<>();
        if (sendingCommand(command) && sendingCommand("go perft 1")) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("Nodes searched") || line.startsWith("Nodes:")) {
                        break;
                    }

                    if (line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            moves.add(parts[0].trim());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading moves from Stockfish: " + e.getMessage());
            }
        }
        return moves;
    }

    // הפיכת מהלך מקודד (int) למחרוזת UCI מול קידוד ה-Bits שלך
    private static String moveToUciString(int move) {
        int from = move & 0x3F;                // 6 bits
        int to = (move >> 6) & 0x3F;           // 6 bits
        int promotion = (move >> 20) & 0x0F;   // 4 bits (20-23)

        String promoStr = switch (promotion) {
            case 2, 10 -> "n"; // WN / BN
            case 3, 11 -> "b"; // WB / BB
            case 4, 12 -> "r"; // WR / BR
            case 5, 13 -> "q"; // WQ / BQ
            default -> "";
        };

        return getSquareName(from) + getSquareName(to) + promoStr;
    }

    private static String getSquareName(int square) {
        int rank = square / 8;
        int file = square % 8;
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('8' - rank);
        return "" + fileChar + rankChar;
    }


    private static boolean sendingCommand(String cmd) {
        if (osFound && process != null) {
            try {
                writer.write(cmd + "\n");
                writer.flush();
                return true;
            } catch (IOException e) {
                System.out.println("couldn't send command: " + cmd + " " + e.getMessage());
                return false;
            }
        }
        return false;
    }
}
