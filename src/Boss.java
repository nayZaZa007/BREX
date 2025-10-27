import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Boss {
    private double x, y;
    private int width, height;
    private int health;
    private int maxHealth;
    private double speed;
    private BufferedImage sprite;
    private int hitboxRadius;
    private long lastShotTime = 0;
    private int fireRate = 2000; // 2 seconds between shots (not used in phase system)
    private boolean isDead = false;
    
    // Movement pattern
    private double moveTimer = 0;
    private double angle = 0;
    
    // Attack phase system
    public enum AttackPhase {
        BARRAGE,    // Phase 1: Shoot bullets in grid pattern
        LASER_SPIN, // Phase 2: 6-way laser spin
        HOMING      // Phase 3: Homing bullets
    }
    
    private AttackPhase currentPhase = AttackPhase.BARRAGE;
    private double phaseTimer = 0;
    private double phaseDuration = 0;
    private Random random = new Random();
    private int phaseCompletionCount = 0; // นับจำนวนเฟสที่ผ่านไปแล้ว
    
    // Phase-specific timers
    private double barrageTimer = 0;
    private static final double BARRAGE_SHOT_INTERVAL = 0.3; // 0.3 seconds between shots
    private int barrageShots = 0;
    
    // Enemy spawn system
    private long lastEnemySpawn = 0;
    private static final long ENEMY_SPAWN_COOLDOWN_MIN = 10000; // 10 seconds
    private static final long ENEMY_SPAWN_COOLDOWN_MAX = 15000; // 15 seconds
    private long nextEnemySpawnTime = 0;
    
    // Movement
    private double moveSpeed = 0.5; // ช้ามาก
    private double targetX, targetY; // เป้าหมายการเคลื่อนที่
    private double movementTimer = 0;
    
    public Boss(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 120;
        this.height = 120;
        this.maxHealth = 1000;
        this.health = maxHealth;
        this.speed = 0.5; // ช้ามาก
        this.targetX = x;
        this.targetY = y;
        
        // Circular hitbox
        int minDim = Math.min(this.width, this.height);
        this.hitboxRadius = (int)(minDim * 0.8) / 2;
        
        loadSprite("boss.png", "Boss.png");
        
        // Start with barrage phase
        startBarragePhase();
        
        // Set first enemy spawn time
        nextEnemySpawnTime = System.currentTimeMillis() + ENEMY_SPAWN_COOLDOWN_MIN + 
                            (long)(random.nextDouble() * (ENEMY_SPAWN_COOLDOWN_MAX - ENEMY_SPAWN_COOLDOWN_MIN));
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
    
    private void startBarragePhase() {
        currentPhase = AttackPhase.BARRAGE;
        phaseDuration = 5.0 + random.nextDouble() * 7.0; // 5-12 seconds
        phaseTimer = 0;
        barrageTimer = 0;
        barrageShots = 0;
        System.out.println("Boss: Starting BARRAGE phase for " + phaseDuration + "s");
    }
    
    private void startLaserPhase() {
        currentPhase = AttackPhase.LASER_SPIN;
        phaseDuration = 4.0 + random.nextDouble() * 4.0; // 4-8 seconds
        phaseTimer = 0;
        System.out.println("Boss: Starting LASER_SPIN phase for " + phaseDuration + "s");
    }
    
    private void startHomingPhase() {
        currentPhase = AttackPhase.HOMING;
        phaseDuration = 3.0; // Fixed 3 seconds for homing phase
        phaseTimer = 0;
        System.out.println("Boss: Starting HOMING phase for " + phaseDuration + "s");
    }
    
    public void update(Player player, long deltaMs) {
        double dt = deltaMs / 1000.0;
        
        // Update phase timer
        phaseTimer += dt;
        
        // Check if phase is over
        if (phaseTimer >= phaseDuration) {
            phaseCompletionCount++;
            
            // หลังจากผ่าน 3 เฟสแรกแล้ว (BARRAGE → LASER → HOMING) ให้สุ่มเฟสต่อไป
            if (phaseCompletionCount >= 3) {
                // สุ่มเฟสถัดไป
                int randomPhase = random.nextInt(3);
                switch (randomPhase) {
                    case 0:
                        startBarragePhase();
                        break;
                    case 1:
                        startLaserPhase();
                        break;
                    case 2:
                        startHomingPhase();
                        break;
                }
            } else {
                // 3 เฟสแรก: BARRAGE → LASER_SPIN → HOMING
                switch (currentPhase) {
                    case BARRAGE:
                        startLaserPhase();
                        break;
                    case LASER_SPIN:
                        startHomingPhase();
                        break;
                    case HOMING:
                        // หลังเฟส 3 เสร็จ จะเข้าสู่โหมดสุ่ม
                        startBarragePhase(); // เฟสแรกของโหมดสุ่ม
                        break;
                }
            }
        }
        
        // Phase-specific updates
        switch (currentPhase) {
            case BARRAGE:
                barrageTimer += dt;
                break;
            case LASER_SPIN:
                // Handled in SpaceGame
                break;
            case HOMING:
                // Handled in SpaceGame
                break;
        }
        
        // Slow movement
        movementTimer += dt;
        if (movementTimer > 3.0) { // เปลี่ยนทิศทุก 3 วินาที
            movementTimer = 0;
            // สุ่มตำแหน่งเป้าหมายใหม่ในพื้นที่บนๆ ของหน้าจอ
            targetX = 100 + random.nextDouble() * (3000 - 200); // WORLD_WIDTH = 3000
            targetY = 50 + random.nextDouble() * 150; // พื้นที่บนสุด
        }
        
        // เคลื่อนที่ไปยังเป้าหมายอย่างช้าๆ
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > moveSpeed) {
            x += (dx / distance) * moveSpeed;
            y += (dy / distance) * moveSpeed;
        }
        
        // Boss movement pattern: slow circular motion
        moveTimer += dt;
        angle = moveTimer * 0.5;
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
        
        // Draw phase indicator
        drawPhaseIndicator(g2d);
    }
    
    private void drawPhaseIndicator(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String phaseText = "PHASE: ";
        Color phaseColor = Color.WHITE;
        
        switch (currentPhase) {
            case BARRAGE:
                phaseText += "BARRAGE";
                phaseColor = Color.ORANGE;
                break;
            case LASER_SPIN:
                phaseText += "LASER SPIN";
                phaseColor = Color.CYAN;
                break;
            case HOMING:
                phaseText += "HOMING";
                phaseColor = Color.YELLOW;
                break;
        }
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)x - fm.stringWidth(phaseText)/2;
        int textY = (int)y - height/2 - 20;
        
        g2d.setColor(phaseColor);
        g2d.drawString(phaseText, textX, textY);
        
        // Draw phase timer bar
        int timerBarWidth = 100;
        int timerBarHeight = 4;
        int timerBarX = (int)x - timerBarWidth/2;
        int timerBarY = textY + 5;
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(timerBarX, timerBarY, timerBarWidth, timerBarHeight);
        
        double progress = Math.min(1.0, phaseTimer / phaseDuration);
        g2d.setColor(phaseColor);
        g2d.fillRect(timerBarX, timerBarY, (int)(timerBarWidth * progress), timerBarHeight);
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
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    // Phase system getters
    public AttackPhase getCurrentPhase() { return currentPhase; }
    public double getPhaseTimer() { return phaseTimer; }
    public double getPhaseDuration() { return phaseDuration; }
    public double getBarrageTimer() { return barrageTimer; }
    public void resetBarrageTimer() { barrageTimer = 0; }
    public int getBarrageShots() { return barrageShots; }
    public void incrementBarrageShots() { barrageShots++; }
    public double getBarrageShotInterval() { return BARRAGE_SHOT_INTERVAL; }
    
    // Enemy spawn system
    public boolean shouldSpawnEnemies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextEnemySpawnTime) {
            lastEnemySpawn = currentTime;
            nextEnemySpawnTime = currentTime + ENEMY_SPAWN_COOLDOWN_MIN + 
                                (long)(random.nextDouble() * (ENEMY_SPAWN_COOLDOWN_MAX - ENEMY_SPAWN_COOLDOWN_MIN));
            return true;
        }
        return false;
    }
}
