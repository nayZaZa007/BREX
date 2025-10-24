import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

public class Bullet {
    private static Random rand = new Random();
    private double x, y;
    private double dx, dy;
    private int width, height;
    private double speed;
    private long creationTime;
    private static final long BULLET_LIFETIME = 60000; // 1 minute in milliseconds
    private int damage; // Random damage
    private Color color = Color.YELLOW; // Default color (can be changed)
    
    public Bullet(int startX, int startY, int targetX, int targetY) {
        this.x = startX;
        this.y = startY;
        this.width = 8;
        this.height = 8;
        this.speed = 10;
        this.creationTime = System.currentTimeMillis();
        this.damage = 8 + rand.nextInt(5); // 8-12 damage
        
        // Calculate direction towards target
        double distance = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));
        if (distance > 0) {
            this.dx = ((targetX - startX) / distance) * speed;
            this.dy = ((targetY - startY) / distance) * speed;
        }
    }

    // New constructor: fire from startX,startY at a given angle (radians)
    public Bullet(int startX, int startY, double angle, double speed) {
        this.x = startX;
        this.y = startY;
        this.width = 8;
        this.height = 8;
        this.speed = speed;
        this.creationTime = System.currentTimeMillis();
        this.damage = 8 + rand.nextInt(5); // 8-12 damage

        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }
    
    // Constructor with custom damage multiplier for manual mode
    public Bullet(int startX, int startY, double angle, double speed, double damageMultiplier) {
        this.x = startX;
        this.y = startY;
        this.width = 8;
        this.height = 8;
        this.speed = speed;
        this.creationTime = System.currentTimeMillis();
        // Apply damage multiplier
        int baseDamage = 8 + rand.nextInt(5); // 8-12 damage
        this.damage = (int) Math.round(baseDamage * damageMultiplier);

        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    
    public void update() {
        x += dx;
        y += dy;
    }
    
    public void draw(Graphics2D g2d) {
        // Draw thin rectangular bullet aligned with velocity
        double angle = Math.atan2(dy, dx);
        int len = Math.max(8, (int) Math.round(speed * 1.5));
        int h = 3; // thin height

        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(angle);

        g2d.setColor(color); // Use custom color
        g2d.fillRect(0, -h/2, len, h);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(0, -h/2, len, h);

        g2d.setTransform(old);
    }
    
    public boolean collidesWith(Enemy enemy) {
        return x >= enemy.getX() - enemy.getWidth()/2 &&
               x <= enemy.getX() + enemy.getWidth()/2 &&
               y >= enemy.getY() - enemy.getHeight()/2 &&
               y <= enemy.getY() + enemy.getHeight()/2;
    }
    
    public boolean collidesWith(Boss boss) {
        double dx = x - boss.getX();
        double dy = y - boss.getY();
        double dist = Math.sqrt(dx*dx + dy*dy);
        return dist < boss.getHitboxRadius() + 4; // bullet radius ~4
    }
    
    public boolean isOffScreen(int gameWidth, int gameHeight) {
        return x < 0 || x > gameWidth || y < 0 || y > gameHeight;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > BULLET_LIFETIME;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDamage() { return damage; }
    
    // Setter for color
    public void setColor(Color color) { this.color = color; }
}