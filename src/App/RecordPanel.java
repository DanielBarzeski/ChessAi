package App;


import Engine.Position;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;

import static Data.Game.CELL_SIZE;


public class RecordPanel extends JPanel {
    private static final ArrayList<Integer> moves = new ArrayList<>();
    private static final JPanel panel = new JPanel();
    private static Component glue;

    public RecordPanel(int width, int height) {
        setBackground(Color.black);
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 1));
        setPreferredSize(new Dimension(width, height));
        setBorder(new LineBorder(new Color(210, 180, 140), 2));

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(78, 42, 26).darker());
        glue = Box.createVerticalGlue();
        panel.add(glue);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected Dimension getMinimumThumbSize() {
                return new Dimension(0, 60);
            }

            @Override
            protected Dimension getMaximumThumbSize() {
                return new Dimension(Integer.MAX_VALUE, 60);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(new Color(210, 180, 140));
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                g.setColor(Color.darkGray);
                g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
                g.setColor(new Color(205, 143, 33));
                g.fillRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2);
            }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scrollPane.getVerticalScrollBar().setBorder(new LineBorder(Color.white, 2));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(width - 8, height - 6));
        scrollPane.setBorder(new MatteBorder(1, 0, 1, 0, Color.black));

        add(scrollPane);
    }

    public static boolean isEmpty() {
        return moves.isEmpty();
    }

    public static Integer getLastMove() {
        return moves.getLast();
    }

    public static void addMove(Position position, int move) {
        moves.add(move);
        if (glue != null && glue.getParent() == panel) {
            panel.remove(glue);
        }
        String sanText = moveToSAN(position, move);
        JLabel label = new JLabel(sanText);
        label.setForeground(position.isWhiteTurn() ? Color.black : Color.white);
        label.setBorder(new MatteBorder(moves.size() == 1 ? 3 : 0, 3, 3, 3, panel.getBackground()));
        label.setOpaque(true);
        label.setBackground(new Color(205, 143, 33).darker());
        label.setFont(new Font("Serif", Font.BOLD, (int) (CELL_SIZE / 4.5)));
        label.setMaximumSize(new Dimension(300, CELL_SIZE / 3));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(label);
        panel.add(glue);
        panel.revalidate();
        SwingUtilities.invokeLater(() -> label.scrollRectToVisible(new Rectangle(label.getBounds())));
    }

    private static String moveToSAN(Position pos, int move) {
        int movingPiece = (move >> 12) & 0xF;
        int from = move & 0x3F;
        int to = (move >> 6) & 0x3F;
        int captured = (move >> 16) & 0xF;
        int promotion = (move >> 20) & 0xF;

        if ((movingPiece == Position.WK || movingPiece == Position.BK) && Math.abs(from - to) == 2) {
            return (to % 8 > from % 8) ? "O-O" : "O-O-O";
        }

        StringBuilder san = new StringBuilder();

        String pieceSymbol = getPieceSymbol(movingPiece);
        if (!pieceSymbol.isEmpty()) {
            san.append(pieceSymbol);
        }

        if (captured != 0) {
            if (movingPiece == Position.WP || movingPiece == Position.BP) {
                char sourceFile = (char) ('a' + (from % 8));
                san.append(sourceFile);
            }
            san.append('x');
        }

        char targetFile = (char) ('a' + (to % 8));
        int targetRank = 8 - (to / 8);
        san.append(targetFile).append(targetRank);

        if (promotion != 0) {
            san.append("=").append(getPieceSymbol(promotion));
        }

        if (pos.isMate()) {
            san.append("#");
        } else if (pos.isInCheck()) {
            san.append("+");
        }

        return san.toString();
    }

    private static String getPieceSymbol(int piece) {
        // ♚ ♛ ♜ ♝ ♞  ♔ ♕ ♖ ♗ ♘
        return switch (piece) {
            case Position.WN, Position.BN -> "♞";
            case Position.WB, Position.BB -> "♝";
            case Position.WR, Position.BR -> "♜";
            case Position.WQ, Position.BQ -> "♛";
            case Position.WK, Position.BK -> "♚";
            default -> "";
        };
    }

    public static void removeLastMove() {
        moves.removeLast();
        if (glue != null) {
            panel.remove(glue);
        }
        int componentCount = panel.getComponentCount();
        if (componentCount > 0) {
            panel.remove(componentCount - 1);
        }
        panel.add(glue);
        panel.revalidate();
        panel.repaint();
        SwingUtilities.invokeLater(() -> {
            int newCount = panel.getComponentCount();
            if (newCount > 1) {
                JLabel lastLabel = (JLabel) panel.getComponent(newCount - 2);
                lastLabel.scrollRectToVisible(new Rectangle(lastLabel.getBounds()));
            }
        });
    }


    public static void clear() {
        panel.removeAll();
        moves.clear();

        glue = Box.createVerticalGlue();
        panel.add(glue);

        panel.revalidate();
        panel.repaint();
    }
}