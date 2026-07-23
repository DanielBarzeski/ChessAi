import App.GameFrame;
import Logic.OpeningBooks;
import LookUpTables.*;
import Ziobrist.ZobristKeys;
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
