package App;


import Data.Game;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static Data.Game.setEnemyStarting;


public class OptionPanel extends JPanel {

    public OptionPanel(int width, int height) {
        setLayout(new GridLayout(0, 1, 0, 10));
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        CustomButton aiButton = new CustomButton("play against computer", 1);
        aiButton.setFont(new Font("Serif", Font.BOLD, 19));
        aiButton.addActionListener(_ -> {
            Game.setAiExistence(true);
            Game.restart();
            Game.setIfVisible(true);
        });
        CustomButton friendButton = new CustomButton("play against friend", 1);
        friendButton.setFont(new Font("Serif", Font.BOLD, 19));
        friendButton.addActionListener(_ -> {
            Game.setAiExistence(false);
            Game.restart();
            Game.setIfVisible(true);
        });

        add(aiButton);
        add(friendButton);
        add(new PlayerSideSelector());
        add(new TimerSetterPanel(true));
        add(new TimerSetterPanel(false));
    }

    private static class PlayerSideSelector extends CustomButton {
        private final CustomButton white, black;

        public PlayerSideSelector() {
            super(null, 4);
            setLayout(new FlowLayout(FlowLayout.CENTER, 10, 8));

            white = new CustomButton("white", 1);
            black = new CustomButton("black", 1);
            white.setType(2);
            black.setType(2);
            this.white.setType(3);
            white.setPreferredSize(new Dimension(80, 32));
            white.setFont(new Font("Serif", Font.BOLD, 18));
            black.setPreferredSize(new Dimension(80, 32));
            black.setFont(new Font("Serif", Font.BOLD, 18));

            this.white.addActionListener(e -> {
                this.white.setType(3);
                this.black.setType(2);
                setEnemyStarting(false);
            });
            this.black.addActionListener(e -> {
                this.black.setType(3);
                this.white.setType(2);
                setEnemyStarting(true);
            });
            JLabel label = new JLabel("play as: ");
            label.setForeground(getForeground());
            label.setFont(new Font("Serif", Font.BOLD, 19));
            add(label);
            add(white);
            add(black);
        }
    }

    public static class TimerSetterPanel extends CustomButton {
        TimerArrowsPanel hours, minutes;

        public TimerSetterPanel(boolean white) {
            super(null, 4);
            setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

            hours = new TimerArrowsPanel(new String[]{" 00", " 01", " 02", " 03"});
            minutes = new TimerArrowsPanel(new String[]{
                    " 00", " 05", " 10", " 15", " 20", " 25", " 30", " 35", " 40", " 45", " 50", " 55"
            });

            this.hours.addActionListener(white ? Clock.white : Clock.black, true);
            // to set the time:
            //   this.hours.upButton.doClick();
            //
            this.minutes.addActionListener(white ? Clock.white : Clock.black, false);

            JLabel label = new JLabel();
            label.setForeground(getForeground());
            label.setText(white ? "white timer : " : "black timer : ");
            label.setFont(new Font("Serif", Font.BOLD, 19));

            add(label);
            add(hours);
            add(minutes);
        }

        private static class TimerArrowsPanel extends JPanel {
            private final String[] items;
            private int index = 0;
            private final JLabel display;
            private final CustomButton upButton, downButton;

            public TimerArrowsPanel(String[] items) {
                this.items = items.clone();
                setPreferredSize(new Dimension(53, 30));
                setLayout(new BorderLayout(0, 1));
                setBackground(Color.white);
                setOpaque(false);
                display = new JLabel(items[0]);
                Border b = BorderFactory.createLineBorder(new Color(143, 100, 23), 1);
                display.setBorder(b);
                display.setFont(new Font("Arial", Font.PLAIN, 14));
                display.setForeground(new Color(143, 100, 23));
                display.setBackground(Color.black);
                display.setOpaque(true);
                add(display, BorderLayout.CENTER);

                JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 0, 0));
                buttonsPanel.setOpaque(false);

                upButton = new CustomButton("  ⮝  ", 2);
                downButton = new CustomButton("  ⮟  ", 2);

                upButton.setFont(new Font("Segue", Font.BOLD, 13));
                downButton.setFont(new Font("Segue", Font.BOLD, 13));

                upButton.setBorder(b);
                downButton.setBorder(b);

                buttonsPanel.add(upButton);
                buttonsPanel.add(downButton);

                add(buttonsPanel, BorderLayout.EAST);
            }

            public void addActionListener(Clock clock, boolean isHours) {
                upButton.addActionListener(e -> {
                    if (index < items.length - 1) {
                        index = index + 1;
                        updateDisplay();
                        if (isHours) {
                            clock.setHour(getValue());
                        } else {
                            clock.setMinute(getValue());
                        }
                    }
                });
                downButton.addActionListener(e -> {
                    if (index > 0) {
                        index = index - 1;
                        updateDisplay();
                        if (isHours) {
                            clock.setHour(getValue());
                        } else {
                            clock.setMinute(getValue());
                        }
                    }
                });
            }

            private void updateDisplay() {
                display.setText(items[index]);
            }

            private int getValue() {
                return Integer.parseInt(items[index].substring(1));
            }
        }
    }
}
