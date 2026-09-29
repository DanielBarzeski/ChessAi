package App;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class CustomButton extends JButton {
    private JLabel label;
    private int type;

    public CustomButton(String text, int type) {
        super();
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Color color = new Color(143, 100, 23);
        if (text != null) {
            setLayout(new GridBagLayout());
            label = new JLabel(text);
            label.setForeground(Color.white);
            label.setFont(new Font("Serif", Font.BOLD, label.getFont().getSize() + 4));
            add(label);
        }

        setFocusable(false);
        setForeground(Color.white);
        setBackground(color.darker());
        setContentAreaFilled(false);
        setOpaque(false);
        this.type = type;
        Color border = new Color(205, 143, 33).darker();
        if (type == 1) {
            getModel().addChangeListener(_ -> {
                ButtonModel model = getModel();
                if (model.isPressed()) {
                    setBorder(new LineBorder(border, 3));
                } else {
                    setBorder(new LineBorder(border, 2));
                }
            });
        }

        setBorder(new LineBorder(border, 2));
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (label != null) label.setFont(font);
    }

    public void setType(int type) {
        this.type = type;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(type == 3 ? new Color(80, 80, 80) : Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (getModel().isPressed()) {
            g.setColor(new Color(80, 80, 80));
            g.fillRect(0, 0, getWidth(), getHeight());
        } else if (getModel().isRollover()) {
            if (type == 1 || type == 2) {
                g.setColor(new Color(55, 55, 55));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
}

