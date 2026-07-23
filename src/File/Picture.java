package File;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static Data.Game.CELL_SIZE;


public enum Picture {
    NOTHING1(null),
    WHITE_PAWN("wP"),
    WHITE_KNIGHT("wN"),
    WHITE_BISHOP("wB"),
    WHITE_ROOK("wR"),
    WHITE_QUEEN("wQ"),
    WHITE_KING("wK"),
    NOTHING2(null),
    NOTHING3(null),
    BLACK_PAWN("bP"),
    BLACK_KNIGHT("bN"),
    BLACK_BISHOP("bB"),
    BLACK_ROOK("bR"),
    BLACK_QUEEN("bQ"),
    BLACK_KING("bK"),
    NUMBERS("numbers"),
    NUMBERS_REVERSED("numbersReversed"),
    BACKGROUND("background"),
    BOARD("board"),
    PAWN("pawnIcon");

    private final BufferedImage image;

    Picture(String filename) {
        this.image = loadImage(filename);
    }

    private BufferedImage loadImage(String name) {
        if (name == null) {
            return new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_RGB);
        }
        try {
            if (name.length() == 2) {
                BufferedImage original = ImageIO.read(new File("assets/images/pieces/" + name + ".png"));
                BufferedImage scaled = new BufferedImage(CELL_SIZE * 10, CELL_SIZE * 10, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g2 = scaled.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(original, 0, 0, CELL_SIZE * 10, CELL_SIZE * 10, null);
                g2.dispose();

                return scaled;
            } else {
                return ImageIO.read(new File("assets/images/graphics/" + name + ".png"));
            }
        } catch (IOException e) {
            System.out.println("In File package in Picture enum class " + e.getMessage());
            return new BufferedImage(CELL_SIZE, CELL_SIZE, BufferedImage.TYPE_INT_RGB);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public static BufferedImage getImage(int index) {
        return Picture.values()[index].getImage();
    }
}

