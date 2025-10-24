import java.awt.*;
import java.awt.geom.Line2D;

public class BossLaser {
    private double x, y; // Boss position
    private double angle; // Current angle in radians
    private double length;
    private int damage;
    private Color color;
    private boolean isActive;
    private int bossHitboxRadius; // Boss hitbox radius to avoid drawing over boss sprite
    
    // Rotation state
    private boolean isRotating = false;
    private double rotationSpeed = 0; // radians per second
    private double warmupTimer = 0;
    private static final double WARMUP_DURATION = 2.0; // 2 seconds warmup
    
    public BossLaser(double x, double y, double angle, double length, int damage, Color color, int bossHitboxRadius) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.length = length;
        this.damage = damage;
        this.color = color;
        this.isActive = true;
        this.bossHitboxRadius = bossHitboxRadius;
    }
    
    public void startRotation(double rotationSpeed) {
        this.isRotating = true;
        this.rotationSpeed = rotationSpeed;
    }
    
    public void update(double dt) {
        if (!isActive) return;
        
        // Warmup phase
        if (warmupTimer < WARMUP_DURATION) {
            warmupTimer += dt;
            return; // Don't rotate during warmup
        }
        
        // Rotate if active
        if (isRotating) {
            angle += rotationSpeed * dt;
            // Normalize angle
            while (angle > Math.PI * 2) angle -= Math.PI * 2;
            while (angle < 0) angle += Math.PI * 2;
        }
    }
    
    public void updatePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void draw(Graphics2D g2d) {
        if (!isActive) return;
        
        // Start laser from edge of boss hitbox (not center)
        double startX = x + Math.cos(angle) * bossHitboxRadius;
        double startY = y + Math.sin(angle) * bossHitboxRadius;
        
        // Calculate end point (full map length from boss edge)
        double endX = startX + Math.cos(angle) * length;
        double endY = startY + Math.sin(angle) * length;
        
        // Draw laser beam
        float alpha = (warmupTimer < WARMUP_DURATION) ? 
                     (float)(warmupTimer / WARMUP_DURATION) * 0.7f : 0.7f;
        
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // Draw glow
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.3f));
        g2d.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setStroke(new BasicStroke(1));
    }
    
    public boolean collidesWith(Player player) {
        if (!isActive || warmupTimer < WARMUP_DURATION) return false;
        
        // Start from edge of boss hitbox
        double startX = x + Math.cos(angle) * bossHitboxRadius;
        double startY = y + Math.sin(angle) * bossHitboxRadius;
        
        // Calculate end point
        double endX = startX + Math.cos(angle) * length;
        double endY = startY + Math.sin(angle) * length;
        
        // Point-to-line segment distance
        double px = player.getX();
        double py = player.getY();
        
        double dx = endX - startX;
        double dy = endY - startY;
        double lengthSquared = dx * dx + dy * dy;
        
        if (lengthSquared == 0) return false;
        
        double t = Math.max(0, Math.min(1, ((px - startX) * dx + (py - startY) * dy) / lengthSquared));
        double closestX = startX + t * dx;
        double closestY = startY + t * dy;
        
        double distX = px - closestX;
        double distY = py - closestY;
        double distance = Math.sqrt(distX * distX + distY * distY);
        
        return distance < (player.getHitboxRadius() + 4); // 4 = half laser width
    }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public void deactivate() { this.isActive = false; }
    public double getAngle() { return angle; }
    public int getDamage() { return damage; }
    public boolean isWarmedUp() { return warmupTimer >= WARMUP_DURATION; }
}
