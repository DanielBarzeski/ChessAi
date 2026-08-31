package App;


import Data.Game;

import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {

    public MenuPanel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(78, 42, 26).darker());
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210, 180, 140), 2),
                BorderFactory.createLineBorder(Color.black, 6)));
        setLayout(new FlowLayout(FlowLayout.CENTER, width / 20, height / 8));


        CustomButton back = new CustomButton("back", 1);
        back.setPreferredSize(new Dimension(width / 7, height / 2));

        CustomButton hint = new CustomButton("hint", 1);
        hint.setPreferredSize(new Dimension(width / 7, height / 2));

        CustomButton restart = new CustomButton("restart", 1);
        restart.setPreferredSize(new Dimension(width / 7, height / 2));

        CustomButton undo = new CustomButton("undo", 1);
        undo.setPreferredSize(new Dimension(width / 7, height / 2));

        CustomButton surrender = new CustomButton("surrender", 1);
        surrender.setPreferredSize(new Dimension(width / 7, height / 2));


        back.addActionListener(_ -> {
            if (Game.isMenuActive()) {
                Game.end(false);
                Game.setIfVisible(false);
            }
        });
        hint.addActionListener(_ -> {
            if (Game.isMenuActive()) {
                Game.setHint(true);
            }
        });
        restart.addActionListener(_ -> {
            if (Game.isMenuActive()) {
                Game.restart();
            }
        });
        undo.addActionListener(_ -> {
            if (Game.isMenuActive()) {
                Game.setUndoing(true);
            }
        });
        surrender.addActionListener(_ -> {
            if (Game.isMenuActive()) {
                Game.end(false);
            }
        });

        add(back);
        add(hint);
        add(restart);
        add(undo);
        add(surrender);
    }

}

