package App;


import Data.Game;
import File.Picture;
import Engine.MoveGenerator;

import javax.swing.*;
import java.awt.*;

import static Data.Game.CELL_SIZE;


public class GamePanel extends JPanel {
    private final JPanel wrapperPanel;
    private final OptionPanel optionPanel;
    private final MenuPanel menuPanel;
    private final RulerPanel rulerPanel;
    private final NumPanel numPanel;
    private final BoardPanel boardPanel;
    private final RecordPanel recordPanel;
    private final ClockPanel clockPanel;
    private boolean waite = true;

    public GamePanel() {
        setPreferredSize(new Dimension(35 * CELL_SIZE / 3, CELL_SIZE * 11));
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBackground(new Color(205, 143, 33));

        wrapperPanel = new JPanel();
        wrapperPanel.setPreferredSize(new Dimension(35 * CELL_SIZE / 3, CELL_SIZE * 11));
        wrapperPanel.setOpaque(false);
        optionPanel = new OptionPanel(300, 300);
        wrapperPanel.setLayout(new FlowLayout(FlowLayout.LEFT,
                (wrapperPanel.getPreferredSize().width) / 2 - (optionPanel.getPreferredSize().width) / 2,
                (wrapperPanel.getPreferredSize().height) / 2 - (optionPanel.getPreferredSize().height) / 2)
        );
        wrapperPanel.add(optionPanel);

        menuPanel = new MenuPanel(35 * CELL_SIZE / 3, CELL_SIZE);

        rulerPanel = new RulerPanel(4 * CELL_SIZE / 6 + 15, CELL_SIZE * 9);

        numPanel = new NumPanel(CELL_SIZE * 9, CELL_SIZE * 9, CELL_SIZE / 2, CELL_SIZE / 2);
        boardPanel = new BoardPanel(CELL_SIZE * 8, CELL_SIZE * 8);

        recordPanel = new RecordPanel(2 * CELL_SIZE - 15, CELL_SIZE * 9);

        clockPanel = new ClockPanel(35 * CELL_SIZE / 3, CELL_SIZE);

        add(wrapperPanel);
        add(menuPanel);
        add(rulerPanel);
        numPanel.add(boardPanel);
        add(numPanel);
        add(recordPanel);
        add(clockPanel);
        update(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!Game.isVisible()) {
            g.drawImage(Picture.BACKGROUND.getImage(), 0, 0, getWidth(), getHeight(), null);
        }
    }

    private void update(boolean visible) {
        wrapperPanel.setVisible(!visible);
        optionPanel.setVisible(!visible);
        menuPanel.setVisible(visible);
        rulerPanel.setVisible(visible);
        numPanel.setVisible(visible);
        recordPanel.setVisible(visible);
        clockPanel.setVisible(visible);
    }

    public void run() {
        new Timer(100, _ -> {
            if (Game.isVisible()) {
                update(true);
                if (Game.isRestarting()) {
                    reset();
                    boardPanel.setBoard();
                    boardPanel.repaint();
                    Game.start();
                    Game.stopRestarting();
                }
                boardPanel.update();
            } else {
                update(false);
                reset();
                this.numPanel.paintNow();
            }
        }).start();
    }

    private void reset() {
        MoveGenerator.cancel();
        RecordPanel.clear();
        RulerPanel.reset();
        Clock.white.reset();
        Clock.black.reset();
        boardPanel.resetPromotions();
        clockPanel.update();
        waite = true;
    }

    public void runClock() {
        new Timer(1000, _ -> {
            if (MoveGenerator.isOperating()) {
                Game.updateTimer();
                System.out.println("    AI thinking for: " + Game.getTimer() +" seconds.");
            }
            if (Game.getTimer() == 60) {
                System.out.println("\nstarting abortion...\n");
                MoveGenerator.abort();
            }
            if (Game.isVisible() && !Game.isFinished()) {
                if (waite) {
                    waite = false;
                    return;
                }
                if (this.boardPanel.isWhiteTurn()) {
                    Clock.white.move();
                } else {
                    Clock.black.move();
                }
                clockPanel.update();
                if (Clock.white.isGameOver() || Clock.black.isGameOver()) {
                    Game.end(false);
                }
            }
        }).start();
    }
}