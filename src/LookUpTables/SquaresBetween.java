package LookUpTables;

public class SquaresBetween {
    public static final long[][] RAYS_BETWEEN = precomputeRaysBetween();
    public static final long[][] RAYS_THROUGH = precomputeRaysThrough();
    public static final long[][] LINES = precomputeLines();

    private static long[][] precomputeLines() {
        long[][] lines = new long[64][64];
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                lines[from][to] = computeLine(from, to);
            }
        }
        return lines;
    }

    private static long computeLine(int from, int to) {
        if (from == to) return 0L;

        int r1 = from / 8, f1 = from % 8;
        int r2 = to / 8, f2 = to % 8;

        int dr = Integer.signum(r2 - r1);
        int df = Integer.signum(f2 - f1);

        // בדיקה אם הם על אותו קו (ישר או אלכסוני)
        if (dr != 0 && df != 0 && Math.abs(r2 - r1) != Math.abs(f2 - f1)) return 0L;

        long line = 0L;

        // רץ לכל אורך הקו (מהמקור עד הקצה בשני הכיוונים)
        int r = r1, f = f1;
        while (r >= 0 && r < 8 && f >= 0 && f < 8) {
            line |= (1L << (r * 8 + f));
            r += dr; f += df;
        }

        r = r1 - dr; f = f1 - df; // חזרה אחורה מהמקור
        while (r >= 0 && r < 8 && f >= 0 && f < 8) {
            line |= (1L << (r * 8 + f));
            r -= dr; f -= df;
        }

        return line;
    }

    private static long[][] precomputeRaysBetween() {
        long[][] rays = new long[64][64];
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                rays[from][to] = computeRayBetween(from, to);
            }
        }
        return rays;
    }

    private static long[][] precomputeRaysThrough() {
        long[][] rays = new long[64][64];
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                rays[from][to] = computeRayBetween(from, to) | (1L << to);
            }
        }
        return rays;
    }

    private static long computeRayBetween(int from, int to) {
        if (from == to) return 0L;

        int rankDiff = to / 8 - from / 8;
        int fileDiff = to % 8 - from % 8;

        // בדוק אם הם על אותו קו
        if (rankDiff != 0 && fileDiff != 0 && Math.abs(rankDiff) != Math.abs(fileDiff)) {
            return 0L; // לא על אותו קו
        }
        if (rankDiff == 0 && fileDiff == 0) {
            return 0L;
        }

        int rankDir = Integer.signum(rankDiff);
        int fileDir = Integer.signum(fileDiff);
        int step = rankDir * 8 + fileDir;

        long ray = 0L;
        for (int sq = from + step; sq != to; sq += step) {
            ray |= 1L << sq;
        }

        return ray;
    }
}
