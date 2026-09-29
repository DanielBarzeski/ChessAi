package File;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;


public class AttacksLoader {

    public static long[][] loadBishopAttacks() {
        return loadAttacks("assets/magicBitboards/bishopAttacks.bin");
    }

    public static long[][] loadRookAttacks() {
        return loadAttacks("assets/magicBitboards/rookAttacks.bin");
    }

    private static long[][] loadAttacks(String fileName) {
        long[][] ATTACKS = new long[64][];
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)))) {
            for (int i = 0; i < 64; i++) {
                int length = dis.readInt();
                ATTACKS[i] = new long[length];
                for (int j = 0; j < length; j++) {
                    ATTACKS[i][j] = dis.readLong();
                }
            }
        } catch (IOException e) {
            System.err.println("error reading binary file: " + e.getMessage());
        }
        return ATTACKS;
    }
}