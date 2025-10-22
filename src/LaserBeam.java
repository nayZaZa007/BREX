import java.awt.*;
import java.awt.geom.AffineTransform;

public class LaserBeam {
    private double startX, startY; // Enemy position
    private double targetX, targetY; // Player position when charging started
    private double angle;
    
    // Laser states
    public enum LaserState {
        CHARGING,  // 0-2s: thin red line, getting thicker
        LOCKED,    // 2-2.7s: thick red line, locked on
        FIRING     // instant, draw bright beam + do damage
    }
    
    private LaserState state;
    private long stateStartTime;
    private double chargeProgress; // 0.0 to 1.0
    private int damage;
    
    // Cooldown tracking
    private boolean onCooldown = false;
    private long cooldownStartTime;
    private static final long COOLDOWN_DURATION = 4000; // 4 seconds
    
    // Timing
    private static final long CHARGE_TIME = 2000;  // 2 seconds
    private static final long LOCK_TIME = 700;     // 0.7 seconds
    
    public LaserBeam(double startX, double startY, double targetX, double targetY) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.angle = Math.atan2(targetY - startY, targetX - startX);
        this.state = LaserState.CHARGING;
        this.stateStartTime = System.currentTimeMillis();
        this.chargeProgress = 0.0;
        this.damage = 20 + (int)(Math.random() * 11); // 20-30
    }
    
    public void update(double enemyX, double enemyY) {
        this.startX = enemyX;
        this.startY = enemyY;
        
        long now = System.currentTimeMillis();
        long elapsed = now - stateStartTime;
        
        if (onCooldown) {
            // Just waiting
            return;
        }
        
        switch (state) {
            case CHARGING:
                chargeProgress = Math.min(1.0, (double)elapsed / CHARGE_TIME);
                if (elapsed >= CHARGE_TIME) {
                    state = LaserState.LOCKED;
                    stateStartTime = now;
                }
                break;
                
            case LOCKED:
                if (elapsed >= LOCK_TIME) {
                    state = LaserState.FIRING;
                    stateStartTime = now;
                }
                break;
                
            case FIRING:
                // Fire happens instantly, then go to cooldown
                onCooldown = true;
                cooldownStartTime = now;
                break;
        }
    }
    
    public void draw(Graphics2D g2d) {
        if (onCooldown) {
            // Don't draw anything during cooldown
            return;
        }
        
        // Calculate end point far away in the direction
        double length = 2000; // very long
        double endX = startX + Math.cos(angle) * length;
        double endY = startY + Math.sin(angle) * length;
        
        switch (state) {
            case CHARGING:
                // Thin line getting thicker
                int thickness = 1 + (int)(chargeProgress * 4); // 1 to 5
                g2d.setColor(new Color(255, 0, 0, 150));
                g2d.setStroke(new BasicStroke(thickness));
                g2d.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
                break;
                
            case LOCKED:
                // Thick red line
                g2d.setColor(new Color(255, 0, 0, 200));
                g2d.setStroke(new BasicStroke(5));
                g2d.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
                break;
                
            case FIRING:
                // Bright thick beam
                g2d.setColor(new Color(255, 255, 100, 255));
                g2d.setStroke(new BasicStroke(8));
                g2d.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
                
                // Glow
                g2d.setColor(new Color(255, 200, 0, 100));
                g2d.setStroke(new BasicStroke(12));
                g2d.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
                break;
        }
        
        // Reset stroke
        g2d.setStroke(new BasicStroke(1));
    }
    
    public boolean shouldDealDamage() {
        return state == LaserState.FIRING;
    }
    
    public boolean hitsPlayer(Player player) {
        if (state != LaserState.FIRING) return false;
        
        // Check if player is on the laser line
        // Use point-to-line distance
        double px = player.getX();
        double py = player.getY();
        
        // Line from (startX, startY) in direction angle
        // Check perpendicular distance
        double dx = px - startX;
        double dy = py - startY;
        
        // Project onto laser direction
        double projection = dx * Math.cos(angle) + dy * Math.sin(angle);
        if (projection < 0) return false; // Behind the laser
        
        // Perpendicular distance
        double perpDist = Math.abs(dx * Math.sin(angle) - dy * Math.cos(angle));
        
        return perpDist < player.getHitboxRadius() + 5; // 5 = laser effective width
    }
    
    public int getDamage() {
        return damage;
    }
    
    public boolean isFinished() {
        if (!onCooldown) return false;
        return System.currentTimeMillis() - cooldownStartTime >= COOLDOWN_DURATION;
    }
    
    public boolean isOnCooldown() {
        return onCooldown;
    }
}
