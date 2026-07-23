package App;

import File.Picture;

import javax.swing.*;
import java.awt.*;

import static Data.Game.isEnemyStarting;

public class PromotionsPanel extends JPanel {
    private final PromotePanel[] promotePanels;
    private int col, row;

    public PromotionsPanel(int width, int height) {
        setLayout(new GridLayout(2, 8));
        setPreferredSize(new Dimension(width, height));
        setOpaque(false);
        this.promotePanels = new PromotePanel[16];

        for (int i = 0; i < 8; i++) {
            this.promotePanels[i] = new PromotePanel(true);
            add(this.promotePanels[i]);
        }
        for (int i = 8; i < 16; i++) {
            this.promotePanels[i] = new PromotePanel(false);
            add(this.promotePanels[i]);
        }
        dispose();
    }

    public void dispose() {
        col = -1;
        row = -1;
        setVisible(false);
        for (PromotePanel promotePanel : promotePanels) {
            promotePanel.setVisible(false);
            promotePanel.resetValue();
            promotePanel.resetPressed();
        }
    }

    public void showButton(int square) {
        if (isVisible()) return;
        setVisible(true);

        int logicalCol = square % 8;
        int logicalRow = square / 8 > 0 ? 1 : 0;

        if (isEnemyStarting()) {
            this.col = 7 - logicalCol;
            this.row = 1 - logicalRow;
        } else {
            this.col = logicalCol;
            this.row = logicalRow;
        }

        int index = this.col + this.row * 8;
        promotePanels[index].setVisible(true);
    }

    public int getValue() {
        if (col == -1 || row == -1) return -1;
        return promotePanels[col + row * 8].getValue();
    }

    public boolean isPressed() {
        if (col == -1 || row == -1) return false;
        return promotePanels[col + row * 8].isPressed();
    }

    private static class PromotePanel extends JPanel {
        private boolean pressed;
        private int value;

        public PromotePanel(boolean isTopRow) {
            setLayout(new GridLayout(4, 1));
            resetValue();

            byte[] pieces = new byte[]{5, 2, 4, 3, 11, 12, 10, 13};

            for (int i = 0; i < 4; i++) {
                final int buttonPos = i;
                CustomButton customButton = new CustomButton(null, 1) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        boolean isWhitePiece = isTopRow != isEnemyStarting();
                        int baseIndex = isWhitePiece ? 0 : 4;

                        int pieceIndex = isEnemyStarting() ? (3 - buttonPos) : buttonPos;
                        int pieceValue = pieces[baseIndex + pieceIndex];

                        g.setColor(new Color(205, 143, 33));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.drawImage(Picture.getImage(pieceValue), 0, 0, getWidth(), getHeight(), null);
                    }
                };

                customButton.addActionListener(e -> {
                    pressed = true;
                    boolean isWhitePiece = isTopRow != isEnemyStarting();
                    int baseIndex = isWhitePiece ? 0 : 4;
                    int pieceIndex = isEnemyStarting() ? (3 - buttonPos) : buttonPos;
                    value = pieces[baseIndex + pieceIndex];
                });

                add(customButton);
            }
        }

        public boolean isPressed() {
            return pressed;
        }

        public int getValue() {
            return value;
        }

        public void resetValue() {
            this.value = -1;
        }

        public void resetPressed() {
            this.pressed = false;
        }
    }
}