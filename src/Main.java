import App.GameFrame;
import Logic.GeneratorTest;
import Logic.OpeningBooks;
import LookUpTables.*;
import Logic.ZobristKeys;
import stockfish.StockfishService;

public class Main {
    public static void main(String[] args) {
        RookMoves.initiate();
        BishopMoves.initiate();
        ZobristKeys.load();
        StockfishService.startEngine();
//        GeneratorTest.test(6);
//        System.exit(2);
        javax.swing.SwingUtilities.invokeLater(() -> new GameFrame().setVisible(true));
        OpeningBooks.loadBook();
    }
}
