package App;

import javax.swing.*;
import java.awt.*;

import static Data.Game.CELL_SIZE;


public class ClockPanel extends JPanel {
    JLabel[] labels;

    public ClockPanel(int width, int height) {
        setLayout(new GridLayout(2, 2, 2, 2));
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(245, 153, 43).darker().darker());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 180, 140), 2),
                BorderFactory.createEmptyBorder(4, 4, 4, 4))
        );
        labels = new JLabel[]{
                new JLabel("white player"),
                new JLabel("black player"),
                new JLabel("remaining time : 00:00:00"),
                new JLabel("remaining time : 00:00:00")
        };
        for (JLabel label : labels) {
            label.setFont(new Font("Serif", Font.BOLD, (int) (CELL_SIZE / 3.2)));
            label.setForeground(new Color(208, 208, 208));
            label.setBackground(Color.black);
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            add(label);
        }
    }

    public void update() {
        labels[2].setText("remaining time: " + Clock.white);
        labels[3].setText("remaining time: " + Clock.black);
    }
}
