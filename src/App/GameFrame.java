package App;


import File.Picture;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("CHESS GAME");
        getContentPane().setBackground(Color.black);
        setIconImage(Picture.PAWN.getImage());
        setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel gamePanel = new GamePanel();
        gamePanel.run();
        gamePanel.runClock();
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

