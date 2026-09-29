package App;

import Board.Factory;
import Data.Game;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {
    private final Factory board;
    private final PromotionsPanel promotionsPanel;

    public BoardPanel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setLayout(new GridBagLayout());
        setBackground(new Color(205, 143, 33).darker());
        this.board = new Factory(this);
        addMouseListener(this.board);
        addMouseMotionListener(this.board);
        promotionsPanel = new PromotionsPanel(width, height);
        add(promotionsPanel);
        setBoard();
    }

    public void setBoard() {
        this.board.clear(Game.isAiExist());
    }

    public void update() {
        if (this.board.isPromoting()) {
            promotionsPanel.showButton(this.board.getCurrSquare());
            if (promotionsPanel.isPressed()) {
                this.board.makeMove(promotionsPanel.getValue());
                promotionsPanel.dispose();
            }
        } else {
            this.board.update();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.board.drawGame(g);
    }

    public boolean isWhiteTurn() {
        return this.board.isWhiteTurn();
    }

    public void resetPromotions() {
        promotionsPanel.dispose();
    }

}
