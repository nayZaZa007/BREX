package interfaces;

import java.awt.Graphics2D;

/**
 * Interface for all objects that can be rendered to the screen
 */
public interface Drawable {
    /**
     * Draw this object to the graphics context
     * @param g2d Graphics2D context for rendering
     */
    void draw(Graphics2D g2d);
}
