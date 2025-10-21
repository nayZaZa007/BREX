package entities;

import interfaces.Collidable;
import interfaces.Drawable;
import interfaces.Updatable;
import java.awt.Graphics2D;

/**
 * Base class for all game entities
 * Provides common functionality for position, size, and basic collision
 */
public abstract class GameObject implements Drawable, Updatable, Collidable {
    protected double x, y;
    protected int width, height;
    protected int collisionRadius;
    
    public GameObject(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.collisionRadius = Math.min(width, height) / 2;
    }
    
    @Override
    public int getCenterX() {
        return (int)(x + width / 2.0);
    }
    
    @Override
    public int getCenterY() {
        return (int)(y + height / 2.0);
    }
    
    @Override
    public int getCollisionRadius() {
        return collisionRadius;
    }
    
    @Override
    public boolean collidesWith(Collidable other) {
        int dx = getCenterX() - other.getCenterX();
        int dy = getCenterY() - other.getCenterY();
        int radiusSum = getCollisionRadius() + other.getCollisionRadius();
        return dx*dx + dy*dy <= radiusSum * radiusSum;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    // Abstract methods to be implemented by subclasses
    @Override
    public abstract void draw(Graphics2D g2d);
    
    @Override
    public abstract void update(long deltaMs);
}
