package chess.graphics;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/**
 * Flyweight: the picture of one kind of piece, such as the white pawn, shared by every
 * piece of that kind. The picture is the intrinsic state and never changes. Where to
 * draw it is the extrinsic state, passed to draw() by the caller.
 *
 * Instances come only from PieceSprites. The PNG is read the first time the sprite is
 * drawn, so creating pieces without drawing them, as the unit tests in src/tests do,
 * never touches the disk. After that the image never changes.
 */
public final class PieceSprite {

    private final String resource;
    private BufferedImage image;

    PieceSprite(String resource) {
        this.resource = resource;
    }

    public void draw(Graphics g, int x, int y, int size) {
        g.drawImage(image(), x, y, size, size, null);
    }

    // Only reached from GamePanel.paintComponent, on the Swing event thread, so no locking.
    private BufferedImage image() {
        if (image == null) {
            URL url = PieceSprite.class.getResource(resource);
            if (url == null) {
                throw new IllegalStateException("Missing piece image " + resource);
            }
            try {
                image = ImageIO.read(url);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read piece image " + resource, e);
            }
        }
        return image;
    }
}
