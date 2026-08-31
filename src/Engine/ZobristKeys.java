package Engine;

import java.io.*;
import java.util.Random;

public class ZobristKeys {

    private static final long[] pieceKeys = new long[960];
    private static final long[] castlingKeys = new long[16];
    private static final long[] enPassantKeys = new long[8];
    private static long sideToMoveKey;

    public static void load() {
        File file = new File("assets/zobrist/zobristKeys.bin");
        if (file.exists()) {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                for (int i = 0; i < 960; i++) pieceKeys[i] = dis.readLong();
                for (int i = 0; i < 16; i++) castlingKeys[i] = dis.readLong();
                for (int i = 0; i < 8; i++) enPassantKeys[i] = dis.readLong();
                sideToMoveKey = dis.readLong();
                return;
            } catch (IOException e) {
                System.err.println("Error reading Zobrist file, regenerating keys... " + e.getMessage());
                return;
            }
        }
        generateAndSave(file);
    }

    private static void generateAndSave(File file) {
        if (file.getParentFile() != null) {
            if(file.getParentFile().mkdirs()){
                System.err.println("creating directory " + file.getParentFile().getAbsolutePath());
            }
        }

        Random random = new Random(57612L);

        for (int i = 0; i < 960; i++) pieceKeys[i] = random.nextLong();
        for (int i = 0; i < 16; i++) castlingKeys[i] = random.nextLong();
        for (int i = 0; i < 8; i++) enPassantKeys[i] = random.nextLong();
        sideToMoveKey = random.nextLong();

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            for (int i = 0; i < 960; i++) dos.writeLong(pieceKeys[i]);
            for (int i = 0; i < 16; i++) dos.writeLong(castlingKeys[i]);
            for (int i = 0; i < 8; i++) dos.writeLong(enPassantKeys[i]);
            dos.writeLong(sideToMoveKey);

        } catch (IOException e) {
            System.err.println("Failed to save Zobrist keys to file: " + e.getMessage());
        }
    }

    public static long generateInitialKey(int[] board, boolean isBlackToMove, int castlingRights, int enPassantFile) {
        long key = 0;
        for (int square = 0; square < 64; square++) {
            int piece = board[square];
            if (piece != Position.NONE) {
                key ^= pieceKeys[piece * 64 + square];
            }
        }
        if (isBlackToMove) key ^= sideToMoveKey;
        key ^= castlingKeys[castlingRights];
        if (enPassantFile >= 0 && enPassantFile <= 7) key ^= enPassantKeys[enPassantFile];

        return key;
    }

    public static long updateMovePiece(long currentKey, int piece, int from, int to) {
        return currentKey ^ pieceKeys[piece * 64 + from] ^ pieceKeys[piece * 64 + to];
    }

    public static long updateRemovePiece(long currentKey, int piece, int square) {
        return currentKey ^ pieceKeys[piece * 64 + square];
    }

    public static long updatePromotionPiece(long currentKey, int oldPiece, int newPiece, int from, int to) {
        return currentKey ^ pieceKeys[oldPiece * 64 + from] ^ pieceKeys[newPiece * 64 + to];
    }

    public static long updateSideToMove(long currentKey) {
        return currentKey ^ sideToMoveKey;
    }

    public static long updateCastling(long currentKey, int oldRights, int newRights) {
        return currentKey ^ castlingKeys[oldRights] ^ castlingKeys[newRights];
    }

    public static long updateEnPassant(long currentKey, int oldFile, int newFile) {
        long key = currentKey;
        if (oldFile >= 0 && oldFile < 8) key ^= enPassantKeys[oldFile];
        if (newFile >= 0 && newFile < 8) key ^= enPassantKeys[newFile];
        return key;
    }
}