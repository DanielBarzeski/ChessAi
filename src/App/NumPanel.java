package App;


import File.Picture;

import javax.swing.*;
import java.awt.*;

import static Data.Game.CELL_SIZE;
import static Data.Game.isEnemyStarting;


public class NumPanel extends JPanel {
    private boolean paint;

    public NumPanel(int width, int height, int hGap, int vGap) {
        setPreferredSize(new Dimension(width, height));
        setLayout(new FlowLayout(FlowLayout.CENTER, hGap - 2, vGap - 2));
        setBorder(BorderFactory.createLineBorder(new Color(210, 180, 140), 2));
        setBackground(new Color(108, 52, 26));
    }

    public void paintNow() {
        paint = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (paint || getGraphicsConfiguration().getBounds().contains(getBounds())) {
            g.setColor(Color.black);
            for (int row = 0; row < 36; row++) {
                for (int col = 0; col < 36; col++) {
                    if ((row + col) % 2 != 0) {
                        g.fillRect(col * CELL_SIZE / 2, row * CELL_SIZE / 2, CELL_SIZE / 2, CELL_SIZE / 2);
                    }
                }
            }
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(
                    isEnemyStarting() ? Picture.NUMBERS_REVERSED.getImage() : Picture.NUMBERS.getImage(),
                    0, -1, getWidth(), getHeight() - 1, null
            );
            g2d.dispose();
            paint = false;
        }

    }

}


