import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Random;

public class EnemyBullet {
    private static Random rand = new Random();
    private double x, y;
    private double dx, dy;
    private int width, height;
    private double speed;
    private long creationTime;
    private static final long BULLET_LIFETIME = 60000; // 1 minute in milliseconds
    private int damage; // Random damage
    
    // For TYPE2 bullets: acceleration toward player
    private boolean hasAcceleration;
    private int targetPlayerX, targetPlayerY;
    private double acceleration = 0.05; // pixels per frame^2
    // Remember the initial aimed point; once bullet passes this point, stop homing
    private double aimedX = Double.NaN;
    private double aimedY = Double.NaN;
    private boolean passedAimedPoint = false;
    
    // Constructor for TYPE1/TYPE3: simple bullet toward player
    public EnemyBullet(int startX, int startY, int targetX, int targetY) {
        this.x = startX;
        this.y = startY;
        this.width = 6;
        this.height = 6;
        this.speed = 3; // ช้ากว่ากระสุนผู้เล่น
        this.creationTime = System.currentTimeMillis();
        this.hasAcceleration = false;
        this.damage = 3 + rand.nextInt(4); // 3-6 damage
        
        // Calculate direction towards player
        double distance = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));
        if (distance > 0) {
            this.dx = ((targetX - startX) / distance) * speed;
            this.dy = ((targetY - startY) / distance) * speed;
        }
    }
    
    // Constructor for TYPE2: bullet fired at angle with acceleration toward player
    public EnemyBullet(int startX, int startY, double initialAngle, int targetPlayerX, int targetPlayerY) {
        this.x = startX;
        this.y = startY;
        this.width = 6;
        this.height = 6;
        this.speed = 2; // start slower
        this.creationTime = System.currentTimeMillis();
        this.hasAcceleration = true;
        this.targetPlayerX = targetPlayerX;
        this.targetPlayerY = targetPlayerY;
        // set aimed point as the player's position at firing time
        this.aimedX = targetPlayerX;
        this.aimedY = targetPlayerY;
        this.damage = 4 + rand.nextInt(5); // 4-8 damage
        
        // Initial velocity from angle
        this.dx = Math.cos(initialAngle) * speed;
        this.dy = Math.sin(initialAngle) * speed;
    }
    
    public void update() {
        if (hasAcceleration) {
            if (!passedAimedPoint) {
                // Check whether we've passed the aimed point: compute vector from bullet to aimed point
                double toAimedX = aimedX - x;
                double toAimedY = aimedY - y;
                double velDot = toAimedX * dx + toAimedY * dy;
                // If velDot < 0, the velocity has a component away from the aimed point (we passed it)
                if (velDot < 0) {
                    passedAimedPoint = true;
                    // stop homing
                    hasAcceleration = false;
                }
            }
            if (hasAcceleration) {
            // Accelerate toward target player position
            double toPlayerX = targetPlayerX - x;
            double toPlayerY = targetPlayerY - y;
            double dist = Math.sqrt(toPlayerX*toPlayerX + toPlayerY*toPlayerY);
            if (dist > 0) {
                dx += (toPlayerX / dist) * acceleration;
                dy += (toPlayerY / dist) * acceleration;
            }
            }
        }
        x += dx;
        y += dy;
    }
    
    public void draw(Graphics2D g2d) {
        // Draw thin rectangular enemy bullet aligned with velocity
        double angle = Math.atan2(dy, dx);
        int len = Math.max(6, (int) Math.round(speed * 1.5));
        int h = 3;

        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(angle);

        g2d.setColor(Color.RED);
        g2d.fillRect(0, -h/2, len, h);
        g2d.setColor(Color.ORANGE);
        g2d.drawRect(0, -h/2, len, h);

        g2d.setTransform(old);
    }
    
    public boolean collidesWith(Player player) {
        // Treat bullet as point vs player's circular hitbox
        int circleX = player.getX();
        int circleY = player.getY();
        int radius = player.getHitboxRadius();

        double dx = x - circleX;
        double dy = y - circleY;
        return dx*dx + dy*dy <= radius * radius;
    }
    
    public boolean isOffScreen(int gameWidth, int gameHeight) {
        return x < -50 || x > gameWidth + 50 || y < -50 || y > gameHeight + 50;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > BULLET_LIFETIME;
    }
    
    public int getDamage() { return damage; }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}