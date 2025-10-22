import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Boss {
    private double x, y;
    private int width, height;
    private int health;
    private int maxHealth;
    private double speed;
    private BufferedImage sprite;
    private int hitboxRadius;
    private long lastShotTime = 0;
    private int fireRate = 2000; // 2 seconds between shots
    private boolean isDead = false;
    
    // Movement pattern
    private double moveTimer = 0;
    private double angle = 0;
    
    public Boss(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 120;
        this.height = 120;
        this.maxHealth = 1000;
        this.health = maxHealth;
        this.speed = 2.0;
        
        // Circular hitbox
        int minDim = Math.min(this.width, this.height);
        this.hitboxRadius = (int)(minDim * 0.8) / 2;
        
        loadSprite("boss.png", "Boss.png");
    }
    
    private void loadSprite(String... possibleNames) {
        String[] searchPaths = {"src/Pic/", "Pic/", "bin/Pic/"};
        for (String name : possibleNames) {
            for (String basePath : searchPaths) {
                try {
                    File imgFile = new File(basePath + name);
                    if (imgFile.exists()) {
                        sprite = ImageIO.read(imgFile);
                        System.out.println("Boss sprite loaded from: " + basePath + name);
                        return;
                    }
                } catch (IOException e) {
                    // Continue searching
                }
            }
        }
        System.out.println("Boss sprite not found, using colored rectangle");
    }
    
    public void update(Player player, long deltaMs) {
        // Boss movement pattern: circular motion around spawn point
        moveTimer += deltaMs / 1000.0;
        angle = moveTimer * 0.5; // slow rotation
        
        // Move in a circle or figure-8 pattern
        double centerX = x;
        double centerY = y;
        
        // Simple circular pattern
        // x and y oscillate
    }
    
    public void draw(Graphics2D g2d) {
        if (sprite != null) {
            AffineTransform old = g2d.getTransform();
            g2d.translate((int)x, (int)y);
            g2d.drawImage(sprite, -width/2, -height/2, width, height, null);
            g2d.setTransform(old);
        } else {
            g2d.setColor(Color.MAGENTA);
            g2d.fillRect((int)x - width/2, (int)y - height/2, width, height);
        }
        
        // Draw health bar below boss
        drawHealthBar(g2d);
    }
    
    private void drawHealthBar(Graphics2D g2d) {
        int barWidth = 150;
        int barHeight = 10;
        int barX = (int)x - barWidth/2;
        int barY = (int)y + height/2 + 10;
        
        // Background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        // Health
        g2d.setColor(Color.RED);
        int healthWidth = (int)((double)health / (double)maxHealth * barWidth);
        g2d.fillRect(barX, barY, healthWidth, barHeight);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barWidth, barHeight);
        
        // Text
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String hpText = health + "/" + maxHealth;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)x - fm.stringWidth(hpText)/2;
        int textY = barY + barHeight + 15;
        g2d.drawString(hpText, textX, textY);
    }
    
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;
        }
    }
    
    public boolean collidesWith(Player player) {
        double dx = this.x - player.getX();
        double dy = this.y - player.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.hitboxRadius + player.getHitboxRadius());
    }
    
    public boolean collidesWith(Bullet bullet) {
        double dx = this.x - bullet.getX();
        double dy = this.y - bullet.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < this.hitboxRadius + 4; // bullet radius ~4
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long time) { this.lastShotTime = time; }
    public int getFireRate() { return fireRate; }
    public int getHitboxRadius() { return hitboxRadius; }
}
