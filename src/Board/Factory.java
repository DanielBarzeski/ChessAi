package Board;

import App.*;
import Data.Game;
import File.Picture;
import Logic.MoveGenerator;
import Logic.Position;
import stockfish.EngineAnalysis;
import stockfish.StockfishService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static Data.Game.*;

public class Factory extends MouseAdapter {
    private static final int[] MOVES = new int[218];
    private static int aiMoveCounter;
    private final BoardPanel boardPanel;
    private Position position;
    private EngineAnalysis analysisResult;
    private Piece currentPiece;
    private boolean ai, enemyIsWhite;
    private ArrayList<Point> availableMoves;
    private Point fromSquare, toSquare, bestMoveFromSquare, bestMoveToSquare;
    private boolean aiIsThinking, pressed, isHoldingPiece, promoting, analysisIsInProcess;

    public Factory(BoardPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    public void clear(boolean ai) {
        if (aiMoveCounter > 0) {
            MoveGenerator.reset();
        }
        aiMoveCounter = 0;
        this.currentPiece = null;
        this.ai = ai;
        this.enemyIsWhite = isEnemyStarting();
        this.position = new Position();
        this.availableMoves = new ArrayList<>();
        this.fromSquare = new Point(-1, -1);
        this.toSquare = new Point(-1, -1);
        this.bestMoveFromSquare = new Point(-1, -1);
        this.bestMoveToSquare = new Point(-1, -1);
        //  this.aiIsThinking = false;
        this.isHoldingPiece = false;
        this.promoting = false;
        analysis(true);
    }

    private void analysis(boolean reset) {
        if (aiIsThinking) return;
        if (analysisIsInProcess) return;
        analysisIsInProcess = true;

        CompletableFuture.runAsync(() -> {
            try {
                analysisResult = StockfishService.getPositionAnalyzation(position.generateFEN(), 16);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }).thenRunAsync(() -> {
            if (reset) {
                Ruler.reset();
            } else if (analysisResult != null) {
                Ruler.setEvaluation(analysisResult, position.isWhiteTurn());
            }
            analysisIsInProcess = false;
        });

//        CompletableFuture.runAsync(() -> {
//            try {
//                 StockfishService.debugPerft(position,4);
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }).thenRunAsync(() -> analysisIsInProcess = false);
    }

    public void update() {
        if (Game.isUndoing()) {
            handleUndoingRequest();
        }
        if (!Game.isFinished()) {
            if (Game.isHinting()) {
                handleHintingRequest();
            }
            boolean aiTurn = this.position.isWhiteTurn() == enemyIsWhite;
            if (ai && aiTurn) {
                handleAI();
            }
        }
        Game.setUndoing(false);
        Game.setHint(false);
        boardPanel.repaint();
    }

    private void handleUndoingRequest() {
        if (analysisIsInProcess) return;
        if (aiIsThinking) return;
        if (Clock.white.isRunning() || Clock.black.isRunning()) {
            System.out.println();
            System.out.println("undoing a move is prohibited in real time game.");
            System.out.println();
        } else if (!RecordPanel.isEmpty()) {
            bestMoveFromSquare.move(-1, -1);
            bestMoveToSquare.move(-1, -1);
            MoveGenerator.cancel();
            if (ai) {
                position.undo(RecordPanel.getLastMove());
                RecordPanel.removeLastMove();
            }
            if (!RecordPanel.isEmpty()) {
                position.undo(RecordPanel.getLastMove());
                RecordPanel.removeLastMove();
            }
            currentPiece = null;
            fromSquare.move(-1, -1);
            toSquare.move(-1, -1);
            Game.start();
            analysis(true);
        }
    }

    private void handleHintingRequest() {
        if (Clock.white.isRunning() || Clock.black.isRunning()) {
            System.out.println();
            System.out.println("asking a hint is prohibited in real time game.");
            System.out.println();
        } else if (analysisResult != null && !aiIsThinking) {
            int from = analysisResult.getFromSquare();
            int to = analysisResult.getToSquare();
            bestMoveFromSquare.move(from % 8, from / 8);
            bestMoveToSquare.move(to % 8, to / 8);
        }
    }

    public void handleAI() {
        if (analysisIsInProcess) return;
        if (aiIsThinking) return;
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            System.out.println(e.getMessage());
//        }
        aiIsThinking = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                return MoveGenerator.getBestMove(position);
            } catch (Throwable t) {
                System.out.println("\n==================================================");
                System.out.println("exception type: " + t.getClass().getSimpleName());
                System.out.println("system message: " + t.getMessage());
                StackTraceElement[] elements = t.getStackTrace();
                for (StackTraceElement element : elements) {
                    String className = element.getClassName();
                    if (className.contains("Position") || className.contains("MoveGenerator") || className.contains("Factory") || className.contains("Game")) {
                        System.out.println(" file " + element.getFileName() + " function -> " + element.getMethodName() + "()" + " Line -> [ " + element.getLineNumber() + " ] ");
                    } else {
                        System.out.println("crash out outside from main functions - at " + element);
                    }
                }
                System.out.println("==================================================\n");
                return position.generateMoves(MOVES)[0];
            }
        }).thenAccept(aiMove -> SwingUtilities.invokeLater(() -> {
            aiMoveCounter++;
            if (!MoveGenerator.isCancelled() && !Game.isFinished()) {
                setMenuActive(false);
                System.out.println();
                System.out.println(aiMoveCounter + " Ai Calculation finished thinking & making move...\n");
                System.out.println();
                int from = aiMove & 0x3F;
                int to = (aiMove >> 6) & 0x3F;
                fromSquare.move(from % 8, from / 8);
                toSquare.move(to % 8, to / 8);
                movePosition(aiMove);
            } else {
                System.out.println();
                System.out.println(aiMoveCounter + " Ai Calculation finished thinking & move was cancelled...\n");
                System.out.println();
            }
        })).thenAccept(_ -> SwingUtilities.invokeLater(() -> {
            setMenuActive(true);
            aiIsThinking = false;
            analysis(false);
        }));
    }


    @Override
    public void mousePressed(MouseEvent e) {
        if (Game.isFinished() || promoting || aiIsThinking) return;
        if (ai && (position.isWhiteTurn() == enemyIsWhite)) return;

        pressed = true;
        int x = getCoordinateX(e.getX());
        int y = getCoordinateY(e.getY());
        int logicalCol = getLogicalCol(x / CELL_SIZE);
        int logicalRow = getLogicalRow(y / CELL_SIZE);

        Point clickedLogicalPoint = new Point(logicalCol, logicalRow);

        if (currentPiece != null && availableMoves.contains(clickedLogicalPoint)) {
            currentPiece.setCurrLocation(x, y);
            processMove(logicalCol, logicalRow);
            toSquare.move(logicalCol, logicalRow);
            return;
        }

        initializePiece(x, y, logicalCol, logicalRow, true);

        if (currentPiece != null) {
            isHoldingPiece = true;
            currentPiece.setCurrLocation(x, y);
            toSquare.move(logicalCol, logicalRow);
        }

        boardPanel.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!pressed || currentPiece == null || !isHoldingPiece) return;

        int x = getCoordinateX(e.getX());
        int y = getCoordinateY(e.getY());

        currentPiece.setCurrLocation(x, y);
        toSquare.move(getLogicalCol(x / CELL_SIZE), getLogicalRow(y / CELL_SIZE));

        boardPanel.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!pressed) return;
        pressed = false;
        isHoldingPiece = false;

        if (currentPiece != null) {
            int x = getCoordinateX(e.getX());
            int y = getCoordinateY(e.getY());
            int visualCol = x / CELL_SIZE;
            int visualRow = y / CELL_SIZE;
            int logicalCol = getLogicalCol(visualCol);
            int logicalRow = getLogicalRow(visualRow);

            Point releasedLogicalPoint = new Point(logicalCol, logicalRow);

            if (availableMoves.contains(releasedLogicalPoint)) {
                currentPiece.setCurrLocation(x, y);
                processMove(logicalCol, logicalRow);
            } else {
                if (currentPiece.inPrevCell(visualCol, visualRow)) {
                    int prevVisualCol = getVisualCol(currentPiece.getPrevSquare() % 8);
                    int prevVisualRow = getVisualRow(currentPiece.getPrevSquare() / 8);
                    currentPiece.setCurrLocation(
                            prevVisualCol * CELL_SIZE + CELL_SIZE / 2,
                            prevVisualRow * CELL_SIZE + CELL_SIZE / 2
                    );
                } else if (!promoting) {
                    currentPiece = null;
                    fromSquare.move(-1, -1);
                    toSquare.move(-1, -1);
                    availableMoves.clear();
                }
            }
        }
        boardPanel.repaint();
    }

    private void processMove(int logicalCol, int logicalRow) {
        int logicalCurSquare = logicalRow * 8 + logicalCol;
        int logicalPrevSquare = getLogicalSquare(currentPiece.getPrevSquare());

        if (position.isPromotionMove(logicalPrevSquare, logicalCurSquare)) {
            promoting = true;
            availableMoves.clear();
        } else {
            makeMove(0);
        }
    }

    private int getCoordinateX(int x) {
        return Math.clamp(x, CELL_SIZE / 2, 15 * CELL_SIZE / 2);
    }

    private int getCoordinateY(int y) {
        return Math.clamp(y, CELL_SIZE / 2, 15 * CELL_SIZE / 2);
    }

    private int getLogicalCol(int visualCol) {
        return enemyIsWhite ? 7 - visualCol : visualCol;
    }

    private int getLogicalRow(int visualRow) {
        return enemyIsWhite ? 7 - visualRow : visualRow;
    }

    private int getVisualCol(int logicalCol) {
        return enemyIsWhite ? 7 - logicalCol : logicalCol;
    }

    private int getVisualRow(int logicalRow) {
        return enemyIsWhite ? 7 - logicalRow : logicalRow;
    }

    private int getLogicalSquare(int visualSquare) {
        if (visualSquare < 0) return visualSquare;
        int col = visualSquare % 8;
        int row = visualSquare / 8;
        return getLogicalRow(row) * 8 + getLogicalCol(col);
    }

    public void makeMove(int promotionValue) {
        int logicalPrevSquare = getLogicalSquare(currentPiece.getPrevSquare());
        int logicalCurSquare = getLogicalSquare(currentPiece.getCurrSquare());

        this.currentPiece = null;
        promoting = false;
        int[] moves = position.generateMoves(MOVES);
        int move = 0;
        int length = position.getLegalMovesAmount();

        for (int i = 0; i < length; i++) {
            move = moves[i];
            int from = move & 0x3F;
            int to = (move >> 6) & 0x3F;
            if (from == logicalPrevSquare && to == logicalCurSquare) {
                break;
            }
        }
        move &= ~(0xF << 20);
        move |= (promotionValue << 20);
        movePosition(move);
        analysis(false);
    }

    public void movePosition(int move) {
        bestMoveFromSquare.move(-1, -1);
        bestMoveToSquare.move(-1, -1);
        position.move(move);
//        long currentZobristKey = position.getZobristKey();
//        long generatedZobristKey = position.generateInitialKey();
//        System.out.println("Generated Zobrist Key: " + generatedZobristKey);
//        System.out.println("Current Zobrist Key: " + currentZobristKey);
//        if (currentZobristKey == generatedZobristKey){
//            System.out.println("successfully generated Zobrist Key");
//        } else {
//            System.out.println("failed to generate Zobrist Key");
//        }
        position.generateMoves(MOVES);
        RecordPanel.addMove(position, move);
        if (position.isMate()) {
            Game.end(false);
        }
        if (position.isDraw()) {
            if (position.isFiftyMoveDraw()) {
                System.out.println("Fifty move draw");
            } else if (position.isThreefoldRepetition()) {
                System.out.println("Three fold repetition");
            } else if (position.hasInsufficientMaterial()) {
                System.out.println("Insufficient material");
            } else if (!position.isInCheck() && position.cantMove()) {
                System.out.println("stale mate");
            }
            Game.end(true);
        }
    }

    public void initializePiece(int x, int y, int col, int row, boolean tryingToGetAnotherPiece) {
        int fromSquare = row * 8 + col;
        int pieceValue = position.getPieceValue(fromSquare);
        if (pieceValue != 0) {
            Piece tempPiece = new Piece(x, y, pieceValue);
            boolean isSameColorAsTurn = (tempPiece.isWhite() == position.isWhiteTurn());
            if (isSameColorAsTurn) {
                if (tryingToGetAnotherPiece) {
                    isHoldingPiece = true;
                }
                currentPiece = tempPiece;
                availableMoves.clear();
                int[] moves = position.generateMoves(MOVES);
                int length = position.getLegalMovesAmount();
                for (int i = 0; i < length; i++) {
                    int move = moves[i];
                    int from = move & 0x3F;
                    if (from == fromSquare) {
                        int toSquareMove = (move >> 6) & 0x3F;
                        int squareRow = toSquareMove / 8;
                        int squareCol = toSquareMove % 8;
                        availableMoves.add(new Point(squareCol, squareRow));
                    }
                }
                this.fromSquare.move(col, row);
            } else if (!tryingToGetAnotherPiece) {
                this.fromSquare.move(-1, -1);
                toSquare.move(-1, -1);
            }
        } else if (!tryingToGetAnotherPiece) {
            this.fromSquare.move(-1, -1);
            toSquare.move(-1, -1);
        }
    }

    public void drawGame(Graphics g) {
        drawBoard(g);
        drawSquares(g);
        drawPieces(g);
    }

    private void drawBoard(Graphics g) {
        g.setColor(new Color(210, 180, 140));
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i + j) % 2 == 0) {
                    g.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    private void drawSquares(Graphics g) {
        g.setColor(new Color(255, 200, 0, 150));

        if (toSquare.x >= 0 && toSquare.y >= 0) {
            g.fillRect(getVisualCol(toSquare.x) * CELL_SIZE, getVisualRow(toSquare.y) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }
        if (fromSquare.x >= 0 && fromSquare.y >= 0) {
            g.fillRect(getVisualCol(fromSquare.x) * CELL_SIZE, getVisualRow(fromSquare.y) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        if (currentPiece != null) {
            g.setColor(new Color(70, 130, 180, 100));
            for (Point availableMove : availableMoves) {
                g.fillRect(getVisualCol(availableMove.x) * CELL_SIZE, getVisualRow(availableMove.y) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        g.setColor(new Color(50, 150, 50, 180));
        if (bestMoveFromSquare.x >= 0 && bestMoveFromSquare.y >= 0) {
            g.fillRect(getVisualCol(bestMoveFromSquare.x) * CELL_SIZE, getVisualRow(bestMoveFromSquare.y) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            g.fillRect(getVisualCol(bestMoveToSquare.x) * CELL_SIZE, getVisualRow(bestMoveToSquare.y) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        if (Game.isFinished()) {
            if (Game.isDraw()){
                int myKingSquare = position.getMyKingSquare();
                int enemyKingSquare = position.getEnemyKingSquare();
                g.setColor(Color.blue.darker());
                g.fillRect(getVisualCol(myKingSquare % 8) * CELL_SIZE, getVisualRow(myKingSquare / 8) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.fillRect(getVisualCol(enemyKingSquare % 8) * CELL_SIZE, getVisualRow(enemyKingSquare / 8) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            } else {
                int kingSquare = position.getMyKingSquare();
                g.setColor(new Color(128, 0, 32, 200));
                g.fillRect(getVisualCol(kingSquare % 8) * CELL_SIZE, getVisualRow(kingSquare / 8) * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void drawPieces(Graphics g) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int visualCol = getVisualCol(col);
                int visualRow = getVisualRow(row);

                boolean paintPiece = currentPiece == null || !currentPiece.inPrevCell(visualCol, visualRow) || (!isHoldingPiece && !promoting);
                if (paintPiece) {
                    int pieceValue = this.position.getPieceValue(row * 8 + col);
                    if (pieceValue != 0) {
                        g.drawImage(Picture.getImage(pieceValue),
                                visualCol * CELL_SIZE, visualRow * CELL_SIZE, CELL_SIZE, CELL_SIZE, null
                        );
                    }
                }
            }
        }
        if (currentPiece != null) {
            if (isHoldingPiece) {
                int pieceValue = currentPiece.getValue();
                g.drawImage(Picture.getImage(pieceValue),
                        currentPiece.getCurrX() - CELL_SIZE / 2, currentPiece.getCurrY() - CELL_SIZE / 2,
                        CELL_SIZE, CELL_SIZE, null
                );
            }
        }
    }

    public boolean isWhiteTurn() {
        return position.isWhiteTurn();
    }

    public int getCurrSquare() {
        if (currentPiece == null) return -1;
        return getLogicalSquare(currentPiece.getCurrSquare());
    }

    public boolean isPromoting() {
        return this.promoting;
    }
}