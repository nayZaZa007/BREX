package interfaces;

/**
 * Interface for all objects that can collide with others
 */
public interface Collidable {
    /**
     * Get the X coordinate of the center of this object
     */
    int getCenterX();
    
    /**
     * Get the Y coordinate of the center of this object
     */
    int getCenterY();
    
    /**
     * Get the collision radius for circular collision detection
     */
    int getCollisionRadius();
    
    /**
     * Check if this object collides with another collidable object
     * @param other The other collidable object
     * @return true if collision detected
     */
    boolean collidesWith(Collidable other);
}
