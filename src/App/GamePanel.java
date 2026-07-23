package App;


import Data.Game;
import File.Picture;
import Logic.MoveGenerator;

import javax.swing.*;
import java.awt.*;

import static Data.Game.CELL_SIZE;


public class GamePanel extends JPanel {
    private final JPanel wrapperPanel;
    private final OptionPanel optionPanel;
    private final MenuPanel menuPanel;
    private final Ruler ruler;
    private final NumPanel numPanel;
    private final BoardPanel boardPanel;
    private final RecordPanel recordPanel;
    private final ClockPanel clockPanel;
    private boolean waite = true;
    public static int timeCounter = 0;

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

        ruler = new Ruler(4 * CELL_SIZE / 6 + 15, CELL_SIZE * 9);

        numPanel = new NumPanel(CELL_SIZE * 9, CELL_SIZE * 9, CELL_SIZE / 2, CELL_SIZE / 2);
        boardPanel = new BoardPanel(CELL_SIZE * 8, CELL_SIZE * 8);

        recordPanel = new RecordPanel(2 * CELL_SIZE - 15, CELL_SIZE * 9);

        clockPanel = new ClockPanel(35 * CELL_SIZE / 3, CELL_SIZE);

        add(wrapperPanel);
        add(menuPanel);
        add(ruler);
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
        ruler.setVisible(visible);
        numPanel.setVisible(visible);
        recordPanel.setVisible(visible);
        clockPanel.setVisible(visible);
    }

    public void run() {
        new Timer(400, e -> {
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
        timeCounter = 0;
        MoveGenerator.cancel();
        RecordPanel.clear();
        Ruler.reset();
        Clock.white.reset();
        Clock.black.reset();
        boardPanel.resetPromotions();
        clockPanel.update();
        waite = true;
    }

    public void runClock() {
        new Timer(1000, e -> {
            if (MoveGenerator.isOperating() && MoveGenerator.isNotCancelled()) {
                timeCounter++;
                System.out.println("    AI thinking for: " + timeCounter +" seconds.");
            }
//            if (timeCounter == 5) {
//                System.out.println();
//                System.out.println("starting small abortion...");
//                System.out.println();
//                MoveGenerator.smallAbort();
//            }
            if (timeCounter == 15) {
                System.out.println();
                System.out.println("starting abortion...");
                System.out.println();
                MoveGenerator.abort();
            }
            if (timeCounter == 25) {
                System.out.println();
                System.out.println("starting extreme abortion...");
                System.out.println();
                MoveGenerator.extremeAbort();
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
                    Game.end();
                }
            }
        }).start();
    }
}