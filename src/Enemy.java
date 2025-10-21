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
    private double speed;
    private Color color;
    private BufferedImage sprite;
    private int hitboxRadius; // circular hitbox
    private long lastShotTime = 0;
    private int fireRate; // ms between shots
    
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
                this.speed = 1.5;
                this.color = Color.RED;
                this.fireRate = 3000; // 3 seconds
                loadSprite("enemy1.png", "enemy_1.png");
                break;
            case TYPE2:
                this.width = 45; // 1.5x of 30
                this.height = 45;
                this.health = 100;
                this.speed = 1.0;
                this.color = Color.MAGENTA;
                this.fireRate = 5000; // 5 seconds
                loadSprite("enemy2.png", "enemy_2.png");
                break;
            case TYPE3:
                this.width = 30;
                this.height = 30;
                this.health = 50;
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
                this.speed = 1.5;
                this.color = Color.RED;
                this.fireRate = 3000; // 3 seconds
                loadSprite("enemy1.png", "enemy_1.png");
                break;
            case TYPE2:
                this.width = playerWidth * 2; // 2x player as requested
                this.height = playerHeight * 2;
                this.health = 100;
                this.speed = 1.0;
                this.color = Color.MAGENTA;
                this.fireRate = 5000; // 5 seconds
                loadSprite("enemy2.png", "enemy_2.png");
                break;
            case TYPE3:
                this.width = playerWidth;
                this.height = playerHeight;
                this.health = 50;
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
    }
    
    public void draw(Graphics2D g2d) {
    int drawX = (int) x;
    int drawY = (int) y;
        
        // Draw sprite if available, otherwise placeholder
        if (sprite != null) {
            g2d.drawImage(sprite, drawX, drawY, width, height, null);
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
        
        // Hitbox drawing removed (collision logic still uses hitboxRadius)
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
    public int getHitboxRadius() { return hitboxRadius; }
    public EnemyType getType() { return type; }
    public long getLastShotTime() { return lastShotTime; }
    public void setLastShotTime(long time) { this.lastShotTime = time; }
    public int getFireRate() { return fireRate; }
}