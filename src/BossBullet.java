import java.awt.*;

public class BossBullet {
    private double x, y;
    private double vx, vy;
    private int damage;
    private int radius;
    private Color color;
    private boolean isHoming;
    private int redirectCount = 0;
    private int maxRedirects = 1;
    private static final double HOMING_SPEED = 7.0; // เร็วขึ้น จาก 4.0
    private static final double TURN_RATE = 0.03; // หันได้น้อยลง จาก 0.08 (missile-like)
    private long creationTime; // เวลาที่สร้าง
    private static final long LIFETIME_MS = 60000; // 1 นาที
    
    // Regular bullet (barrage phase)
    public BossBullet(double x, double y, double targetX, double targetY, int damage, Color color) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.radius = 6;
        this.color = color;
        this.isHoming = false;
        this.creationTime = System.currentTimeMillis();
        
        // Calculate direction
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            this.vx = (dx / dist) * 5.0;
            this.vy = (dy / dist) * 5.0;
        }
    }
    
    // Homing bullet (phase 3)
    public BossBullet(double x, double y, int damage, Color color, boolean isHoming, int maxRedirects) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.radius = 8;
        this.color = color;
        this.isHoming = isHoming;
        this.maxRedirects = maxRedirects;
        this.vx = 0;
        this.vy = 0;
        this.creationTime = System.currentTimeMillis();
    }
    
    public void update(Player player) {
        if (isHoming && redirectCount <= maxRedirects) {
            // Calculate direction to player
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist > 0) {
                double targetVx = (dx / dist) * HOMING_SPEED;
                double targetVy = (dy / dist) * HOMING_SPEED;
                
                // Smoothly turn toward target
                vx += (targetVx - vx) * TURN_RATE;
                vy += (targetVy - vy) * TURN_RATE;
                
                // Normalize speed
                double speed = Math.sqrt(vx * vx + vy * vy);
                if (speed > HOMING_SPEED) {
                    vx = (vx / speed) * HOMING_SPEED;
                    vy = (vy / speed) * HOMING_SPEED;
                }
                
                // Check if bullet passed player
                double currentDist = dist;
                double nextX = x + vx;
                double nextY = y + vy;
                double nextDist = Math.sqrt((player.getX() - nextX) * (player.getX() - nextX) + 
                                           (player.getY() - nextY) * (player.getY() - nextY));
                
                if (nextDist > currentDist && speed > 1) {
                    // Bullet passed player, increment redirect count
                    redirectCount++;
                    if (redirectCount > maxRedirects) {
                        // No more redirects, keep current velocity
                        isHoming = false;
                    }
                }
            }
        }
        
        x += vx;
        y += vy;
    }
    
    public void draw(Graphics2D g2d) {
        // ทุกกระสุน (ทั้งติดตามและธรรมดา) ใช้รูปสี่เหลี่ยม
        double bulletAngle = Math.atan2(vy, vx);
        int width = 12;
        int height = 4;
        
        g2d.setColor(color);
        g2d.translate((int)x, (int)y);
        g2d.rotate(bulletAngle);
        g2d.fillRect(-width/2, -height/2, width, height);
        g2d.rotate(-bulletAngle);
        g2d.translate(-(int)x, -(int)y);
        
        // เพิ่ม glow สำหรับกระสุนติดตามที่ยังใช้งานอยู่
        if (isHoming && redirectCount <= maxRedirects) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int)(x - radius * 1.5), (int)(y - radius * 1.5), 
                        (int)(radius * 3), (int)(radius * 3));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    public boolean collidesWith(Player player) {
        double dx = x - player.getX();
        double dy = y - player.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        return dist < (radius + player.getHitboxRadius());
    }
    
    public boolean isOffScreen(int worldWidth, int worldHeight) {
        // Check lifetime (1 minute)
        if (System.currentTimeMillis() - creationTime > LIFETIME_MS) {
            return true;
        }
        return x < -50 || x > worldWidth + 50 || y < -50 || y > worldHeight + 50;
    }
    
    public int getDamage() { return damage; }
    public double getX() { return x; }
    public double getY() { return y; }
}
