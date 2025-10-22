import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Enemy {
    public enum EnemyType {
        TYPE1,  // enemy1.png - common, shoots 1 bullet
        TYPE2,  // enemy2.png - rare, shoots 6 bullets in circle, 1.5x size
        TYPE3   // enemy3.png - common, shoots 1 bullet อยากจะร้องไห้
    }
    
    private EnemyType type;
    private double x, y;
    private int width, height;
    private int health;
    private int maxHealth; // For TYPE2 HP tracking
    private double speed;
    private Color color;
    private BufferedImage sprite;
    private int hitboxRadius; // circular hitbox
    private long lastShotTime = 0;
    private int fireRate; // ms between shots
    private boolean showHealthBar = false; // For TYPE2
    private long healthBarShowTime = 0; // When to hide health bar
    
    // TYPE1 laser
    private LaserBeam activeLaser = null;
    
    public Enemy(int x, int y, EnemyType type) {
        this.type = type;
        this.x = x;
        this.y = y;
        
        // Set properties based on type
        switch (type) {
            case TYPE1:
                this.width = 30;
                this.height = 30;
                this.health = 50;
                this.maxHealth = 50;
                this.speed = 1.5;
                this.color = Color.RED;
                this.fireRate = 3000; // 3 seconds (not used for laser, laser has own timing)
                loadSprite("enemy1.png", "enemy_1.png");
                break;
            case TYPE2:
                this.width = 45; // 1.5x of 30
                this.height = 45;
                this.health = 3; // 3 hits to kill
                this.maxHealth = 3;
                this.speed = 1.0;
                this.color = Color.MAGENTA;
                this.fireRate = 5000; // 5 seconds
                loadSprite("enemy2.png", "enemy_2.png");
                break;
            case TYPE3:
                this.width = 30;
                this.height = 30;
                this.health = 50;
                this.maxHealth = 50;
                this.speed = 1.5;
                this.color = Color.ORANGE;
                this.fireRate = 3000; // 3 seconds
                loadSprite("enemy3.png", "enemy_3.png");
                break;
        }
        
        // Circular hitbox = sprite size
        int minDim = Math.min(this.width, this.height);
        this.hitboxRadius = minDim / 2;
    }

    // New constructor: size relative to player dimensions
    public Enemy(int x, int y, EnemyType type, int playerWidth, int playerHeight) {
        this.type = type;
        this.x = x;
        this.y = y;

        // Set properties based on type, but use player dimensions as base
        switch (type) {
            case TYPE1:
                this.width = playerWidth;
                this.height = playerHeight;
                this.health = 50;
                this.maxHealth = 50;
                this.speed = 1.5;
                this.color = Color.RED;
                this.fireRate = 3000; // 3 seconds
                loadSprite("enemy1.png", "enemy_1.png");
                break;
            case TYPE2:
                this.width = playerWidth * 2; // 2x player as requested
                this.height = playerHeight * 2;
                this.health = 3; // 3 hits to kill
                this.maxHealth = 3;
                this.speed = 1.0;
                this.color = Color.MAGENTA;
                this.fireRate = 5000; // 5 seconds
                loadSprite("enemy2.png", "enemy_2.png");
                break;
            case TYPE3:
                this.width = playerWidth;
                this.height = playerHeight;
                this.health = 50;
                this.maxHealth = 50;
                this.speed = 1.5;
                this.color = Color.ORANGE;
                this.fireRate = 3000; // 3 seconds
                loadSprite("enemy3.png", "enemy_3.png");
                break;
        }

        int minDim = Math.min(this.width, this.height);
        this.hitboxRadius = minDim / 2;
    }
    
    private void loadSprite(String... names) {
        String[] basePaths = {
            "src/Pic/",
            "./src/Pic/",
            "Pic/",
            "./Pic/",
            "bin/Pic/"
        };
        
        for (String name : names) {
            // Try classpath first
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/Pic/" + name);
                if (is != null) {
                    sprite = ImageIO.read(is);
                    is.close();
                    return;
                }
            } catch (IOException ex) {
                // continue
            }
            
            // Try filesystem paths
            for (String base : basePaths) {
                try {
                    File f = new File(base + name);
                    if (f.exists()) {
                        sprite = ImageIO.read(f);
                        return;
                    }
                } catch (IOException ex) {
                    // continue
                }
            }
        }
        // If not found, sprite remains null and we'll draw placeholder
    }
    
    public void update(Player player) {
        // Move towards player
        int playerX = player.getX();
        int playerY = player.getY();
        
        double dx = playerX - (x + width/2);
        double dy = playerY - (y + height/2);
        double distance = Math.sqrt(dx*dx + dy*dy);
        
        if (distance > 0) {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
        
        // TYPE1: update laser if active
        if (type == EnemyType.TYPE1 && activeLaser != null) {
            activeLaser.update(getX(), getY());
            if (activeLaser.isFinished()) {
                activeLaser = null; // Remove finished laser
            }
        }
        
        // Hide health bar after 2 seconds
        if (showHealthBar && System.currentTimeMillis() - healthBarShowTime > 2000) {
            showHealthBar = false;
        }
    }
    
    public void draw(Graphics2D g2d) {
    int drawX = (int) x;
    int drawY = (int) y;
        
        // Draw sprite if available, otherwise placeholder
        if (sprite != null) {
            // TYPE1: rotate sprite to south (180 degrees) when charging laser
            if (type == EnemyType.TYPE1 && activeLaser != null) {
                java.awt.geom.AffineTransform old = g2d.getTransform();
                g2d.translate(drawX + width/2, drawY + height/2);
                g2d.rotate(Math.PI); // 180 degrees
                g2d.drawImage(sprite, -width/2, -height/2, width, height, null);
                g2d.setTransform(old);
            } else {
                g2d.drawImage(sprite, drawX, drawY, width, height, null);
            }
        } else {
            // Draw simple enemy placeholder
            g2d.setColor(color);
            g2d.fillRect(drawX, drawY, width, height);
            
            // Draw "E" for Enemy
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "E";
            int textX = drawX + (width - fm.stringWidth(text)) / 2;
            int textY = drawY + (height + fm.getHeight()) / 2 - 2;
            g2d.drawString(text, textX, textY);
        }
        
        // TYPE2: Draw health bar if visible
        if (type == EnemyType.TYPE2 && showHealthBar) {
            drawHealthBar(g2d);
        }
        
        // TYPE1: Draw laser if active
        if (type == EnemyType.TYPE1 && activeLaser != null) {
            activeLaser.draw(g2d);
        }
    }
    
    private void drawHealthBar(Graphics2D g2d) {
        int barW = 40;
        int barH = 5;
        int barX = (int)x + width/2 - barW/2;
        int barY = (int)y - 10;
        
        // Background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barX, barY, barW, barH);
        
        // Health
        g2d.setColor(Color.GREEN);
        int healthW = (int)((double)health / (double)maxHealth * barW);
        g2d.fillRect(barX, barY, healthW, barH);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barW, barH);
    }
    
    public boolean collidesWith(Player player) {
        // Circle-circle collision (enemy hitbox vs player hitbox)
        int cx = (int)(x + width/2);
        int cy = (int)(y + height/2);
        int playerCX = player.getX();
        int playerCY = player.getY();
        int playerRadius = player.getHitboxRadius();
        
        int dx = cx - playerCX;
        int dy = cy - playerCY;
        int radiusSum = hitboxRadius + playerRadius;
        return dx*dx + dy*dy <= radiusSum * radiusSum;
    }
    
    public boolean isOffScreen(int screenWidth, int screenHeight, int cameraX, int cameraY) {
        // Check if enemy is far from camera view (with buffer)
        int buffer = 200; // Buffer zone around screen
        return x < cameraX - buffer || 
               x > cameraX + screenWidth + buffer || 
               y < cameraY - buffer || 
               y > cameraY + screenHeight + buffer;
    }
    
    // Getters
    public int getX() { return (int)(x + width/2); } // Center X
    public int getY() { return (int)(y + height/2); } // Center Y
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getHitboxRadius() { return hitboxRadius; }
    public EnemyType getType() { return type; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long time) { this.lastShotTime = time; }
    public int getFireRate() { return fireRate; }
    public LaserBeam getActiveLaser() { return activeLaser; }
    
    // Methods
    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
        
        // TYPE2: show health bar when hit
        if (type == EnemyType.TYPE2) {
            showHealthBar = true;
            healthBarShowTime = System.currentTimeMillis();
        }
    }
    
    public boolean isDead() {
        if (type == EnemyType.TYPE2) {
            return health <= 0;
        }
        return health <= 0;
    }
    
    public void startLaser(double targetX, double targetY) {
        // TYPE1 only
        if (type == EnemyType.TYPE1 && activeLaser == null) {
            activeLaser = new LaserBeam(getX(), getY(), targetX, targetY);
        }
    }
}