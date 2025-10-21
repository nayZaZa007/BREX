import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Player {
    private int x, y;
    private int width, height;
    private int health;
    private int maxHealth;
    private int speed;
    private long lastShotTime;
    private int fireRate; // milliseconds between shots ddd
    private BufferedImage spriteHighFire;
    private double facingAngle = -Math.PI / 2.0; // radians, 0 = right; set to face up initially
    // Smooth rotation target and speed
    private double targetFacingAngle = facingAngle;
    private double rotationSpeedRadPerSec = 4*Math.PI; // radians per second (default 180°/s)

    // Circular hitbox
    private int hitboxRadius; // in pixels (floor of 80% of min(width,height) / 2)
    
    // Movement with inertia
    private double vx = 0.0;
    private double vy = 0.0;
    // Input direction (-1,0,1)
    private int inputX = 0;
    private int inputY = 0;
    // Acceleration (pixels per second^2) and damping
    private double accel = 1200.0; // fairly responsive acceleration
    private double damping = 6.0; // per second, used when no input to slow down
    
    // Spacecraft / special ability state
    private int spacecraftType = 0; // 0=Large,1=Medium,2=Small
    private long lastSpecialUseTime = 0;
    private long specialCooldownMs = 30000; // default 30sddd
    // Large: shield
    private int shieldMax = 0;
    private int shieldCurrent = 0;
    // Medium: double fire
    private boolean doubleFireActive = false;
    private long doubleFireEndTime = 0;
    private int fireRateBackup = -1;
    // Small: teleport distance
    private int teleportDistance = 300;
    
    public Player(int x, int y) {
        this(x, y, 0, 400, 300, 60); // Default to Large spacecraft
    }
    
    // Constructor with spacecraft selection
    // spacecraftType: 0=Large, 1=Medium, 2=Small
    // stats: HP, Speed, Firerate (shots/min), Special ability index
    public Player(int x, int y, int spacecraftType, int hp, int speedStat, int firerateStat) {
        this.x = x;
        this.y = y;
        // store spacecraft type so specials map correctly
        this.spacecraftType = spacecraftType;
        this.width = 30;
        this.height = 30;
        // compute circular hitbox radius: 80% of the smaller dimension, then floor and divide by 2
        int minDim = Math.min(this.width, this.height);
        this.hitboxRadius = (int) Math.floor((minDim * 0.8) / 2.0);
        this.maxHealth = hp;
        this.health = hp;
        // speed is in pixels per second for the inertia integrator
        this.speed = speedStat;
        this.lastShotTime = 0;
        // Convert shots/min to milliseconds between shots
        this.fireRate = (int)(60000.0 / firerateStat); // e.g., 60 shots/min -> 1000ms
        
        // Load sprite based on spacecraft type
        String[] spriteNames = {
            "Spacecraft-Large.png",
            "Spacecraft-Medium.png",
            "spacecraft.png"
        };
        String spriteName = spriteNames[spacecraftType];
        
        // Load spacecraft sprite
        loadSprite(spriteName);
    }
    
    private void loadSprite(String spriteName) {
        // Try to load sprite from resources or Pic folder
        try {
            // Try classpath resource first
            java.io.InputStream is = getClass().getResourceAsStream("/Pic/" + spriteName);
            if (is != null) {
                spriteHighFire = ImageIO.read(is);
                is.close();
                return;
            }
        } catch (IOException ex) {
            spriteHighFire = null;
        }

        // If not found in classpath, try several fallback paths
        String cwd = new File(".").getAbsolutePath();
        System.out.println("Current working dir: " + cwd);
        String[] basePaths = {
            "Pic/",
            "./Pic/",
            "src/Pic/",
            "bin/Pic/",
            "resources/Pic/"
        };
        for (String base : basePaths) {
            try {
                File f = new File(base + spriteName);
                System.out.println("Trying: " + f.getAbsolutePath());
                if (f.exists()) {
                    spriteHighFire = ImageIO.read(f);
                    System.out.println("Loaded player sprite from: " + f.getAbsolutePath());
                    return;
                }
            } catch (IOException ex) {
                // continue trying
            }
        }

        System.out.println("Player sprite not found (" + spriteName + "). Using placeholder.");
    }
    
    // Update with delta time in milliseconds; smooth-rotate toward targetFacingAngle
    public void update(long deltaMs) {
        if (deltaMs <= 0) return;

        double maxDelta = rotationSpeedRadPerSec * (deltaMs / 1000.0);

        // Normalize angle difference to [-PI, PI]
        double diff = targetFacingAngle - facingAngle;
        diff = Math.atan2(Math.sin(diff), Math.cos(diff));

        if (Math.abs(diff) <= maxDelta) {
            facingAngle = targetFacingAngle;
        } else {
            facingAngle += Math.signum(diff) * maxDelta;
            // normalize facingAngle to [-PI, PI]
            facingAngle = Math.atan2(Math.sin(facingAngle), Math.cos(facingAngle));
        }
    }

    // Called from game loop to integrate movement and inertia
    // deltaMs: milliseconds since last frame
    public void integrateMovement(long deltaMs, int worldWidth, int worldHeight) {
        if (deltaMs <= 0) return;
        double dt = deltaMs / 1000.0;

        double targetVx = inputX * speed;
        double targetVy = inputY * speed;

        // Accelerate towards target velocity
        double ax = 0.0, ay = 0.0;
        if (inputX != 0) {
            ax = (targetVx - vx);
            // clamp acceleration per dt using accel
            double maxChange = accel * dt;
            if (Math.abs(ax) > maxChange) ax = Math.signum(ax) * maxChange;
        } else {
            // apply damping toward 0
            double maxChange = damping * dt * speed;
            if (Math.abs(vx) <= maxChange) ax = -vx;
            else ax = -Math.signum(vx) * maxChange;
        }

        if (inputY != 0) {
            ay = (targetVy - vy);
            double maxChange = accel * dt;
            if (Math.abs(ay) > maxChange) ay = Math.signum(ay) * maxChange;
        } else {
            double maxChange = damping * dt * speed;
            if (Math.abs(vy) <= maxChange) ay = -vy;
            else ay = -Math.signum(vy) * maxChange;
        }

        vx += ax;
        vy += ay;

        // Integrate position
        double nx = x + vx * dt;
        double ny = y + vy * dt;

        // Keep player within world bounds (x,y are top-left)
        if (nx < 0) {
            nx = 0;
            vx = 0;
        }
        if (nx > worldWidth - width) {
            nx = worldWidth - width;
            vx = 0;
        }
        if (ny < 0) {
            ny = 0;
            vy = 0;
        }
        if (ny > worldHeight - height) {
            ny = worldHeight - height;
            vy = 0;
        }

        x = (int) Math.round(nx);
        y = (int) Math.round(ny);
    }
    
    public void move(int dx, int dy, int worldWidth, int worldHeight) {
        // Deprecated immediate move: interpret dx/dy as directional intent
        // dx/dy are expected to be -1/0/1 multiplied by speed in the caller
        // Set input direction which will be consumed by integrateMovement
        if (dx < 0) inputX = -1;
        else if (dx > 0) inputX = 1;
        else inputX = 0;

        if (dy < 0) inputY = -1;
        else if (dy > 0) inputY = 1;
        else inputY = 0;
    }
    
    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }
    
    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }
    
    public void increaseSpeed(int amount) {
        speed += amount;
    }
    
    public void increaseFireRate(int reduction) {
        fireRate -= reduction;
        if (fireRate < 100) fireRate = 100; // Minimum fire rate
    }

    // Special ability: use when player presses 'F'
    // worldWidth/worldHeight used for teleport bounds
    public void useSpecial(int worldWidth, int worldHeight) {
        long now = System.currentTimeMillis();
        // Check cooldown
        long cd = getSpecialCooldownMsForType(spacecraftType);
        if (now - lastSpecialUseTime < cd) return; // still cooling down

        lastSpecialUseTime = now;

        switch (spacecraftType) {
            case 0: // Large: grant shield gauge (equal to 50% of maxHealth)
                shieldMax = Math.max(1, maxHealth / 2);
                shieldCurrent = shieldMax;
                break;
            case 1: // Medium: double fire rate for duration
                if (!doubleFireActive) {
                    fireRateBackup = fireRate;
                    // halve the delay (double shots) - fireRate stored as ms between shots
                    fireRate = Math.max(50, fireRate / 2);
                    doubleFireActive = true;
                    doubleFireEndTime = now + 18000; // 18s
                }
                break;
            case 2: // Small: teleport forward in facing direction
                double dirX = Math.cos(facingAngle);
                double dirY = Math.sin(facingAngle);
                double nx = x + dirX * teleportDistance;
                double ny = y + dirY * teleportDistance;
                // Clamp to world
                if (nx < 0) nx = 0;
                if (nx > worldWidth - width) nx = worldWidth - width;
                if (ny < 0) ny = 0;
                if (ny > worldHeight - height) ny = worldHeight - height;
                x = (int)Math.round(nx);
                y = (int)Math.round(ny);
                break;
        }
    }

    private long getSpecialCooldownMsForType(int type) {
        switch (type) {
            case 0: return 30000; // 30s
            case 1: return 18000; // 18s
            case 2: return 8000;  // 8s
        }
        return 30000;
    }

    // Call this per-frame to update timed effects
    public void updateSpecials() {
        long now = System.currentTimeMillis();
        if (doubleFireActive && now >= doubleFireEndTime) {
            doubleFireActive = false;
            if (fireRateBackup > 0) {
                fireRate = fireRateBackup;
                fireRateBackup = -1;
            }
        }
    }

    // Consume damage - respects shield first
    public void consumeDamage(int dmg) {
        if (shieldCurrent > 0) {
            int taken = Math.min(shieldCurrent, dmg);
            shieldCurrent -= taken;
            dmg -= taken;
        }
        if (dmg > 0) takeDamage(dmg);
    }

    // Getters for UI
    public int getShieldCurrent() { return shieldCurrent; }
    public int getShieldMax() { return shieldMax; }
    public long getLastSpecialUseTime() { return lastSpecialUseTime; }
    public long getSpecialCooldownMs() { return getSpecialCooldownMsForType(spacecraftType); }
    public boolean isDoubleFireActive() { return doubleFireActive; }
    
    public void draw(Graphics2D g2d) {

        // Draw sprite at 2x scale (centered on player center) and glow behind it
        int spriteDrawW = width * 2;
        int spriteDrawH = height * 2;
    // sprite draw coordinates are computed when drawing with transform (centered at 0,0)

        int cx = x + width/2;
        int cy = y + height/2;
        // No glow: draw only the sprite (or fallback rectangle)

        // Draw sprite if available, otherwise fallback to cyan rect (2x)
        AffineTransform old = g2d.getTransform();
    // rotate around player center; add +90deg offset so the image's right side is the nose
    double rotationOffset = Math.PI / 2.0; // 90 degrees
    g2d.translate(cx, cy);
    g2d.rotate(facingAngle + rotationOffset);
        if (spriteHighFire != null) {
            g2d.drawImage(spriteHighFire, -spriteDrawW/2, -spriteDrawH/2, spriteDrawW, spriteDrawH, null);
        } else {
            g2d.setColor(Color.CYAN);
            g2d.fillRect(-spriteDrawW/2, -spriteDrawH/2, spriteDrawW, spriteDrawH);
        }
        g2d.setTransform(old);

        // (Hitbox drawing removed; collisions still use hitboxRadius internally)
    }
    
    // Rectangle-circle collision: other is axis-aligned rectangle
    public boolean collidesWith(int otherX, int otherY, int otherWidth, int otherHeight) {
        // circle center
        int cxHit = x + width/2;
        int cyHit = y + height/2;
        int radius = hitboxRadius;

        // Find closest point on rect to circle center
        int closestX = Math.max(otherX, Math.min(cxHit, otherX + otherWidth));
        int closestY = Math.max(otherY, Math.min(cyHit, otherY + otherHeight));

        int dx = cxHit - closestX;
        int dy = cyHit - closestY;
        return dx*dx + dy*dy <= radius * radius;
    }
    
    // Getters
    public int getX() { return x + width/2; } // Center X
    public int getY() { return y + height/2; } // Center Y
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    // Hitbox radius getter
    public int getHitboxRadius() { return hitboxRadius; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getSpeed() { return speed; }
    public long getLastShotTime() { return lastShotTime; }
    public int getFireRate() { return fireRate; }
    
    // Setters
    public void setLastShotTime(long time) { this.lastShotTime = time; }

    // Set the target facing angle; rotation will smoothly move toward it in update(dt)
    public void setFacingAngle(double angle) { this.targetFacingAngle = Math.atan2(Math.sin(angle), Math.cos(angle)); }
    public double getFacingAngle() { return this.facingAngle; }

    // Optional: adjust rotation speed (radians per second)
    public void setRotationSpeedRadPerSec(double r) { this.rotationSpeedRadPerSec = r; }
    public double getRotationSpeedRadPerSec() { return this.rotationSpeedRadPerSec; }
}