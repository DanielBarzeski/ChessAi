package Logic;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class OpeningBooks {
    private static final Map<Long, List<BookMove>> BOOKS = new HashMap<>();

    public static void loadBook() {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream("assets/openings/games.bin")))) {
            while (dis.available() >= 16) {
                long key = dis.readLong();     // 8 bytes
                int moveBits = dis.readInt();  // 4 bytes
                int weight = dis.readInt();    // 4 bytes
                BOOKS.computeIfAbsent(key, k -> new ArrayList<>()).add(new BookMove(moveBits, weight));
            }
        } catch (IOException ex) {
            System.err.println("Error loading opening book: " + ex.getMessage());
        }
    }

    public static BookMove getBestMove(long boardHash) {
        List<BookMove> book = BOOKS.get(boardHash);
        if (book == null || book.isEmpty()) {
            return null;
        }

        // סריקת המהלכים ומציאת המהלך בעל המשקל הגבוה ביותר
        BookMove bestMove = book.getFirst();
        for (int i = 1; i < book.size(); i++) {
            BookMove current = book.get(i);
            // אם weight הוא מתודה (getWeight) או שדה (weight), הנתון מושווה כאן
            if (current.getWeight() > bestMove.getWeight()) {
                bestMove = current;
            }
        }
        return bestMove;
    }
}