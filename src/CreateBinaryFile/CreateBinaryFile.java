package CreateBinaryFile;

import Logic.OpeningBooks;
import Logic.Position;
import LookUpTables.BishopMoves;
import LookUpTables.RookMoves;
import Ziobrist.ZobristKeys;
import stockfish.StockfishService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class CreateBinaryFile {
    public static void main(String[] args) {
        RookMoves.initiate();
        BishopMoves.initiate();
        ZobristKeys.load();
        StockfishService.startEngine();

        initiate();
        // readBinaryFile(path);
       // Main.main(args);
        OpeningBooks.loadBook();
    }

    public static void initiate(){
        String path = "assets/openings/games.bin";
        emptyBinaryFile(path);
        writeToBinaryFile("message.txt", path);
    }

    public static void writeToBinaryFile(String textFilePath, String binaryFilePath) {
        // יצירת תיקיית היעד במידה והיא לא קיימת
        try {
            Files.createDirectories(Paths.get("assets/openings"));
        } catch (IOException ignored) {}

        // שימוש ב-BufferedReader לקריאת טקסט שורה-שורה, ו-DataOutputStream לכתיבה בינארית
        try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFilePath)))) {

            Position position = new Position();
            ArrayList<Integer> movesHistory = new ArrayList<>();
            String line;
            int gameCount = 0;

            // לולאה ראשית: קוראת משחק שלם בכל שורה (מתוך 7,000 השורות)
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // אתחול הלוח וההיסטוריה מחדש עבור המשחק הנוכחי
                movesHistory.clear();

                // פיצול השורה לפי רווחים כדי לקבל את רשימת המהלכים (d4, Nf6, c4...)
                String[] sanMoves = line.split("\\s+");

                for (String sanMove : sanMoves) {
                    // בדיקה האם הגענו לתוצאת סיום המשחק (כמו 1-0, 0-1, 1/2-1/2 או סימן חלוקה)
                    if (sanMove.equals("1-0") || sanMove.equals("0-1") || sanMove.equals("1/2-1/2") || sanMove.contains("/")) {
                        break;
                    }

                    int[] legalMoves = position.getAllClearedMoves();
                    int matchedMove = 0;
                    boolean foundMatch = false;

                    // הלולאה שלך: סריקת המהלכים החוקיים כדי למצוא מי מהם מתאים לטקסט ה-SAN
                    for (int legalMove : legalMoves) {
                        if (matchMoveWithSAN(legalMove, sanMove)) {
                            matchedMove = legalMove;
                            foundMatch = true;
                            break; // מצאנו התאמה, אפשר לצאת מהלולאה הפנימית
                        }
                    }

                    if (foundMatch) {
                        // הפקת המפתח לאחר ביצוע המהלך (כפי שביקשת)
                        long zobristKey = position.getZobristKey();

                        // ביצוע המהלך על הלוח ושמירתו בהיסטוריה לצורך ה-Undo בסוף
                        position.move(matchedMove);
                        movesHistory.add(matchedMove);



                        // כתיבה מסודרת של 16 בייטים לקובץ הבינארי
                        dos.writeLong(zobristKey);  // 8 bytes
                        dos.writeInt(matchedMove);  // 4 bytes (המהלך האמיתי ב-32 ביטים!)
                        dos.writeInt(1);            // 4 bytes (משקל ברירת מחדל)
                    } else {
                       // System.err.println("שגיאה: לא נמצא מהלך חוקי שמתאים לטקסט: " + sanMove + " במשחק מספר " + (gameCount + 1));
                        break; // אם מהלך אחד נכשל, אי אפשר להמשיך לסמלץ את המשחק הנוכחי
                    }
                }

                // בסיום המשחק: החזרת הלוח למצב ההתחלתי צעד אחר צעד (Undo)
                for (int i = movesHistory.size() - 1; i >= 0; i--) {
                    position.undoMove(movesHistory.get(i));
                }

                gameCount++;
            }

         //   System.out.println("ספר הפתיחות נוצר בהצלחה! סומלצו " + gameCount + " משחקים.");

        } catch (IOException e) {
            System.err.println("שגיאה בתהליך הכתיבה: " + e.getMessage());
        }
    }

    // פונקציית העזר החכמה שמשווה בין מהלך בינארי חוקי לבין הטקסט מהקובץ
    private static boolean matchMoveWithSAN(int legalMove, String san) {
        // ניקוי סימני שח (+) או מט (#) מהטקסט
        san = san.replace("+", "").replace("#", "");

        // פירוק המהלך החוקי לשדות לפי ה-Masks וה-Shifts שלך
        int from = legalMove & 0x3F;
        int to = (legalMove >> 6) & 0x3F;
        int movingPiece = (legalMove >> 12) & 0xF;
        int promotion = (legalMove >> 20) & 0xF;
        int castling = (legalMove >> 24) & 0x3;

        // 1. בדיקת הצרחות
        if (san.equals("O-O")) return castling == 2;     // הצרחה קטנה (Kingside)
        if (san.equals("O-O-O")) return castling == 1;   // הצרחה גדולה (Queenside)

        // 2. בדיקת משבצת היעד (To)
        String toSquareName = convertIndexToSquareName(to);
        String sanTargetSquare;
        if (san.contains("=")) {
            // אם יש הכתרה (b8=Q), משבצת היעד היא שתי האותיות שלפני ה- '='
            sanTargetSquare = san.substring(san.indexOf("=") - 2, san.indexOf("="));
        } else {
            // בדרך כלל משבצת היעד היא שתי האותיות האחרונות (d4, Nf6)
            sanTargetSquare = san.substring(san.length() - 2);
        }
        if (!toSquareName.equals(sanTargetSquare)) return false;

        // 3. בדיקת סוג הכלי שזז
        char sanPieceChar = 'P'; // ברירת מחדל: רגלי (ב-SAN לרגלי אין אות גדולה)
        if (Character.isUpperCase(san.charAt(0))) {
            sanPieceChar = san.charAt(0); // למשל N, B, R, Q, K
        }
        if (!matchPieceType(movingPiece, sanPieceChar)) return false;

        // 4. בדיקת הכתרה (Promotion)
        if (san.contains("=")) {
            char promChar = san.charAt(san.indexOf("=") + 1);
            if (!matchPieceType(promotion, promChar)) return false;
        } else {
            if (promotion != 0) return false;
        }

        // 5. טיפול בכפילויות מהלכים (Disambiguation - למשל Nbd2 או R1e4)
        String remainder = san;
        if (Character.isUpperCase(remainder.charAt(0))) remainder = remainder.substring(1);
        if (remainder.contains("=")) remainder = remainder.substring(0, remainder.indexOf("=") - 2);
        else remainder = remainder.substring(0, remainder.length() - 2);
        remainder = remainder.replace("x", ""); // הסרת סימן הכאה

        if (!remainder.isEmpty()) {
            String fromSquareName = convertIndexToSquareName(from);
            if (remainder.length() == 1) {
                char c = remainder.charAt(0);
                if (c >= 'a' && c <= 'h') { // כפילות טור (Nbd2 -> הטור הוא b)
                    return fromSquareName.charAt(0) == c;
                } else if (c >= '1' && c <= '8') { // כפילות שורה (R1e4 -> השורה היא 1)
                    return fromSquareName.charAt(1) == c;
                }
            } else if (remainder.length() == 2) { // כפילות מלאה של משבצת מקור
                return fromSquareName.equals(remainder);
            }
        }

        return true;
    }
    public static String convertIndexToSquareName(int square) {
        int file = square % 8;
        int rank = 7 - (square / 8); // הפיכת הדירוג בלבד
        return "" + (char)('a' + file) + (rank + 1);
    }

    // מתאים את תו ה-SAN לקבועי הכלים של המנוע שלך
    private static boolean matchPieceType(int piece, char sanChar) {
        // **** קריטי: שנה כאן את המספרים בהתאם למזהי הכלים במנוע שלך! ****
        // לדוגמה, אם אצלך במנוע 1 ו-7 הם רגלי (לבן ושחור), 2 ו-8 הם פרש...
        char c = Character.toUpperCase(sanChar);
        return switch (c) {
            case 'P' -> piece == 1 || piece == 9;  // רגלי
            case 'N' -> piece == 2 || piece == 10;  // פרש
            case 'B' -> piece == 3 || piece == 11;  // רץ
            case 'R' -> piece == 4 || piece == 12; // צריח
            case 'Q' -> piece == 5 || piece == 13; // מלכה
            case 'K' -> piece == 6 || piece == 14; // מלך
            default -> false;
        };
    }

    public static void emptyBinaryFile(String path) {
        try {
            Files.write(Paths.get(path), new byte[0]);
           // System.out.println("הקובץ רוקן לחלוטין.");
        } catch (IOException e) {
            System.err.println("שגיאה בריקון הקובץ: " + e.getMessage());
        }
    }

    public static void readBinaryFile(String path) {
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            System.out.println("הקובץ ריק או לא קיים.");
            return;
        }

        System.out.println("\n--- קריאת נתונים מהקובץ הבינארי (" + path + ") ---");

        // שימוש ב-DataInputStream כדי לקרוא טיפוסים פרימיטיביים (Long, Int) בדיוק כפי שנכתבו
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)))) {
            int recordCount = 0;

            // כל רשומה היא בדיוק 16 בייטים (8 למפתח, 4 למהלך, 4 למשקל)
            while (dis.available() >= 16) {
                long zobristKey = dis.readLong();
                int move = dis.readInt();
                int weight = dis.readInt();

                // פענוח המהלך מתוך ה-32 ביטים שלו כדי להציג אותו בצורה אנושית
                int from = move & 0x3F;
                int to = (move >> 6) & 0x3F;
                int movingPiece = (move >> 12) & 0xF;
                int promotion = (move >> 20) & 0xF;

                String fromSquare = convertIndexToSquareName(from);
                String toSquare = convertIndexToSquareName(to);

                // הדפסת המידע בצורה מיושרת ומסודרת
                System.out.printf("Record #%-4d | Key: %016X | Move: %s -> %s | Piece: %-2d | Promotion: %-2d | Weight: %d%n",
                        ++recordCount, zobristKey, fromSquare, toSquare, movingPiece, promotion, weight);

                // מנגנון הגנה: כיוון שיש לך אלפי משחקים, אנחנו לא רוצים להציף את הטרמינל במיליוני שורות
                if (recordCount >= 1000) {
                    System.out.println("... הדפסה נעצרה לאחר 100 רשומות כדי למנוע קריסה/הצפה של הטרמינל ...");
                    break;
                }
            }

            // הדפסת גודל הקובץ האמיתי בשטח
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("קריאה הסתיימה בהצלחה. גודל הקובץ הכולל: " + file.length() + " בייטים.");

        } catch (IOException e) {
            System.err.println("שגיאה בפענוח וקריאת הקובץ הבינארי: " + e.getMessage());
        }
    }
}