package App;

import Data.Game;
import stockfish.EngineAnalysis;

import javax.swing.*;
import java.awt.*;

public class Ruler extends JPanel {
    private static JProgressBar progressBar;
    private static JLabel topLabel;
    private static JLabel bottomLabel;

    public Ruler(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(78, 42, 26).darker());
        setBorder(BorderFactory.createLineBorder(new Color(210, 180, 140), 2));
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        JLabel label = new JLabel("stockfish");
        label.setFont(new Font("Serif", Font.PLAIN, 12));
        label.setForeground(new Color(208, 208, 208));
        label.setBackground(Color.black);
        label.setBorder(BorderFactory.createLineBorder(new Color(205, 143, 33).darker(), 2));
        label.setOpaque(true);
        add(label);

        progressBar = new JProgressBar(JProgressBar.VERTICAL, 0, 1000);
        progressBar.setValue(500);
        progressBar.setPreferredSize(new Dimension(width - 20, height - 50));
        progressBar.setLayout(new BorderLayout());
        progressBar.setBorderPainted(false);
        topLabel = new JLabel("0.50", SwingConstants.CENTER);
        topLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        topLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        progressBar.add(topLabel, BorderLayout.NORTH);

        bottomLabel = new JLabel("0.50", SwingConstants.CENTER);
        bottomLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        bottomLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        progressBar.add(bottomLabel, BorderLayout.SOUTH);
        add(progressBar);
    }

    public static void setEvaluation(EngineAnalysis engineAnalysis, boolean whiteTurn) {
        int cp = engineAnalysis.getCentipawns();
        int mateIn = engineAnalysis.getMateIn();
        boolean isMate = engineAnalysis.isMate();
        boolean isCheckmate = engineAnalysis.isCheckmate();
        boolean isStalemate = engineAnalysis.isStalemate();
        if (!whiteTurn) {
            cp = -cp;
            mateIn = -mateIn;
        }
        int percentage;
        String labelText;
        boolean whiteAdvantage;
        if (isCheckmate || isMate) {
            if (isCheckmate) {
                whiteAdvantage = !whiteTurn;
                labelText = "M0";
            } else {
                whiteAdvantage = mateIn > 0;
                labelText = "M" + Math.abs(mateIn);
            }
            percentage = whiteAdvantage ? 1000 : 0;
        }
        else if (isStalemate) {
            whiteAdvantage = true;
            percentage = 500;
            labelText = "0.00";
        }
        else {
            double winChance = 1.0 / (1.0 + Math.exp(-0.00368208 * cp));
            int rawPercentage = (int) Math.round(winChance * 1000);
            percentage = Math.clamp(rawPercentage, 10, 990);
            double pawns = cp / 100.0;
            pawns = Math.clamp(pawns, -50.0, 50.0);
            whiteAdvantage = cp >= 0;
            labelText = (cp >= 0) ? String.format("+%.2f", pawns) : String.format("%.2f", pawns);
        }
        boolean showAtBottom = Game.isEnemyStarting() != whiteAdvantage;

        if (showAtBottom) {
            bottomLabel.setText(labelText);
            topLabel.setText("");
        } else {
            topLabel.setText(labelText);
            bottomLabel.setText("");
        }

        int visualPercentage = Game.isEnemyStarting()  ? (progressBar.getMaximum() - percentage) : percentage;
        progressBar.setValue(visualPercentage);
    }

    public static void reset() {
        if (Game.isEnemyStarting()) {
            progressBar.setBackground(Color.white);
            progressBar.setForeground(Color.black);
            topLabel.setForeground(Color.black);
            bottomLabel.setForeground(Color.white);
        } else {
            progressBar.setBackground(Color.black);
            progressBar.setForeground(Color.white);
            topLabel.setForeground(Color.white);
            bottomLabel.setForeground(Color.black);
        }
        progressBar.setValue(500);
        topLabel.setText("0.50");
        bottomLabel.setText("0.50");
    }
}