package Logic;

import LookUpTables.*;
import Ziobrist.*;

import static LookUpTables.Evaluation.*;

public class Position {
    private static final int[][] MOVES = new int[50][218];
    private static final int[] CASTLING_MASK = new int[64];
    public static final int
            NONE = 0,
            WP = 1, WN = 2, WB = 3, WR = 4, WQ = 5, WK = 6,
            BP = 9, BN = 10, BB = 11, BR = 12, BQ = 13, BK = 14,
    /// white king whiteTurn:
    WKS = 0b0001,
    /// white queen whiteTurn
    WQS = 0b0010,
    /// black king whiteTurn
    BKS = 0b0100,
    /// black queen whiteTurn
    BQS = 0b1000;

    private final long[] allPieces, keyHistory;
    private final int[] piecesValues, enPassantRecords;
    private long myPieces, enemyPieces, occupancy, kingAttackers, zobristKey;
    private int halfMoveClock, enPassant, castlingRights, kingSquare, whiteTurn, ply, depthPly, movesAmount, currentMove;

    public Position() {
        depthPly = 0;
        ply = 0;
        this.enPassantRecords = new int[1024];
        this.keyHistory = new long[1024];
        this.allPieces = new long[15];
        this.allPieces[BP] = 0x000000000000FF00L;
        this.allPieces[BN] = 0x0000000000000042L;
        this.allPieces[BB] = 0x0000000000000024L;
        this.allPieces[BR] = 0x0000000000000081L;
        this.allPieces[BQ] = 0x0000000000000008L;
        this.allPieces[BK] = 0x0000000000000010L;
        this.allPieces[WP] = 0x00FF000000000000L;
        this.allPieces[WN] = 0x4200000000000000L;
        this.allPieces[WB] = 0x2400000000000000L;
        this.allPieces[WR] = 0x8100000000000000L;
        this.allPieces[WQ] = 0x0800000000000000L;
        this.allPieces[WK] = 0x1000000000000000L;
        this.piecesValues = new int[64];
        for (int square = 0; square < 64; square++) {
            CASTLING_MASK[square] = ~0;
            for (int piece = 1; piece < 15; piece++) {
                if ((allPieces[piece] & 1L << square) != 0) {
                    piecesValues[square] = piece;
                    break;
                }
            }
            if (piecesValues[square] == WK) {
                CASTLING_MASK[square] = ~(WKS | WQS);
            } else if (piecesValues[square] == BK) {
                CASTLING_MASK[square] = ~(BKS | BQS);
            }
        }
        CASTLING_MASK[0] = ~BQS; // a8
        CASTLING_MASK[7] = ~BKS; // h8
        CASTLING_MASK[56] = ~WQS; // a1
        CASTLING_MASK[63] = ~WKS; // h1
        this.castlingRights = 0b1111;
        this.whiteTurn = 0;
        this.zobristKey = ZobristKeys.generateInitialKey(piecesValues, false, castlingRights, -1);
    }

    public Position(Position other) {
        allPieces = new long[15];
        piecesValues = new int[64];
        enPassantRecords = new int[1024];
        keyHistory = new long[1024];
        if (other != null) {
            myPieces = other.myPieces;
            enemyPieces = other.enemyPieces;
            occupancy = other.occupancy;
            kingAttackers = other.kingAttackers;
            zobristKey = other.zobristKey;
            halfMoveClock = other.halfMoveClock;
            enPassant = other.enPassant;
            castlingRights = other.castlingRights;
            kingSquare = other.kingSquare;
            whiteTurn = other.whiteTurn;
            ply = other.ply;
            depthPly = other.depthPly;
            movesAmount = other.movesAmount;
            currentMove = other.currentMove;
            System.arraycopy(other.allPieces, 0, allPieces, 0, 15);
            System.arraycopy(other.piecesValues, 0, piecesValues, 0, 64);
            System.arraycopy(other.keyHistory, 0, keyHistory, 0, ply);
            System.arraycopy(other.enPassantRecords, 0, enPassantRecords, 0, ply);
        }
    }

    public long generateInitialKey() {
        long generatedKey = ZobristKeys.generateInitialKey(piecesValues, whiteTurn == 8, castlingRights, -1);
        int from = currentMove & 0x3F;
        int to = (currentMove >> 6) & 0x3F;
        long toMask = 1L << to;
        int movingPiece = (currentMove >> 12) & 0xF;
        long leftNeighbor = (toMask >>> 1) & 0x7F7F7F7F7F7F7F7FL;
        long rightNeighbor = (toMask << 1) & 0xFEFEFEFEFEFEFEFEL;
        long adjacentMask = leftNeighbor | rightNeighbor;
        int moveDistance = to - from;
        if (movingPiece == (BP ^ whiteTurn) &&
                (moveDistance == 16 || moveDistance == -16) &&
                (adjacentMask & allPieces[WP | whiteTurn]) != 0) {
            int newEnPassant = from + (moveDistance >> 1);
            if (ply > 0 && this.enPassantRecords[ply -1] == enPassant) {
                generatedKey = ZobristKeys.updateEnPassant(generatedKey, -1, newEnPassant % 8);
            }
        }
        return generatedKey;
    }


    private long calculatePinnedPieces() {
        long bishopAttackers = allPieces[BB ^ whiteTurn] | allPieces[BQ ^ whiteTurn];
        long rookAttackers = allPieces[BR ^ whiteTurn] | allPieces[BQ ^ whiteTurn];
        if ((bishopAttackers | rookAttackers) == 0L) return 0L;
        long potentialPins = (BishopMoves.getPossibleMoves(kingSquare, enemyPieces) & bishopAttackers) |
                (RookMoves.getPossibleMoves(kingSquare, enemyPieces) & rookAttackers);
        long pinnedMask = 0L;
        while (potentialPins != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(potentialPins);
            long piecesInBetween = SquaresBetween.RAYS_BETWEEN[kingSquare][attackerSquare] & occupancy;
            if ((piecesInBetween & myPieces) != 0 && piecesInBetween != 0 && (piecesInBetween & (piecesInBetween - 1)) == 0) {
                pinnedMask |= SquaresBetween.RAYS_THROUGH[kingSquare][attackerSquare];
            }
            potentialPins &= potentialPins - 1;
        }
        return pinnedMask;
    }

    private long calculateCheckMask() {
        if (kingAttackers == 0L) {
            return -1L;
        }
        long nonSliders = allPieces[BP ^ whiteTurn] | allPieces[BN ^ whiteTurn];
        if ((kingAttackers & nonSliders) != 0) {
            return kingAttackers;
        }
        return SquaresBetween.RAYS_THROUGH[kingSquare][Long.numberOfTrailingZeros(kingAttackers)];
    }

    public boolean isSquareAttacked(int sq, long requiredOccupancy) {
        return (RookMoves.getPossibleMoves(sq, requiredOccupancy) & (allPieces[BR ^ whiteTurn] | allPieces[BQ ^ whiteTurn])) != 0L ||
                ((BishopMoves.getPossibleMoves(sq, requiredOccupancy) & (allPieces[BB ^ whiteTurn] | allPieces[BQ ^ whiteTurn])) != 0L) ||
                ((KnightMoves.ATTACKS[sq] & allPieces[BN ^ whiteTurn]) | (PawnMoves.ATTACKS[whiteTurn >> 3][sq] & allPieces[BP ^ whiteTurn])
                        | (KingMoves.ATTACKS[sq] & allPieces[BK ^ whiteTurn])) != 0L;
    }

    private boolean isEnPassantLegal(int pawnSquare, int enPassantSquare) {
        long tempOccupancy = occupancy & ~(1L << pawnSquare) & ~(1L << enPassantSquare + 8 - (whiteTurn << 1)) | 1L << enPassantSquare;
        return (RookMoves.getPossibleMoves(kingSquare, tempOccupancy) & (allPieces[BR ^ whiteTurn] | allPieces[BQ ^ whiteTurn])) == 0L &&
                (BishopMoves.getPossibleMoves(kingSquare, tempOccupancy) & (allPieces[BB ^ whiteTurn] | allPieces[BQ ^ whiteTurn])) == 0L;
    }

    public static int encodeMove(
            int from,          // 0-63. 0b 111 111 // 6  (0-5)
            int to,            // 0-63. 0b 111 111 // 6  (6-11)
            int movingPiece,   // 0 = no piece, 1-14 = piece to move. 0b1111 // 4 (12-15)
            int captured,      // 0 = no capture, 1-14 = piece captured. 0b1111 // 4  (16-19)
            int promotion,     // 0 = no promotion, 1-14 = piece to promote. 0b1111 // 4 (20-23)
            int castling,      // 0 = none, 1 = queenside, 2 = kingside. 0b11 // 2 (24-25)
            int castlingRights,// 0b1111 // 4 (26 - 29)
            int enPassant      // 0 = no, 1 = yes. 0b1 // 1 (30)
    ) {
        return from |                    // 6 bits for from
                (to << 6) |              // 6 bits for to
                (movingPiece << 12) |    // 4 bits for a moving piece(0-14)
                (captured << 16) |       // 4 bits for captured (0-14)
                (promotion << 20) |      // 4 bits for promoting
                (castling << 24) |       // 2 bits for casting
                (castlingRights << 26) | // 4 bits for castling rights
                (enPassant << 30);       // 1 bit for en passant
    }

    public int[] getAllClearedMoves() {
        depthPly = 0;
        return getAllMoves();
    }

    public int[] getAllMoves() {
        int myKingIndex = WK | whiteTurn;
        myPieces = allPieces[WP | whiteTurn] | allPieces[WN | whiteTurn] | allPieces[WB | whiteTurn] |
                allPieces[WR | whiteTurn] | allPieces[WQ | whiteTurn] | allPieces[myKingIndex];
        enemyPieces = allPieces[BP ^ whiteTurn] | allPieces[BN ^ whiteTurn] | allPieces[BB ^ whiteTurn] |
                allPieces[BR ^ whiteTurn] | allPieces[BQ ^ whiteTurn] | allPieces[BK ^ whiteTurn];
        occupancy = myPieces | enemyPieces;
        kingSquare = Long.numberOfTrailingZeros(allPieces[myKingIndex]);
        kingAttackers = PawnMoves.ATTACKS[whiteTurn >> 3][kingSquare] & allPieces[BP ^ whiteTurn]
                | KnightMoves.ATTACKS[kingSquare] & allPieces[BN ^ whiteTurn]
                | BishopMoves.getPossibleMoves(kingSquare, occupancy) & (allPieces[BB ^ whiteTurn] | allPieces[BQ ^ whiteTurn])
                | RookMoves.getPossibleMoves(kingSquare, occupancy) & (allPieces[BR ^ whiteTurn] | allPieces[BQ ^ whiteTurn]);
        movesAmount = 0;
        int[] currentMoves = MOVES[depthPly];
        long kingLegalMoves = KingMoves.ATTACKS[kingSquare] & ~myPieces;
        int currentCastling = (castlingRights >> ((whiteTurn >> 3) << 1)) & 3;
        if (currentCastling != 0 && kingAttackers == 0L) {
            int rowOffset = kingSquare & 0x38;
            int isKingAtE = kingSquare & 4;
            if ((currentCastling & 1) != 0 && ((occupancy & ((0x70L - (isKingAtE << 2)) << rowOffset)) == 0L) &&
                    (!isSquareAttacked(kingSquare + 1, occupancy) && !isSquareAttacked(kingSquare + 2, occupancy))) {
                kingLegalMoves |= 1L << (kingSquare + 2);
            }
            if ((currentCastling & 2) != 0 && ((occupancy & ((0x06L + (isKingAtE << 1)) << rowOffset)) == 0L) &&
                    (!isSquareAttacked(kingSquare - 1, occupancy) && !isSquareAttacked(kingSquare - 2, occupancy))) {
                kingLegalMoves |= 1L << (kingSquare - 2);
            }
        }
        long occupancyWithoutKing = occupancy & ~(1L << kingSquare);
        while (kingLegalMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(kingLegalMoves);
            if (isSquareAttacked(toSquare, occupancyWithoutKing)) {
                kingLegalMoves &= kingLegalMoves - 1;
                continue;
            }
            int capturedPiece = piecesValues[toSquare];
            int diff = toSquare - kingSquare;
            if (diff == 2) {
                currentMoves[movesAmount++] = encodeMove(kingSquare, toSquare, myKingIndex, capturedPiece, 0, 2, castlingRights, 0);
            } else if (diff == -2) {
                currentMoves[movesAmount++] = encodeMove(kingSquare, toSquare, myKingIndex, capturedPiece, 0, 1, castlingRights, 0);
            } else {
                currentMoves[movesAmount++] = encodeMove(kingSquare, toSquare, myKingIndex, capturedPiece, 0, 0, castlingRights, 0);
            }
            kingLegalMoves &= kingLegalMoves - 1;
        }
        if ((kingAttackers & (kingAttackers - 1)) != 0) {
            return currentMoves;
        }
        long checked = calculateCheckMask();
        long pinned = calculatePinnedPieces();
        long pawns = allPieces[WP | whiteTurn];
        int shiftedRank = (whiteTurn == 0) ? 8 : -8;
        long enemyEpPawnSquare = 1L << (enPassant + shiftedRank);
        long enemyPawns = allPieces[BP ^ whiteTurn];
        while (pawns != 0) {
            int fromSquare = Long.numberOfTrailingZeros(pawns);
            long legalMoves = PawnMoves.PUSHES[whiteTurn >> 3][fromSquare] & ~occupancy;
            if (legalMoves != 0L) {
                legalMoves |= PawnMoves.DOUBLE_PUSHES[whiteTurn >> 3][fromSquare] & ~occupancy;
            }
            long attackMask = PawnMoves.ATTACKS[whiteTurn >> 3][fromSquare];
            legalMoves |= attackMask & enemyPieces;
            long pawnChecked = checked;
            long fromBB = 1L << fromSquare;
            long leftNeighbor = (fromBB >>> 1) & 0x7F7F7F7F7F7F7F7FL;
            long rightNeighbor = (fromBB << 1) & 0xFEFEFEFEFEFEFEFEL;
            long adjacentMask = leftNeighbor | rightNeighbor;
            if (enPassant != 0 && (attackMask & 1L << enPassant) != 0L && isEnPassantLegal(fromSquare, enPassant)) {
                if ((enemyEpPawnSquare & enemyPawns & adjacentMask) != 0L && (kingAttackers == 0L || (kingAttackers & enemyEpPawnSquare) != 0L)) {
                    legalMoves |= 1L << enPassant;
                    pawnChecked |= 1L << enPassant;
                }
            }
            legalMoves &= pawnChecked;
            if ((1L << fromSquare & pinned) != 0L) {
                legalMoves &= SquaresBetween.LINES[kingSquare][fromSquare] & pinned;
            }
            int movingPiece = WP | whiteTurn;
            while (legalMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(legalMoves);
                int capturedPiece = piecesValues[toSquare];
                boolean isPromoting = toSquare < 8 || toSquare > 55;
                if (isPromoting) {
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WQ | whiteTurn, 0, castlingRights, 0);
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WR | whiteTurn, 0, castlingRights, 0);
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WB | whiteTurn, 0, castlingRights, 0);
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WN | whiteTurn, 0, castlingRights, 0);
                } else if ((enemyEpPawnSquare & enemyPawns & adjacentMask) != 0L && enPassant != 0 && enPassant == toSquare) {
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, BP ^ whiteTurn, 0, 0, castlingRights, 1);
                } else {
                    currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, 0, 0, castlingRights, 0);
                }
                legalMoves &= legalMoves - 1;
            }
            pawns &= pawns - 1;
        }
        long knights = allPieces[WN | whiteTurn];
        while (knights != 0) {
            int fromSquare = Long.numberOfTrailingZeros(knights);
            long legalMoves = KnightMoves.ATTACKS[fromSquare] & ~myPieces;
            knights = getLegalMoves(currentMoves, checked, pinned, knights, fromSquare, legalMoves, WN);
        }
        long bishops = allPieces[WB | whiteTurn];
        while (bishops != 0) {
            int fromSquare = Long.numberOfTrailingZeros(bishops);
            long legalMoves = BishopMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            bishops = getLegalMoves(currentMoves, checked, pinned, bishops, fromSquare, legalMoves, WB);
        }
        long rooks = allPieces[WR | whiteTurn];
        while (rooks != 0) {
            int fromSquare = Long.numberOfTrailingZeros(rooks);
            long legalMoves = RookMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            rooks = getLegalMoves(currentMoves, checked, pinned, rooks, fromSquare, legalMoves, WR);
        }
        long queens = allPieces[WQ | whiteTurn];
        while (queens != 0) {
            int fromSquare = Long.numberOfTrailingZeros(queens);
            long legalMoves = QueenMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            queens = getLegalMoves(currentMoves, checked, pinned, queens, fromSquare, legalMoves, WQ);
        }
        return currentMoves;
    }

    private long getLegalMoves(int[] currentMoves, long checked, long pinned, long pieces, int fromSquare, long legalMoves, int pieceType) {
        legalMoves &= checked;
        if ((1L << fromSquare & pinned) != 0L) {
            legalMoves &= SquaresBetween.LINES[kingSquare][fromSquare] & pinned;
        }
        int movingPiece = pieceType | whiteTurn;
        while (legalMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(legalMoves);
            int capturedPiece = piecesValues[toSquare];
            currentMoves[movesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, 0, 0, castlingRights, 0);
            legalMoves &= legalMoves - 1;
        }
        pieces &= pieces - 1;
        return pieces;
    }

    public int getMovesLength() {
        return movesAmount;
    }

    public void sortMoves(int[] moves, int count, int ttMove) {

        for (int i = 0; i < count - 1; i++) {
            int bestIndex = i;
            int bestScore = getMoveScoreWithTt(moves[i], ttMove);

            for (int j = i + 1; j < count; j++) {
                int currentScore = getMoveScoreWithTt(moves[j], ttMove);
                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestIndex = j;
                }
            }

            if (bestIndex != i) {
                int temp = moves[i];
                moves[i] = moves[bestIndex];
                moves[bestIndex] = temp;
            }
        }
    }

    // פונקציית עזר קטנה שמחזירה את הציון כולל בדיקת ה-TT Move
    private int getMoveScoreWithTt(int move, int ttMove) {
        // אם המהלך הוא מהלך ה-TT (ובתנאי שקיים מהלך כזה - שונה מ-0)
        if (ttMove != 0 && move == ttMove) {
            return Integer.MAX_VALUE; // עדיפות עליונה מוחלטת
        }
        return scoreMove(move);
    }

    private int scoreMove(int move) {
        int promotion = (move >> 20) & 0xF;
        if (promotion != 0) {
            return 90000 + PIECE_VALUES_ABS_MG[promotion];
        }
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        if (captured != 0) {
            return 10000 + (PIECE_VALUES_ABS_MG[captured] * 10) - PIECE_VALUES_ABS_MG[movingPiece];
        }
        return 0;
    }

    public void move(int move) {
        keyHistory[ply] = zobristKey;
        this.currentMove = move;
        int from = move & 0x3F;
        int to = (move >> 6) & 0x3F;
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        int promotion = (move >> 20) & 0xF;
        int castling = (move >> 24) & 0x3;
        int enPassant = (move >> 30) & 0x1;
        long fromMask = 1L << from;
        long toMask = 1L << to;

        if (enPassant == 1) {
            int pawnSquare = (24 + whiteTurn) | (to & 7);
            allPieces[captured] &= ~(1L << pawnSquare);
            piecesValues[pawnSquare] = NONE;
            this.zobristKey = ZobristKeys.updateRemovePiece(this.zobristKey, captured, pawnSquare);
        } else if (captured != NONE) {
            // Regular capture
            allPieces[captured] &= ~toMask;
            this.zobristKey = ZobristKeys.updateRemovePiece(this.zobristKey, captured, to);
        }

        //  Check for promotion
        if (promotion == 0) {
            // Regular move
            allPieces[movingPiece] = (allPieces[movingPiece] & ~fromMask) | toMask;
            piecesValues[from] = NONE;
            piecesValues[to] = movingPiece;
            this.zobristKey = ZobristKeys.updateMovePiece(this.zobristKey, movingPiece, from, to);
        } else {
            allPieces[movingPiece] &= ~fromMask;
            piecesValues[from] = NONE;
            allPieces[promotion] |= toMask;
            piecesValues[to] = promotion;
            this.zobristKey = ZobristKeys.updatePromotionPiece(this.zobristKey, movingPiece, promotion, from, to);
        }
        long leftNeighbor = (toMask >>> 1) & 0x7F7F7F7F7F7F7F7FL;
        long rightNeighbor = (toMask << 1) & 0xFEFEFEFEFEFEFEFEL;
        long adjacentMask = leftNeighbor | rightNeighbor;
        int moveDistance = to - from;
        if (movingPiece == (WP | whiteTurn) &&
                (moveDistance == 16 || moveDistance == -16) &&
                (adjacentMask & allPieces[BP ^ whiteTurn]) != 0) {
            int newEnPassant = from + (moveDistance >> 1);
            if (this.enPassant == 0) {
                this.zobristKey = ZobristKeys.updateEnPassant(this.zobristKey, -1, newEnPassant % 8);
            }
            this.enPassant = newEnPassant;
            this.enPassantRecords[ply] = this.enPassant;
        } else if (this.enPassant != 0) {
            this.zobristKey = ZobristKeys.updateEnPassant(this.zobristKey, this.enPassant % 8, -1);
            this.enPassant = 0;
        }


        // Handle castling - move the rook
        if (castling != 0) {
            int rookPiece = WR | whiteTurn;
            int rookTo = (from + to) >> 1;
            int rookFrom = (from & 0x38) | ((castling - 1) * 7);
            allPieces[rookPiece] &= ~(1L << rookFrom);
            allPieces[rookPiece] |= (1L << rookTo);
            piecesValues[rookFrom] = NONE;
            piecesValues[rookTo] = rookPiece;
            this.zobristKey = ZobristKeys.updateMovePiece(this.zobristKey, rookPiece, rookFrom, rookTo);
        }
        int newRights = castlingRights & CASTLING_MASK[from] & CASTLING_MASK[to];
        this.zobristKey = ZobristKeys.updateCastling(this.zobristKey, castlingRights, newRights);
        castlingRights = newRights;


        ply++;
        depthPly++;
        whiteTurn ^= 8;
        this.zobristKey = ZobristKeys.updateSideToMove(this.zobristKey);
    }


    public void undoMove(int move) {
        ply--;
        depthPly--;
        this.zobristKey = keyHistory[ply];

        int to = (move >> 6) & 0x3F;
        int from = move & 0x3F;
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        int promotion = (move >> 20) & 0xF;
        int castling = (move >> 24) & 0x3;
        int castlingRights = (move >> 26) & 0xF;
        int enPassant = (move >> 30) & 0x1;
        long fromMask = 1L << from;
        long toMask = 1L << to;
        if (ply > 0) {
            this.enPassant = this.enPassantRecords[ply - 1];
        }
        if (promotion == 0) {
            allPieces[movingPiece] = (allPieces[movingPiece] & ~toMask) | fromMask;
        } else {
            allPieces[promotion] &= ~toMask;
            allPieces[movingPiece] |= fromMask;
        }
        piecesValues[to] = NONE;
        piecesValues[from] = movingPiece;
        if (enPassant == 1) {
            int pawnSquare = (24 + (whiteTurn ^ 8)) | (to & 7);
            allPieces[captured] |= (1L << pawnSquare);
            piecesValues[pawnSquare] = captured;
        } else if (captured != NONE) {
            allPieces[captured] |= toMask;
            piecesValues[to] = captured;
        }
        if (castling != 0) {
            int rookPiece = WR | (whiteTurn ^ 8);
            int rookTo = (from + to) >> 1;
            int isKingside = (from - to) >>> 31;
            int rookFrom = (from & 0x38) | (isKingside * 7);
            allPieces[rookPiece] &= ~(1L << rookTo);
            allPieces[rookPiece] |= (1L << rookFrom);
            piecesValues[rookTo] = NONE;
            piecesValues[rookFrom] = rookPiece;
        }
        this.castlingRights = castlingRights;
        whiteTurn ^= 8;
    }

    public int evaluate() {
        int mgScore = 0;
        int egScore = 0;
        int gamePhase = 0;

        // --- 1. חישוב שלב המשחק (Game Phase) דינמי ---
        // ספירת הכלים המשניים לקביעת שלב המשחק (לפי משקלים קבועים)
        gamePhase += Long.bitCount(allPieces[WN]) * Evaluation.PHASE_WEIGHTS[WN];
        gamePhase += Long.bitCount(allPieces[WB]) * Evaluation.PHASE_WEIGHTS[WB];
        gamePhase += Long.bitCount(allPieces[WR]) * Evaluation.PHASE_WEIGHTS[WR];
        gamePhase += Long.bitCount(allPieces[WQ]) * Evaluation.PHASE_WEIGHTS[WQ];
        gamePhase += Long.bitCount(allPieces[BN]) * Evaluation.PHASE_WEIGHTS[BN];
        gamePhase += Long.bitCount(allPieces[BB]) * Evaluation.PHASE_WEIGHTS[BB];
        gamePhase += Long.bitCount(allPieces[BR]) * Evaluation.PHASE_WEIGHTS[BR];
        gamePhase += Long.bitCount(allPieces[BQ]) * Evaluation.PHASE_WEIGHTS[BQ];

        // מניעת חריגה במקרה של הכתרת רגלים (Promotions)
        if (gamePhase > Evaluation.MAX_PHASE) {
            gamePhase = Evaluation.MAX_PHASE;
        }

        // --- 2. ערכי כלים בסיסיים (Material) ---
        // לבן
        mgScore += Long.bitCount(allPieces[WP]) * Evaluation.PIECE_VALUES_ABS_MG[WP];
        egScore += Long.bitCount(allPieces[WP]) * Evaluation.PIECE_VALUES_ABS_EG[WP];
        mgScore += Long.bitCount(allPieces[WN]) * Evaluation.PIECE_VALUES_ABS_MG[WN];
        egScore += Long.bitCount(allPieces[WN]) * Evaluation.PIECE_VALUES_ABS_EG[WN];
        mgScore += Long.bitCount(allPieces[WB]) * Evaluation.PIECE_VALUES_ABS_MG[WB];
        egScore += Long.bitCount(allPieces[WB]) * Evaluation.PIECE_VALUES_ABS_EG[WB];
        mgScore += Long.bitCount(allPieces[WR]) * Evaluation.PIECE_VALUES_ABS_MG[WR];
        egScore += Long.bitCount(allPieces[WR]) * Evaluation.PIECE_VALUES_ABS_EG[WR];
        mgScore += Long.bitCount(allPieces[WQ]) * Evaluation.PIECE_VALUES_ABS_MG[WQ];
        egScore += Long.bitCount(allPieces[WQ]) * Evaluation.PIECE_VALUES_ABS_EG[WQ];

        // שחור
        mgScore -= Long.bitCount(allPieces[BP]) * Evaluation.PIECE_VALUES_ABS_MG[BP];
        egScore -= Long.bitCount(allPieces[BP]) * Evaluation.PIECE_VALUES_ABS_EG[BP];
        mgScore -= Long.bitCount(allPieces[BN]) * Evaluation.PIECE_VALUES_ABS_MG[BN];
        egScore -= Long.bitCount(allPieces[BN]) * Evaluation.PIECE_VALUES_ABS_EG[BN];
        mgScore -= Long.bitCount(allPieces[BB]) * Evaluation.PIECE_VALUES_ABS_MG[BB];
        egScore -= Long.bitCount(allPieces[BB]) * Evaluation.PIECE_VALUES_ABS_EG[BB];
        mgScore -= Long.bitCount(allPieces[BR]) * Evaluation.PIECE_VALUES_ABS_MG[BR];
        egScore -= Long.bitCount(allPieces[BR]) * Evaluation.PIECE_VALUES_ABS_EG[BR];
        mgScore -= Long.bitCount(allPieces[BQ]) * Evaluation.PIECE_VALUES_ABS_MG[BQ];
        egScore -= Long.bitCount(allPieces[BQ]) * Evaluation.PIECE_VALUES_ABS_EG[BQ];

        // בונוס זוג רצים (Bishop Pair)
        if (Long.bitCount(allPieces[WB]) >= 2) {
            mgScore += 40;
            egScore += 40;
        }
        if (Long.bitCount(allPieces[BB]) >= 2) {
            mgScore -= 40;
            egScore -= 40;
        }

        // --- 3. מיקומי כלים (PST) מפוצלים ל-MG ו-EG ---

        // רגלים (Pawns)
        long whitePawns = allPieces[WP];
        while (whitePawns != 0) {
            int sq = Long.numberOfTrailingZeros(whitePawns);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.PAWN_PST_MG[targetSq];
            egScore += Evaluation.PAWN_PST_EG[targetSq];

            // 1. בונוס רגלי תומך (Connected Pawn)
            if ((allPieces[WP] & Evaluation.WHITE_SUPPORT_MASKS[sq]) != 0) {
                mgScore += 12; // תמריץ לבנות שרשראות רגלים יציבות
                egScore += 18;
            }

            // 2. בונוס רגלי עובר (Passed Pawn)
            if ((allPieces[BP] & Evaluation.WHITE_PASSED_PAWN_MASKS[sq]) == 0) {
                int rank = sq / 8; // רנק נוכחי (1 עד 6)
                mgScore += Evaluation.PASSED_PAWN_BONUS_MG[rank];
                egScore += Evaluation.PASSED_PAWN_BONUS_EG[rank];
            }

            whitePawns &= whitePawns - 1;
        }

        long blackPawns = allPieces[BP];
        while (blackPawns != 0) {
            int sq = Long.numberOfTrailingZeros(blackPawns);
            mgScore -= Evaluation.PAWN_PST_MG[sq];
            egScore -= Evaluation.PAWN_PST_EG[sq];

            // 1. בונוס רגלי תומך (Connected Pawn)
            if ((allPieces[BP] & Evaluation.BLACK_SUPPORT_MASKS[sq]) != 0) {
                mgScore -= 12;
                egScore -= 18;
            }

            // 2. בונוס רגלי עובר (Passed Pawn)
            if ((allPieces[WP] & Evaluation.BLACK_PASSED_PAWN_MASKS[sq]) == 0) {
                int rank = 7 - (sq / 8); // הפיכת כיוון לשחור (ככל שהלוח קטן, הוא קרוב יותר להכתרה)
                mgScore -= Evaluation.PASSED_PAWN_BONUS_MG[rank];
                egScore -= Evaluation.PASSED_PAWN_BONUS_EG[rank];
            }

            blackPawns &= blackPawns - 1;
        }

        // פרשים (Knights)
        long whiteKnights = allPieces[WN];
        while (whiteKnights != 0) {
            int sq = Long.numberOfTrailingZeros(whiteKnights);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.KNIGHT_PST_MG[targetSq];
            egScore += Evaluation.KNIGHT_PST_EG[targetSq];
            whiteKnights &= whiteKnights - 1;
        }
        long blackKnights = allPieces[BN];
        while (blackKnights != 0) {
            int sq = Long.numberOfTrailingZeros(blackKnights);
            mgScore -= Evaluation.KNIGHT_PST_MG[sq];
            egScore -= Evaluation.KNIGHT_PST_EG[sq];
            blackKnights &= blackKnights - 1;
        }

        // רצים (Bishops)
        long whiteBishops = allPieces[WB];
        while (whiteBishops != 0) {
            int sq = Long.numberOfTrailingZeros(whiteBishops);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.BISHOP_PST_MG[targetSq];
            egScore += Evaluation.BISHOP_PST_EG[targetSq];
            whiteBishops &= whiteBishops - 1;
        }
        long blackBishops = allPieces[BB];
        while (blackBishops != 0) {
            int sq = Long.numberOfTrailingZeros(blackBishops);
            mgScore -= Evaluation.BISHOP_PST_MG[sq];
            egScore -= Evaluation.BISHOP_PST_EG[sq];
            blackBishops &= blackBishops - 1;
        }

        // צריחים (Rooks)
        long whiteRooks = allPieces[WR];
        while (whiteRooks != 0) {
            int sq = Long.numberOfTrailingZeros(whiteRooks);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.ROOK_PST_MG[targetSq];
            egScore += Evaluation.ROOK_PST_EG[targetSq];
            whiteRooks &= whiteRooks - 1;
        }
        long blackRooks = allPieces[BR];
        while (blackRooks != 0) {
            int sq = Long.numberOfTrailingZeros(blackRooks);
            mgScore -= Evaluation.ROOK_PST_MG[sq];
            egScore -= Evaluation.ROOK_PST_EG[sq];
            blackRooks &= blackRooks - 1;
        }

        // מלכות (Queens)
        long whiteQueens = allPieces[WQ];
        while (whiteQueens != 0) {
            int sq = Long.numberOfTrailingZeros(whiteQueens);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.QUEEN_PST_MG[targetSq];
            egScore += Evaluation.QUEEN_PST_EG[targetSq];
            whiteQueens &= whiteQueens - 1;
        }
        long blackQueens = allPieces[BQ];
        while (blackQueens != 0) {
            int sq = Long.numberOfTrailingZeros(blackQueens);
            mgScore -= Evaluation.QUEEN_PST_MG[sq];
            egScore -= Evaluation.QUEEN_PST_EG[sq];
            blackQueens &= blackQueens - 1;
        }

        // מלכים (Kings)
        long whiteKing = allPieces[WK];
        if (whiteKing != 0) {
            int sq = Long.numberOfTrailingZeros(whiteKing);
            int targetSq = sq ^ 56;
            mgScore += Evaluation.KING_PST_MG[targetSq];
            egScore += Evaluation.KING_PST_EG[targetSq];
        }
        long blackKing = allPieces[BK];
        if (blackKing != 0) {
            int sq = Long.numberOfTrailingZeros(blackKing);
            mgScore -= Evaluation.KING_PST_MG[sq];
            egScore -= Evaluation.KING_PST_EG[sq];
        }

        // --- 4. מבנה רגלים (עובר אופטימיזציה עם מסכות קבועות מראש) ---
        for (int file = 0; file < 8; file++) {
            long mask = Evaluation.FILE_MASKS[file];

            int whitePawnsInFile = Long.bitCount(allPieces[WP] & mask);
            if (whitePawnsInFile > 1) {
                int penalty = 15 * (whitePawnsInFile - 1);
                mgScore -= penalty;
                egScore -= penalty;
            }

            int blackPawnsInFile = Long.bitCount(allPieces[BP] & mask);
            if (blackPawnsInFile > 1) {
                int penalty = 15 * (blackPawnsInFile - 1);
                mgScore += penalty;
                egScore += penalty;
            }
        }

        // --- 5. אינטרפולציה מדורגת (Tapered Phase Calculation) ---
        int score = ((mgScore * gamePhase) + (egScore * (Evaluation.MAX_PHASE - gamePhase))) / Evaluation.MAX_PHASE;

        // --- 6. היפוך פרספקטיבה והתאמת איומים / שח ---
        int finalScore = isWhiteTurn() ? score : -score;

        if (isDoubleCheck()) {
            finalScore -= 50;
        } else if (isInCheck()) {
            finalScore -= 30;
        }

        return finalScore;
    }

    private boolean hasInsufficientMaterial() {
        if ((allPieces[WP] | allPieces[BP] | allPieces[WR] | allPieces[BR] | allPieces[WQ] | allPieces[BQ]) != 0L) {
            return false;
        }
        int whitePieceCount = Long.bitCount(myPieces);
        int blackPieceCount = Long.bitCount(enemyPieces);
        int totalPieces = whitePieceCount + blackPieceCount;
        if (totalPieces == 2) return true;
        if (totalPieces == 3 && (allPieces[WN] != 0L || allPieces[BN] != 0L)) {
            return true;
        }
        long whiteBishops = allPieces[WB];
        long blackBishops = allPieces[BB];

        if (totalPieces == whitePieceCount + blackPieceCount) {
            if (Long.bitCount(allPieces[WN] | allPieces[BN]) == 0) {
                long lightSquares = 0x55AA55AA55AA55AAL;
                boolean whiteHasDark = (whiteBishops & ~lightSquares) != 0;
                boolean whiteHasLight = (whiteBishops & lightSquares) != 0;
                boolean blackHasDark = (blackBishops & ~lightSquares) != 0;
                boolean blackHasLight = (blackBishops & lightSquares) != 0;
                return !whiteHasDark && !blackHasDark || !whiteHasLight && !blackHasLight;
            }
        }
        return false;
    }

    public int getKingSquare() {
        return kingSquare;
    }

    public boolean isWhiteTurn() {
        return whiteTurn == 0;
    }

    public int getPieceValue(int square) {
        return piecesValues[square];
    }

    public boolean isPromotionMove(int from, int to) {
        int piece = piecesValues[from];
        return (piece == WP && to >= 0 && to <= 7) || (piece == BP && to >= 56 && to <= 63);
    }

    public boolean isInCheck() {
        return kingAttackers != 0L;
    }

    private boolean cantMove() {
        return movesAmount == 0;
    }

    public boolean isDoubleCheck() {
        return (kingAttackers & (kingAttackers - 1)) != 0;
    }

    public boolean isMate() {
        return isInCheck() && cantMove();
    }

    public boolean isThreefoldRepetition() {
        if (ply < 4) {
            return false;
        }
        int count = 1;

        for (int i = ply - 2; i >= 0; i -= 2) {
            if (keyHistory[i] == zobristKey) {
                count++;
                if (count >= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getDepthPly() {
        return depthPly;
    }

    public boolean isDraw() {
        // if (halfMoveClock >= 100) return true;
        if (isThreefoldRepetition()) return true;
        if (hasInsufficientMaterial()) return true;
        return !isInCheck() && cantMove();
    }

    public boolean isGameOver() {
        return isMate() || isDraw();
    }

    public int getPly() {
        return ply;
    }

    public int getCurrentMove() {
        return currentMove;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public void loadFEN(String fen) {
        for (int i = 0; i < 15; i++) {
            allPieces[i] = 0L;
        }
        for (int i = 0; i < 64; i++) {
            piecesValues[i] = NONE;
        }
        String[] parts = fen.split(" ");

        String boardPart = parts[0];
        String sidePart = parts[1];
        String castlingPart = parts[2];
        String enPassantPart = parts[3];
        halfMoveClock = (parts.length > 4)
                ? Integer.parseInt(parts[4])
                : 0;
        int fullMoveNumber = (parts.length > 5)
                ? Integer.parseInt(parts[5])
                : 1;
        ply = (fullMoveNumber - 1) * 2;
        int square = 0;
        for (int i = 0; i < boardPart.length(); i++) {
            char c = boardPart.charAt(i);
            if (c == '/') {
                continue;
            }
            if (Character.isDigit(c)) {
                square += c - '0';
            } else {
                int piece = fenCharToPiece(c);
                int actualSquare = square;
                allPieces[piece] |= (1L << actualSquare);
                piecesValues[actualSquare] = piece;
                square++;
            }
        }
        whiteTurn = sidePart.equals("w") ? 0 : 8;
        castlingRights = 0;
        if (!castlingPart.equals("-")) {
            if (castlingPart.contains("K")) castlingRights |= WKS;
            if (castlingPart.contains("Q")) castlingRights |= WQS;
            if (castlingPart.contains("k")) castlingRights |= BKS;
            if (castlingPart.contains("q")) castlingRights |= BQS;
        }

        if (enPassantPart.equals("-")) {
            enPassant = 0;
        } else {
            int file = enPassantPart.charAt(0) - 'a';
            int rank = 8 - (enPassantPart.charAt(1) - '0');
            enPassant = rank * 8 + file;
        }
    }

    private int fenCharToPiece(char c) {
        return switch (c) {
            case 'P' -> WP;
            case 'N' -> WN;
            case 'B' -> WB;
            case 'R' -> WR;
            case 'Q' -> WQ;
            case 'K' -> WK;
            case 'p' -> BP;
            case 'n' -> BN;
            case 'b' -> BB;
            case 'r' -> BR;
            case 'q' -> BQ;
            case 'k' -> BK;

            default -> NONE;
        };
    }

    public String generateFEN() {
        StringBuilder fen = new StringBuilder();

        // 1. חלק הלוח (Board representation)
        int square = 0;
        for (int r = 0; r < 8; r++) {
            int emptySquares = 0;
            for (int f = 0; f < 8; f++) {
                int actualSquare = square;


                int piece = piecesValues[actualSquare];

                if (piece == NONE) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    fen.append(pieceToFenChar(piece));
                }
                square++;
            }

            if (emptySquares > 0) {
                fen.append(emptySquares);
            }

            if (r < 7) {
                fen.append("/");
            }
        }

        // 2. תור (Active color)
        fen.append(" ").append(whiteTurn == 0 ? "w" : "b");

        // 3. זכויות הצרחה (Castling rights)
        StringBuilder castling = new StringBuilder();
        if ((castlingRights & WKS) != 0) castling.append("K");
        if ((castlingRights & WQS) != 0) castling.append("Q");
        if ((castlingRights & BKS) != 0) castling.append("k");
        if ((castlingRights & BQS) != 0) castling.append("q");

        fen.append(" ").append(castling.isEmpty() ? "-" : castling.toString());

        // 4. הכאה דרך הילוך (En passant target square)
        if (enPassant == 0) {
            fen.append(" -");
        } else {
            int rank = enPassant / 8;
            int file = enPassant % 8;

            char fileChar = (char) ('a' + file);
            char rankChar = (char) ('0' + (8 - rank));
            fen.append(" ").append(fileChar).append(rankChar);
        }

        // 5. שעון חצי-מהלך (Half move clock)
        fen.append(" ").append(halfMoveClock);

        // 6. מספר מהלך מלא (Full move number)
        int fullMoveNumber = (ply / 2) + 1;
        fen.append(" ").append(fullMoveNumber);

        return fen.toString();
    }

    // פונקציית עזר להמרת קבוע הכלי לתו FEN
    private static char pieceToFenChar(int piece) {
        if (piece == WP) return 'P';
        if (piece == WN) return 'N';
        if (piece == WB) return 'B';
        if (piece == WR) return 'R';
        if (piece == WQ) return 'Q';
        if (piece == WK) return 'K';
        if (piece == BP) return 'p';
        if (piece == BN) return 'n';
        if (piece == BB) return 'b';
        if (piece == BR) return 'r';
        if (piece == BQ) return 'q';
        if (piece == BK) return 'k';
        return '?';
    }

}