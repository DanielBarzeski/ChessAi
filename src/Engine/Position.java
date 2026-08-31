package Engine;

import LookUpTables.Evaluation;
import LookUpTables.*;

public class Position {
    public static final int NONE = 0;
    public static final int WP = 1, WN = 2, WB = 3, WR = 4, WQ = 5, WK = 6;
    public static final int BP = 9, BN = 10, BB = 11, BR = 12, BQ = 13, BK = 14;
    private static final int[] CASTLING_MASK = new int[64];
    private static final int WKS = 0b0001, WQS = 0b0010, BKS = 0b0100, BQS = 0b1000;

    private long[] keyHistory;
    private long zobristKey;
    private int[] epSquareHistory;
    private int currentEpSquare;
    private int[] halfMoveClockHistory;
    private int halfMoveClock;

    private long[] pieces;
    private int[] board;
    private long occupancy;
    private long myPieces, enemyPieces;
    private long myKingAttackers;
    private int myKingSquare, enemyKingSquare;
    private int whiteTurn;
    private int currentMove;
    private int castlingRights;
    private int legalMovesAmount;
    private int ply;

    private int gamePhase;
    private int mgScore, egScore;

    public Position() {
        loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public Position(Position other) {
        pieces = new long[15];
        board = new int[64];
        epSquareHistory = new int[1024];
        keyHistory = new long[1024];
        halfMoveClockHistory = new int[1024];
        if (other != null) {
            zobristKey = other.zobristKey;
            currentEpSquare = other.currentEpSquare;
            occupancy = other.occupancy;
            myPieces = other.myPieces;
            enemyPieces = other.enemyPieces;
            myKingSquare = other.myKingSquare;
            enemyKingSquare = other.enemyKingSquare;
            whiteTurn = other.whiteTurn;
            currentMove = other.currentMove;
            castlingRights = other.castlingRights;
            halfMoveClock = other.halfMoveClock;
            legalMovesAmount = other.legalMovesAmount;
            ply = other.ply;
            gamePhase = other.gamePhase;
            mgScore = other.mgScore;
            egScore = other.egScore;
            myKingAttackers = other.myKingAttackers;
            System.arraycopy(other.pieces, 0, pieces, 0, 15);
            System.arraycopy(other.board, 0, board, 0, 64);
            System.arraycopy(other.keyHistory, 0, keyHistory, 0, ply);
            System.arraycopy(other.epSquareHistory, 0, epSquareHistory, 0, ply);
            System.arraycopy(other.halfMoveClockHistory, 0, halfMoveClockHistory, 0, ply);
        }
    }

    public void resetEvaluation() {
        gamePhase = 0;
        mgScore = 0;
        egScore = 0;

        for (int square = 0; square < 64; square++) {
            int piece = board[square];
            if (piece == NONE) continue;

            gamePhase += Evaluation.PHASE_WEIGHTS[piece];
            mgScore += Evaluation.PIECE_PST_MG[piece][square];
            egScore += Evaluation.PIECE_PST_EG[piece][square];
        }
    }

    public static int encodeMove(
            int from,          // 0-63. 0b 111 111 // 6  (0-5)
            int to,            // 0-63. 0b 111 111 // 6  (6-11)
            int movingPiece,   // 0 = no piece, 1-14 = piece to move. 0b1111 // 4 (12-15)
            int captured,      // 0 = no capture, 1-14 = piece captured. 0b1111 // 4  (16-19)
            int promotion,     // 0 = no promotion, 1-14 = piece to promote. 0b1111 // 4 (20-23)
            int castling,      // 0 = none, 1 = queenside, 2 = kingside. 0b11 // 2 (24-25)
            int castlingRights,// 0b1111 // 4 (26 - 29)
            int enPassant,     // 0 = no, 1 = yes. 0b1 // 1 (30)
            int givesCheck     // 0 = no, 1 = yes. 0b1 // 1 (31) -> negative number.
    ) {
        return from |                    // 6 bits for from
                (to << 6) |              // 6 bits for to
                (movingPiece << 12) |    // 4 bits for a moving piece(0-14)
                (captured << 16) |       // 4 bits for captured (0-14)
                (promotion << 20) |      // 4 bits for promoting
                (castling << 24) |       // 2 bits for casting
                (castlingRights << 26) | // 4 bits for castling rights
                (enPassant << 30) |      // 1 bit for en passant
                (givesCheck << 31);      // 1 bit for giving a check
    }

    private long calculatePinnedPieces() {
        long bishopAttackers = pieces[BB ^ whiteTurn] | pieces[BQ ^ whiteTurn];
        long rookAttackers = pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn];
        if ((bishopAttackers | rookAttackers) == 0L) return 0L;
        long potentialPins = (BishopMoves.getPossibleMoves(myKingSquare, enemyPieces) & bishopAttackers) |
                (RookMoves.getPossibleMoves(myKingSquare, enemyPieces) & rookAttackers);
        long pinnedMask = 0L;
        while (potentialPins != 0L) {
            int attackerSquare = Long.numberOfTrailingZeros(potentialPins);
            long piecesInBetween = SquaresBetween.RAYS_BETWEEN[myKingSquare][attackerSquare] & occupancy;
            if ((piecesInBetween & myPieces) != 0 && piecesInBetween != 0 && (piecesInBetween & (piecesInBetween - 1)) == 0) {
                pinnedMask |= SquaresBetween.RAYS_THROUGH[myKingSquare][attackerSquare];
            }
            potentialPins &= potentialPins - 1;
        }
        return pinnedMask;
    }

    private long calculateCheckMask(long kingAttackers) {
        if (kingAttackers == 0L) {
            return -1L;
        }
        long nonSliders = pieces[BP ^ whiteTurn] | pieces[BN ^ whiteTurn];
        if ((kingAttackers & nonSliders) != 0) {
            return kingAttackers;
        }
        return SquaresBetween.RAYS_THROUGH[myKingSquare][Long.numberOfTrailingZeros(kingAttackers)];
    }

    public boolean isSquareAttacked(int sq, long requiredOccupancy) {
        return (RookMoves.getPossibleMoves(sq, requiredOccupancy) & (pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn])) != 0L ||
                ((BishopMoves.getPossibleMoves(sq, requiredOccupancy) & (pieces[BB ^ whiteTurn] | pieces[BQ ^ whiteTurn])) != 0L) ||
                ((KnightMoves.ATTACKS[sq] & pieces[BN ^ whiteTurn]) | (PawnMoves.ATTACKS[whiteTurn >> 3][sq] & pieces[BP ^ whiteTurn])
                        | (KingMoves.ATTACKS[sq] & pieces[BK ^ whiteTurn])) != 0L;
    }

    private boolean isEnPassantLegal(int pawnSquare, int enPassantSquare) {
        long tempOccupancy = occupancy & ~(1L << pawnSquare) & ~(1L << enPassantSquare + 8 - (whiteTurn << 1)) | 1L << enPassantSquare;
        return (RookMoves.getPossibleMoves(myKingSquare, tempOccupancy) & (pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn])) == 0L &&
                (BishopMoves.getPossibleMoves(myKingSquare, tempOccupancy) & (pieces[BB ^ whiteTurn] | pieces[BQ ^ whiteTurn])) == 0L;
    }

    public int[] generateMoves(int[] moves) {
        legalMovesAmount = 0;
        myPieces = pieces[WP | whiteTurn] | pieces[WN | whiteTurn] | pieces[WB | whiteTurn] |
                pieces[WR | whiteTurn] | pieces[WQ | whiteTurn] | pieces[WK | whiteTurn];
        enemyPieces = pieces[BP ^ whiteTurn] | pieces[BN ^ whiteTurn] | pieces[BB ^ whiteTurn] |
                pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn] | pieces[BK ^ whiteTurn];
        occupancy = myPieces | enemyPieces;
        int myKingIndex = WK | whiteTurn;
        myKingSquare = Long.numberOfTrailingZeros(pieces[myKingIndex]);
        enemyKingSquare = Long.numberOfTrailingZeros(pieces[BK ^ whiteTurn]);
        myKingAttackers = PawnMoves.ATTACKS[whiteTurn >> 3][myKingSquare] & pieces[BP ^ whiteTurn]
                | KnightMoves.ATTACKS[myKingSquare] & pieces[BN ^ whiteTurn]
                | BishopMoves.getPossibleMoves(myKingSquare, occupancy) & (pieces[BB ^ whiteTurn] | pieces[BQ ^ whiteTurn])
                | RookMoves.getPossibleMoves(myKingSquare, occupancy) & (pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn]);
        long doubleCheck = myKingAttackers & (myKingAttackers - 1);

        long notDoubleCheckMask = ~((doubleCheck | -doubleCheck) >> 63);
        long checked = calculateCheckMask(myKingAttackers);
        long pinned = calculatePinnedPieces();

        long pawnCheckMask = PawnMoves.ATTACKS[(whiteTurn ^ 8) >> 3][enemyKingSquare];
        long knightCheckMask = KnightMoves.ATTACKS[enemyKingSquare];
        long bishopCheckMask = BishopMoves.getPossibleMoves(enemyKingSquare, occupancy);
        long rookCheckMask = RookMoves.getPossibleMoves(enemyKingSquare, occupancy);
        long queenCheckMask = bishopCheckMask | rookCheckMask;
        long pawns = pieces[WP | whiteTurn] & notDoubleCheckMask;
        int shiftedRank = (whiteTurn == 0) ? 8 : -8;
        long enemyEpPawnSquare = 1L << (currentEpSquare + shiftedRank);
        long enemyPawns = pieces[BP ^ whiteTurn];
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
            if (currentEpSquare != 0 && (attackMask & 1L << currentEpSquare) != 0L && isEnPassantLegal(fromSquare, currentEpSquare)) {
                if ((enemyEpPawnSquare & enemyPawns & adjacentMask) != 0L && (myKingAttackers == 0L || (myKingAttackers & enemyEpPawnSquare) != 0L)) {
                    legalMoves |= 1L << currentEpSquare;
                    pawnChecked |= 1L << currentEpSquare;
                }
            }
            legalMoves &= pawnChecked;
            if ((1L << fromSquare & pinned) != 0L) {
                legalMoves &= SquaresBetween.LINES[myKingSquare][fromSquare] & pinned;
            }
            int movingPiece = WP | whiteTurn;
            while (legalMoves != 0) {
                int toSquare = Long.numberOfTrailingZeros(legalMoves);
                int capturedPiece = board[toSquare];
                boolean isPromoting = toSquare < 8 || toSquare > 55;
                if (isPromoting) {
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WQ | whiteTurn, 0, castlingRights, 0,(int) ((queenCheckMask >>> toSquare) & 1));
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WR | whiteTurn, 0, castlingRights, 0,(int) ((rookCheckMask >>> toSquare) & 1));
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WB | whiteTurn, 0, castlingRights, 0,(int) ((bishopCheckMask >>> toSquare) & 1));
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, WN | whiteTurn, 0, castlingRights, 0,(int) ((knightCheckMask >>> toSquare) & 1));
                } else if ((enemyEpPawnSquare & enemyPawns & adjacentMask) != 0L && currentEpSquare != 0 && currentEpSquare == toSquare) {
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, BP ^ whiteTurn, 0, 0, castlingRights, 1,(int) ((pawnCheckMask >>> toSquare) & 1));
                } else {
                    moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, 0, 0, castlingRights, 0,(int) ((pawnCheckMask >>> toSquare) & 1));
                }
                legalMoves &= legalMoves - 1;
            }
            pawns &= pawns - 1;
        }
        long knights = pieces[WN | whiteTurn] & notDoubleCheckMask;
        while (knights != 0) {
            int fromSquare = Long.numberOfTrailingZeros(knights);
            long legalMoves = KnightMoves.ATTACKS[fromSquare] & ~myPieces;
            knights = getLegalMoves(moves, checked, pinned, knights, fromSquare, legalMoves, WN, knightCheckMask);
        }
        long bishops = pieces[WB | whiteTurn] & notDoubleCheckMask;
        while (bishops != 0) {
            int fromSquare = Long.numberOfTrailingZeros(bishops);
            long legalMoves = BishopMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            bishops = getLegalMoves(moves, checked, pinned, bishops, fromSquare, legalMoves, WB, bishopCheckMask);
        }
        long queens = pieces[WQ | whiteTurn] & notDoubleCheckMask;
        while (queens != 0) {
            int fromSquare = Long.numberOfTrailingZeros(queens);
            long legalMoves = QueenMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            queens = getLegalMoves(moves, checked, pinned, queens, fromSquare, legalMoves, WQ, queenCheckMask);
        }
        long kingLegalMoves = KingMoves.ATTACKS[myKingSquare] & ~myPieces;
        int currentCastling = (castlingRights >> ((whiteTurn >> 3) << 1)) & 3;
        if (currentCastling != 0 && myKingAttackers == 0L) {
            int rowOffset = myKingSquare & 0x38;
            int isKingAtE = myKingSquare & 4;
            if ((currentCastling & 1) != 0 && ((occupancy & ((0x70L - (isKingAtE << 2)) << rowOffset)) == 0L) &&
                    (!isSquareAttacked(myKingSquare + 1, occupancy) && !isSquareAttacked(myKingSquare + 2, occupancy))) {
                kingLegalMoves |= 1L << (myKingSquare + 2);
            }
            if ((currentCastling & 2) != 0 && ((occupancy & ((0x06L + (isKingAtE << 1)) << rowOffset)) == 0L) &&
                    (!isSquareAttacked(myKingSquare - 1, occupancy) && !isSquareAttacked(myKingSquare - 2, occupancy))) {
                kingLegalMoves |= 1L << (myKingSquare - 2);
            }
        }
        long occupancyWithoutKing = occupancy & ~(1L << myKingSquare);
        while (kingLegalMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(kingLegalMoves);
            if (isSquareAttacked(toSquare, occupancyWithoutKing)) {
                kingLegalMoves &= kingLegalMoves - 1;
                continue;
            }
            int capturedPiece = board[toSquare];
            int diff = toSquare - myKingSquare;
            moves[legalMovesAmount++] = encodeMove(myKingSquare, toSquare, myKingIndex, capturedPiece, 0, diff == 2 ? 2 : (diff == -2 ? 1 : 0), castlingRights, 0,0);
            kingLegalMoves &= kingLegalMoves - 1;
        }
        long rooks = pieces[WR | whiteTurn] & notDoubleCheckMask;
        while (rooks != 0) {
            int fromSquare = Long.numberOfTrailingZeros(rooks);
            long legalMoves = RookMoves.getPossibleMoves(fromSquare, occupancy) & ~myPieces;
            rooks = getLegalMoves(moves, checked, pinned, rooks, fromSquare, legalMoves, WR, rookCheckMask);
        }
        return moves;
    }

    private long getLegalMoves(int[] moves, long checked, long pinned, long pieces, int fromSquare, long legalMoves, int pieceType, long checkMask) {
        legalMoves &= checked;
        if ((1L << fromSquare & pinned) != 0L) {
            legalMoves &= SquaresBetween.LINES[myKingSquare][fromSquare] & pinned;
        }
        int movingPiece = pieceType | whiteTurn;
        while (legalMoves != 0) {
            int toSquare = Long.numberOfTrailingZeros(legalMoves);
            int capturedPiece = board[toSquare];
            moves[legalMovesAmount++] = encodeMove(fromSquare, toSquare, movingPiece, capturedPiece, 0, 0, castlingRights, 0,(int) ((checkMask >>> toSquare) & 1));
            legalMoves &= legalMoves - 1;
        }
        pieces &= pieces - 1;
        return pieces;
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

    private int getMoveScoreWithTt(int move, int ttMove) {
        if (ttMove != 0 && move == ttMove) {
            return 1000000;
        }
        return scoreMove(move);
    }

    private int scoreMove(int move) {
        int promotion = (move >> 20) & 0xF;
        if (promotion != 0) {
            return 90000 + Evaluation.PIECE_VALUES_MG[promotion];
        }
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        if (captured != 0) {
            return 10000 + (Evaluation.PIECE_VALUES_MG[captured] * 10) - Evaluation.PIECE_VALUES_MG[movingPiece];
        }
        if (move < 0) {
            return 5000;
        }
        return 0;
    }

    public void move(int move) {
        currentMove = move;
        keyHistory[ply] = zobristKey;
        halfMoveClockHistory[ply] = halfMoveClock;
        int from = move & 0x3F;
        int to = (move >> 6) & 0x3F;
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        int promotion = (move >> 20) & 0xF;
        int castling = (move >> 24) & 0x3;
        int enPassant = (move >> 30) & 0x1;
        long fromMask = 1L << from;
        long toMask = 1L << to;
        halfMoveClock++;
        if (captured != NONE || movingPiece == (WP | whiteTurn)) {
            halfMoveClock = 0;
        }
        // --- 1. הכאות ---
        if (enPassant == 1) {
            int pawnSquare = (24 + whiteTurn) | (to & 7);
            pieces[captured] &= ~(1L << pawnSquare);
            board[pawnSquare] = NONE;
            zobristKey = ZobristKeys.updateRemovePiece(zobristKey, captured, pawnSquare);

            // הסרת הכלב שנאכל ב-En Passant
            mgScore -= Evaluation.PIECE_PST_MG[captured][pawnSquare];
            egScore -= Evaluation.PIECE_PST_EG[captured][pawnSquare];
            gamePhase -= Evaluation.PHASE_WEIGHTS[captured];
        } else if (captured != NONE) {
            pieces[captured] &= ~toMask;
            zobristKey = ZobristKeys.updateRemovePiece(zobristKey, captured, to);

            // הסרת הכלי שנאכל בהכאה רגילה
            mgScore -= Evaluation.PIECE_PST_MG[captured][to];
            egScore -= Evaluation.PIECE_PST_EG[captured][to];
            gamePhase -= Evaluation.PHASE_WEIGHTS[captured];
        }

        // --- 2. תנועה / הכתרה ---
        if (promotion == 0) {
            pieces[movingPiece] = (pieces[movingPiece] & ~fromMask) | toMask;
            board[from] = NONE;
            board[to] = movingPiece;
            zobristKey = ZobristKeys.updateMovePiece(zobristKey, movingPiece, from, to);
            // עדכון הזזת הכלי מ-from ל-to בשורת חיסור/חיבור אחת
            mgScore += Evaluation.PIECE_PST_MG[movingPiece][to] - Evaluation.PIECE_PST_MG[movingPiece][from];
            egScore += Evaluation.PIECE_PST_EG[movingPiece][to] - Evaluation.PIECE_PST_EG[movingPiece][from];
        } else {
            pieces[movingPiece] &= ~fromMask;
            board[from] = NONE;
            pieces[promotion] |= toMask;
            board[to] = promotion;
            zobristKey = ZobristKeys.updatePromotionPiece(zobristKey, movingPiece, promotion, from, to);

            // הסרת הרגלי והוספת הכלי המוגדל
            mgScore += Evaluation.PIECE_PST_MG[promotion][to] - Evaluation.PIECE_PST_MG[movingPiece][from];
            egScore += Evaluation.PIECE_PST_EG[promotion][to] - Evaluation.PIECE_PST_EG[movingPiece][from];
            gamePhase += Evaluation.PHASE_WEIGHTS[promotion] - Evaluation.PHASE_WEIGHTS[movingPiece];
        }

        // --- 3. En Passant Tracker ---
        long leftNeighbor = (toMask >>> 1) & 0x7F7F7F7F7F7F7F7FL;
        long rightNeighbor = (toMask << 1) & 0xFEFEFEFEFEFEFEFEL;
        long adjacentMask = leftNeighbor | rightNeighbor;
        int moveDistance = to - from;
        if (movingPiece == (WP | whiteTurn) &&
                (moveDistance == 16 || moveDistance == -16) &&
                (adjacentMask & pieces[BP ^ whiteTurn]) != 0) {
            int newEnPassant = from + (moveDistance >> 1);
            if (currentEpSquare == 0) {
                zobristKey = ZobristKeys.updateEnPassant(zobristKey, -1, newEnPassant % 8);
            }
            currentEpSquare = newEnPassant;
            epSquareHistory[ply] = currentEpSquare;
        } else if (currentEpSquare != 0) {
            zobristKey = ZobristKeys.updateEnPassant(zobristKey, currentEpSquare % 8, -1);
            currentEpSquare = 0;
        }

        // --- 4. הצרחה ---
        if (castling != 0) {
            int rookPiece = WR | whiteTurn;
            int rookTo = (from + to) >> 1;
            int rookFrom = (from & 0x38) | ((castling - 1) * 7);
            pieces[rookPiece] &= ~(1L << rookFrom);
            pieces[rookPiece] |= (1L << rookTo);
            board[rookFrom] = NONE;
            board[rookTo] = rookPiece;
            zobristKey = ZobristKeys.updateMovePiece(zobristKey, rookPiece, rookFrom, rookTo);

            // עדכון הזזת הצריח בהצרחה
            mgScore += Evaluation.PIECE_PST_MG[rookPiece][rookTo] - Evaluation.PIECE_PST_MG[rookPiece][rookFrom];
            egScore += Evaluation.PIECE_PST_EG[rookPiece][rookTo] - Evaluation.PIECE_PST_EG[rookPiece][rookFrom];
        }

        int newRights = castlingRights & CASTLING_MASK[from] & CASTLING_MASK[to];
        zobristKey = ZobristKeys.updateCastling(zobristKey, castlingRights, newRights);
        castlingRights = newRights;

        ply++;
        whiteTurn ^= 8;
        zobristKey = ZobristKeys.updateSideToMove(zobristKey);
    }


    public void undo(int move) {
        ply--;
        zobristKey = keyHistory[ply];
        halfMoveClock = halfMoveClockHistory[ply];
        if (ply > 0) currentEpSquare = epSquareHistory[ply-1];
        int to = (move >> 6) & 0x3F;
        int from = move & 0x3F;
        int movingPiece = (move >> 12) & 0xF;
        int captured = (move >> 16) & 0xF;
        int promotion = (move >> 20) & 0xF;
        int castling = (move >> 24) & 0x3;
        int previousCastlingRights = (move >> 26) & 0xF;
        int enPassant = (move >> 30) & 0x1;
        long fromMask = 1L << from;
        long toMask = 1L << to;
        // --- 1. ביטול תנועה / הכתרה ---
        if (promotion == 0) {
            pieces[movingPiece] = (pieces[movingPiece] & ~toMask) | fromMask;

            // החזרת הכלי מ-to ל-from
            mgScore += Evaluation.PIECE_PST_MG[movingPiece][from] - Evaluation.PIECE_PST_MG[movingPiece][to];
            egScore += Evaluation.PIECE_PST_EG[movingPiece][from] - Evaluation.PIECE_PST_EG[movingPiece][to];
        } else {
            pieces[promotion] &= ~toMask;
            pieces[movingPiece] |= fromMask;

            // ביטול הכתרה: החזרת הרגלי והסרת הכלי המוגדל
            mgScore += Evaluation.PIECE_PST_MG[movingPiece][from] - Evaluation.PIECE_PST_MG[promotion][to];
            egScore += Evaluation.PIECE_PST_EG[movingPiece][from] - Evaluation.PIECE_PST_EG[promotion][to];
            gamePhase += Evaluation.PHASE_WEIGHTS[movingPiece] - Evaluation.PHASE_WEIGHTS[promotion];
        }
        board[to] = NONE;
        board[from] = movingPiece;

        // --- 2. שחזור כלים שנלכדו ---
        if (enPassant == 1) {
            int pawnSquare = (24 + (whiteTurn ^ 8)) | (to & 7);
            pieces[captured] |= (1L << pawnSquare);
            board[pawnSquare] = captured;

            // החזרת הכלי שנאכל ב-En Passant
            mgScore += Evaluation.PIECE_PST_MG[captured][pawnSquare];
            egScore += Evaluation.PIECE_PST_EG[captured][pawnSquare];
            gamePhase += Evaluation.PHASE_WEIGHTS[captured];
        } else if (captured != NONE) {
            pieces[captured] |= toMask;
            board[to] = captured;

            // החזרת הכלי שנאכל בהכאה רגילה
            mgScore += Evaluation.PIECE_PST_MG[captured][to];
            egScore += Evaluation.PIECE_PST_EG[captured][to];
            gamePhase += Evaluation.PHASE_WEIGHTS[captured];
        }

        // --- 3. ביטול הצרחה ---
        if (castling != 0) {
            int rookPiece = WR | (whiteTurn ^ 8);
            int rookTo = (from + to) >> 1;
            int isKingside = (from - to) >>> 31;
            int rookFrom = (from & 0x38) | (isKingside * 7);
            pieces[rookPiece] &= ~(1L << rookTo);
            pieces[rookPiece] |= (1L << rookFrom);
            board[rookTo] = NONE;
            board[rookFrom] = rookPiece;

            // החזרת הצריח למיקומו המקורי
            mgScore += Evaluation.PIECE_PST_MG[rookPiece][rookFrom] - Evaluation.PIECE_PST_MG[rookPiece][rookTo];
            egScore += Evaluation.PIECE_PST_EG[rookPiece][rookFrom] - Evaluation.PIECE_PST_EG[rookPiece][rookTo];
        }

        castlingRights = previousCastlingRights;
        whiteTurn ^= 8;
    }

    public int evaluate() {
        int mg = mgScore;
        int eg = egScore;

        long wPawns = pieces[WP];
        long bPawns = pieces[BP];

        long wFileFill = Evaluation.northFill(wPawns) | Evaluation.southFill(wPawns);
        long bFileFill = Evaluation.northFill(bPawns) | Evaluation.southFill(bPawns);

        long wAdjacentFills = Evaluation.adjacentFiles(wFileFill);
        long bAdjacentFills = Evaluation.adjacentFiles(bFileFill);

        int wIsolatedCount = Long.bitCount(wPawns & ~wAdjacentFills);
        int bIsolatedCount = Long.bitCount(bPawns & ~bAdjacentFills);

        int wDoubledCount = Long.bitCount(wPawns & Evaluation.southFill(wPawns << 8));
        int bDoubledCount = Long.bitCount(bPawns & Evaluation.northFill(bPawns >>> 8));

        long wPassed = wPawns & ~Evaluation.southFill(bPawns | Evaluation.adjacentFiles(bPawns));
        long bPassed = bPawns & ~Evaluation.northFill(wPawns | Evaluation.adjacentFiles(wPawns));

        int mgPawn = -(wIsolatedCount - bIsolatedCount) * Evaluation.ISOLATED_PAWN_PENALTY_MG
                - (wDoubledCount - bDoubledCount) * Evaluation.DOUBLED_PAWN_PENALTY_MG;

        int egPawn = -(wIsolatedCount - bIsolatedCount) * Evaluation.ISOLATED_PAWN_PENALTY_EG
                - (wDoubledCount - bDoubledCount) * Evaluation.DOUBLED_PAWN_PENALTY_EG;

        while (wPassed != 0) {
            int sq = Long.numberOfTrailingZeros(wPassed);
            mgPawn += Evaluation.WHITE_PASSED_BONUS_MG[sq];
            egPawn += Evaluation.WHITE_PASSED_BONUS_EG[sq];
            wPassed &= wPassed - 1;
        }

        while (bPassed != 0) {
            int sq = Long.numberOfTrailingZeros(bPassed);
            mgPawn -= Evaluation.BLACK_PASSED_BONUS_MG[sq];
            egPawn -= Evaluation.BLACK_PASSED_BONUS_EG[sq];
            bPassed &= bPassed - 1;
        }

        mg += mgPawn;
        eg += egPawn;

        long wRooks = pieces[WR];
        while (wRooks != 0) {
            int sq = Long.numberOfTrailingZeros(wRooks);
            long fileMask = Evaluation.FILE_MASKS[sq & 7];
            if ((wPawns & fileMask) == 0) {
                if ((bPawns & fileMask) == 0) {
                    mg += Evaluation.ROOK_OPEN_FILE_BONUS_MG;
                    eg += Evaluation.ROOK_OPEN_FILE_BONUS_EG;
                } else {
                    mg += Evaluation.ROOK_SEMI_OPEN_FILE_BONUS_MG;
                    eg += Evaluation.ROOK_SEMI_OPEN_FILE_BONUS_EG;
                }
            }
            wRooks &= wRooks - 1;
        }

        long bRooks = pieces[BR];
        while (bRooks != 0) {
            int sq = Long.numberOfTrailingZeros(bRooks);
            long fileMask = Evaluation.FILE_MASKS[sq & 7];
            if ((bPawns & fileMask) == 0) {
                if ((wPawns & fileMask) == 0) {
                    mg -= Evaluation.ROOK_OPEN_FILE_BONUS_MG;
                    eg -= Evaluation.ROOK_OPEN_FILE_BONUS_EG;
                } else {
                    mg -= Evaluation.ROOK_SEMI_OPEN_FILE_BONUS_MG;
                    eg -= Evaluation.ROOK_SEMI_OPEN_FILE_BONUS_EG;
                }
            }
            bRooks &= bRooks - 1;
        }

        long wPiecesMask = pieces[WP] | pieces[WN] | pieces[WB] | pieces[WR] | pieces[WQ] | pieces[WK];
        long bPiecesMask = pieces[BP] | pieces[BN] | pieces[BB] | pieces[BR] | pieces[BQ] | pieces[BK];
        long allOccupancy = wPiecesMask | bPiecesMask;

        if (Long.bitCount(pieces[WB]) >= 2) {
            mg += Evaluation.BISHOP_PAIR_BONUS_MG;
            eg += Evaluation.BISHOP_PAIR_BONUS_EG;
        }
        if (Long.bitCount(pieces[BB]) >= 2) {
            mg -= Evaluation.BISHOP_PAIR_BONUS_MG;
            eg -= Evaluation.BISHOP_PAIR_BONUS_EG;
        }

        long tempKnights = pieces[WN];
        while (tempKnights != 0) {
            int sq = Long.numberOfTrailingZeros(tempKnights);
            int mobility = Long.bitCount(KnightMoves.ATTACKS[sq] & ~wPiecesMask);
            mg += mobility * Evaluation.KNIGHT_MOBILITY_MG;
            eg += mobility * Evaluation.KNIGHT_MOBILITY_EG;
            tempKnights &= tempKnights - 1;
        }
        tempKnights = pieces[BN];
        while (tempKnights != 0) {
            int sq = Long.numberOfTrailingZeros(tempKnights);
            int mobility = Long.bitCount(KnightMoves.ATTACKS[sq] & ~bPiecesMask);
            mg -= mobility * Evaluation.KNIGHT_MOBILITY_MG;
            eg -= mobility * Evaluation.KNIGHT_MOBILITY_EG;
            tempKnights &= tempKnights - 1;
        }

        long tempBishops = pieces[WB];
        while (tempBishops != 0) {
            int sq = Long.numberOfTrailingZeros(tempBishops);
            int mobility = Long.bitCount(BishopMoves.getPossibleMoves(sq, allOccupancy) & ~wPiecesMask);
            mg += mobility * Evaluation.BISHOP_MOBILITY_MG;
            eg += mobility * Evaluation.BISHOP_MOBILITY_EG;
            tempBishops &= tempBishops - 1;
        }
        tempBishops = pieces[BB];
        while (tempBishops != 0) {
            int sq = Long.numberOfTrailingZeros(tempBishops);
            int mobility = Long.bitCount(BishopMoves.getPossibleMoves(sq, allOccupancy) & ~bPiecesMask);
            mg -= mobility * Evaluation.BISHOP_MOBILITY_MG;
            eg -= mobility * Evaluation.BISHOP_MOBILITY_EG;
            tempBishops &= tempBishops - 1;
        }

        int wKingSq = Long.numberOfTrailingZeros(pieces[WK]);
        int bKingSq = Long.numberOfTrailingZeros(pieces[BK]);

        int wShield = Long.bitCount(Evaluation.KING_SHIELD_MASKS[0][wKingSq] & wPawns);
        int bShield = Long.bitCount(Evaluation.KING_SHIELD_MASKS[1][bKingSq] & bPawns);
        mg += (wShield - bShield) * Evaluation.KING_SHIELD_BONUS_MG;

        long wKingFileMask = Evaluation.FILE_MASKS[wKingSq & 7];
        if ((wPawns & wKingFileMask) == 0) {
            mg -= ((bPawns & wKingFileMask) == 0) ? Evaluation.OPEN_FILE_KING_PENALTY : Evaluation.SEMI_OPEN_FILE_KING_PENALTY;
        }

        long bKingFileMask = Evaluation.FILE_MASKS[bKingSq & 7];
        if ((bPawns & bKingFileMask) == 0) {
            mg += ((wPawns & bKingFileMask) == 0) ? Evaluation.OPEN_FILE_KING_PENALTY : Evaluation.SEMI_OPEN_FILE_KING_PENALTY;
        }

        int phase = Math.min(gamePhase, 24);

        int wKingRank = wKingSq >> 3, wKingFile = wKingSq & 7;
        int bKingRank = bKingSq >> 3, bKingFile = bKingSq & 7;
        int distBetweenKings = Math.abs(wKingRank - bKingRank) + Math.abs(wKingFile - bKingFile);

        int bDstCenter = Math.max(3 - bKingFile, bKingFile - 4) + Math.max(3 - bKingRank, bKingRank - 4);
        int wMatingEval = (bDstCenter * 4) + ((14 - distBetweenKings) * 2);

        int wDstCenter = Math.max(3 - wKingFile, wKingFile - 4) + Math.max(3 - wKingRank, wKingRank - 4);
        int bMatingEval = (wDstCenter * 4) + ((14 - distBetweenKings) * 2);

        long wNonPawn = pieces[WN] | pieces[WB] | pieces[WR] | pieces[WQ];
        long bNonPawn = pieces[BN] | pieces[BB] | pieces[BR] | pieces[BQ];

        if (phase < 6) {
            if (eg > 200 && bNonPawn == 0) {
                eg += wMatingEval;
            } else if (eg < -200 && wNonPawn == 0) {
                eg -= bMatingEval;
            }
        }

        if (whiteTurn == 0) {
            mg += Evaluation.TEMPO_BONUS;
            eg += Evaluation.TEMPO_BONUS;
        } else {
            mg -= Evaluation.TEMPO_BONUS;
            eg -= Evaluation.TEMPO_BONUS;
        }

        int eval = (mg * phase + eg * (24 - phase)) / 24;

        int sign = whiteTurn >> 3;
        return (eval ^ -sign) + sign;
    }

    public boolean cantMove() {
        return legalMovesAmount == 0;
    }

    public boolean isInCheck() {
        return myKingAttackers != 0;
    }

    public boolean isInDoubleCheck() {
        return (myKingAttackers & (myKingAttackers - 1)) != 0;
    }

    public boolean isGivingCheck() {
        return currentMove < 0;
    }

    public boolean isFiftyMoveDraw() {
        return halfMoveClock >= 100;
    }

    public boolean isThreefoldRepetition() {
        int count = 0;
        int limit = Math.max(0, ply - halfMoveClock);
        for (int i = ply - 2; i >= limit; i -= 2) {
            if (keyHistory[i] == zobristKey) {
                count++;
                if (count >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSearchRepetition() {
        int limit = Math.max(0, ply - halfMoveClock);
        for (int i = ply - 2; i >= limit; i -= 2) {
            if (keyHistory[i] == zobristKey) {
                return true;
            }
        }
        return false;
    }

    public boolean isForcedDraw() {
        if (isFiftyMoveDraw()) return true;
        if (isThreefoldRepetition()) return true;
        return hasInsufficientMaterial();
    }

    public boolean isDraw() {
        if (isForcedDraw()) return true;
        return !isInCheck() && cantMove();
    }

    public boolean isMate() {
        return isInCheck() && cantMove();
    }

    public boolean hasInsufficientMaterial() {
        if ((pieces[WP] | pieces[BP] | pieces[WR] | pieces[BR] | pieces[WQ] | pieces[BQ]) != 0L) {
            return false;
        }
        long knights = pieces[WN] | pieces[BN];
        long bishops = pieces[WB] | pieces[BB];
        long minors = knights | bishops;
        if ((minors & (minors - 1)) == 0L) {
            return true;
        }
        if (knights != 0L) {
            return false;
        }
        long lightSquares = 0x55AA55AA55AA55AAL;
        return (bishops & lightSquares) == 0L || (bishops & ~lightSquares) == 0L;
    }

    public void loadFEN(String fen) {
        pieces = new long[15];
        board = new int[64];
        epSquareHistory = new int[1024];
        keyHistory = new long[1024];
        halfMoveClockHistory = new int[1024];
        String[] parts = fen.trim().split("\\s+");
        String boardPart = parts[0];
        String sidePart = parts[1];
        String castlingPart = parts[2];
        String enPassantPart = parts[3];
        halfMoveClock = (parts.length > 4) ? Integer.parseInt(parts[4]) : 0;
        int fullMoveNumber = (parts.length > 5) ? Integer.parseInt(parts[5]) : 1;
        whiteTurn = sidePart.equals("w") ? 0 : 8;
        ply = (fullMoveNumber - 1) * 2 + (whiteTurn == 8 ? 1 : 0);
        int square = 0;
        for (int i = 0; i < boardPart.length(); i++) {
            char c = boardPart.charAt(i);
            if (c == '/') continue;
            if (Character.isDigit(c)) {
                square += c - '0';
            } else {
                int piece = " PNBRQK  pnbrqk".indexOf(c);
                if (piece > 0 && square < 64) {
                    pieces[piece] |= (1L << square);
                    board[square] = piece;
                }
                square++;
            }
        }
        for (int sq = 0; sq < 64; sq++) {
            CASTLING_MASK[sq] = ~0;
            if (board[sq] == WK) {
                CASTLING_MASK[sq] &= ~(WKS | WQS);
            } else if (board[sq] == BK) {
                CASTLING_MASK[sq] &= ~(BKS | BQS);
            }
        }
        CASTLING_MASK[0] &= ~BQS;
        CASTLING_MASK[7] &= ~BKS;
        CASTLING_MASK[56] &= ~WQS;
        CASTLING_MASK[63] &= ~WKS;
        castlingRights = 0;
        if (!castlingPart.equals("-")) {
            if (castlingPart.contains("K")) castlingRights |= WKS;
            if (castlingPart.contains("Q")) castlingRights |= WQS;
            if (castlingPart.contains("k")) castlingRights |= BKS;
            if (castlingPart.contains("q")) castlingRights |= BQS;
        }
        if (enPassantPart.equals("-")) {
            currentEpSquare = 0;
        } else {
            int file = enPassantPart.charAt(0) - 'a';
            int rank = 8 - (enPassantPart.charAt(1) - '0');
            currentEpSquare = rank * 8 + file;
        }
        myPieces = 0L;
        enemyPieces = 0L;
        for (int p = WP | whiteTurn; p <= (WK | whiteTurn); p++) myPieces |= pieces[p];
        for (int p = BP ^ whiteTurn; p <= (BK ^ whiteTurn); p++) enemyPieces |= pieces[p];
        occupancy = myPieces | enemyPieces;
        myKingSquare = Long.numberOfTrailingZeros(pieces[WK | whiteTurn]);
        enemyKingSquare = Long.numberOfTrailingZeros(pieces[BK ^ whiteTurn]);
        int epFile = (currentEpSquare == 0) ? -1 : (currentEpSquare % 8);
        boolean isBlackToMove = (whiteTurn != 0);
        zobristKey = ZobristKeys.generateInitialKey(board, isBlackToMove, castlingRights, epFile);
        keyHistory[ply] = zobristKey;
        epSquareHistory[ply] = currentEpSquare;
        halfMoveClockHistory[ply] = halfMoveClock;
        myKingAttackers = KnightMoves.ATTACKS[myKingSquare] & pieces[BN ^ whiteTurn]
                | PawnMoves.ATTACKS[whiteTurn >> 3][myKingSquare] & pieces[BP ^ whiteTurn]
                | BishopMoves.getPossibleMoves(myKingSquare, occupancy) & (pieces[BB ^ whiteTurn] | pieces[BQ ^ whiteTurn])
                | RookMoves.getPossibleMoves(myKingSquare, occupancy) & (pieces[BR ^ whiteTurn] | pieces[BQ ^ whiteTurn]);
        currentMove = 0;
        legalMovesAmount = -1;

        resetEvaluation();
    }

    public String generateFEN() {
        StringBuilder fen = new StringBuilder();
        int square = 0;
        for (int r = 0; r < 8; r++) {
            int emptySquares = 0;
            for (int f = 0; f < 8; f++) {
                int piece = board[square];
                if (piece == NONE) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    fen.append(" PNBRQK  pnbrqk".charAt(piece));
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
        fen.append(" ").append(whiteTurn == 0 ? "w" : "b");
        StringBuilder castling = new StringBuilder();
        if ((castlingRights & WKS) != 0) castling.append("K");
        if ((castlingRights & WQS) != 0) castling.append("Q");
        if ((castlingRights & BKS) != 0) castling.append("k");
        if ((castlingRights & BQS) != 0) castling.append("q");
        fen.append(" ").append(castling.isEmpty() ? "-" : castling.toString());
        int enPassant = currentEpSquare;
        if (enPassant == 0) {
            fen.append(" -");
        } else {
            int file = enPassant % 8;
            int rank = enPassant / 8;
            char fileChar = (char) ('a' + file);
            char rankChar = (char) ('0' + (8 - rank));
            fen.append(" ").append(fileChar).append(rankChar);
        }
        fen.append(" ").append(halfMoveClock);
        int fullMoveNumber = (ply / 2) + 1;
        fen.append(" ").append(fullMoveNumber);
        return fen.toString();
    }

    public int getPieceValue(int square) {
        return board[square];
    }

    public boolean isPromotionMove(int from, int to) {
        int piece = board[from];
        return (piece == WP && to >= 0 && to <= 7) || (piece == BP && to >= 56 && to <= 63);
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public int getLegalMovesAmount() {
        return legalMovesAmount;
    }

    public boolean isWhiteTurn() {
        return whiteTurn == 0;
    }

    public int getCurrentMove() {
        return currentMove;
    }

    public int getMyKingSquare() {
        return myKingSquare;
    }

    public int getEnemyKingSquare() {
        return enemyKingSquare;
    }
}