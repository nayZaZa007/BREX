import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.sound.sampled.*;

public class SpaceGame extends JPanel implements ActionListener, KeyListener {
    
    // Game States
    public enum GameState {
        MENU, SPACECRAFT_SELECT, GAME, OPTIONS, EXIT_CONFIRM, PAUSED
    }
    
    private static final int SCREEN_WIDTH = 1000;  // หน้าจอที่เห็น
    private static final int SCREEN_HEIGHT = 700;  // หน้าจอที่เห็น
    private static final int WORLD_WIDTH = 3000;   // โลกทั้งหมด (3x ใหญ่กว่า)
    private static final int WORLD_HEIGHT = 2100;  // โลกทั้งหมด (3x ใหญ่กว่า)
    private static final int DELAY = 16; // ~60 FPS
    
    private Timer timer;
    private Player player;
    private Player player2; // Co-op mode player 2
    private boolean coopMode = false; // Co-op mode active
    private ArrayList<Bullet> bullets2; // Player 2 bullets
    private ArrayList<Enemy> enemies;
    private ArrayList<Bullet> bullets;
    private ArrayList<EnemyBullet> enemyBullets;
    private ArrayList<PowerUp> powerUps;
    private ArrayList<DamagePopup> damagePopups;
    private ArrayList<ExplosionParticle> explosionParticles;
    private Random random;
    private int score;
    private int level;
    private int enemySpawnRate;
    private long gameStartTime;
    private long lastEnemySpawn;
    private boolean gameRunning;
    private long lastUpdateTimeMillis = System.currentTimeMillis();
    
    // Boss
    private Boss boss;
    private boolean bossSpawned = false;
    private static final long BOSS_SPAWN_TIME = 10000; // 3.5 minutes in milliseconds
    
    // Boss attack system
    private ArrayList<BossBullet> bossBullets = new ArrayList<>();
    private ArrayList<BossLaser> bossLasers = new ArrayList<>();
    private boolean bossLasersCreated = false;
    private double laserRotationDirection = 1.0; // 1.0 = clockwise, -1.0 = counter-clockwise
    private long lastHomingBulletSpawn = 0; // เวลาที่ spawn homing bullet ครั้งล่าสุด
    private static final long HOMING_SPAWN_INTERVAL = 2000; // spawn ทุก 2 วินาที
    
    // Boss damage cooldowns
    private long lastBossCollisionDamage = 0;
    private long lastBossLaserDamage = 0;
    private static final long BOSS_DAMAGE_COOLDOWN = 500; // 0.5 วินาที
    
    // Boss death animation
    private boolean bossDeathAnimationActive = false;
    private long bossDeathStartTime = 0;
    private static final long BOSS_DEATH_DURATION = 8000; // 8 seconds
    private int bossFinalX, bossFinalY; // Store boss position when it dies
    private float bossDeathAlpha = 1.0f; // For fading boss sprite
    
    // TYPE1 enemy spawn cap
    private int type1MaxCap = 1; // Random 1-6
    private long lastCapUpdate = 0;
    
    // Camera System
    private int cameraX, cameraY;
    
    // Background System
    private final int STAR_COUNT = 200;
    private int[] starX, starY, starZ; // Z for depth/size
    
    // Game State Management
    private GameState currentState;
    private int selectedMenuOption;
    private int selectedOptionsItem;
    private GameState lastStateBeforeExitConfirm;
    
    // Menu Options
    private final String[] mainMenuOptions = {"Play", "Options", "Exit"};
    // Options will be generated from state (volume + fullscreen)
    private String[] optionsMenuItems;
    private final String[] pauseMenuOptions = {"Resume", "Settings", "Return to Main Menu"};

    // Options state
    private int bgmVolume; // 0-100
    private int sfxVolume; // 0-100
    private boolean fullscreen;
    
    // Spacecraft selection
    private int selectedSpacecraft = 0; // 0=Large, 1=Medium, 2=Small
    private BufferedImage[] spacecraftMenuSprites = new BufferedImage[3];
    // Spacecraft stats: [stat][spacecraft] where spacecraft: 0=Large, 1=Medium, 2=Small
    // Stats: 0=HP, 1=Speed, 2=Firerate, 3=Special (description stored separately)
    private int[][] spacecraftStats = {
        {400, 200, 75},      // HP
        {70, 100, 170},       // Speed
        {60, 85, 120},       // Firerate (shots per minute)
        {0, 1, 2}            // Special ability index
    };
    private String[] specialAbilityDescriptions = {
        "Shield: Absorbs damage (CD 30s)",
        "Double Fire Rate (CD 18s)",
        "Teleport in direction (CD 8s)"
    };
    
    // Controls
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    
    // Manual control mode
    private boolean manualControlMode = false;
    private boolean arrowUpPressed, arrowDownPressed, arrowLeftPressed, arrowRightPressed;
    
    // Special ability keys
    private boolean fPressed = false; // F key for player1 special
    private boolean rightShiftPressed = false; // Right Shift for player2 special
    
    // Easter Egg System
    private static final String EASTER_EGG_FILE = "easter_egg.dat";
    private int easterEggMode = 0; // 0 = normal, 1 = special mode
    private String keySequence = ""; // Track key sequence in menu
    private static final String SECRET_CODE_SPECIAL = "drifx";
    private static final String SECRET_CODE_NORMAL = "brex";
    
    // Audio System
    private Clip bgmClip;
    private static final int SFX_POOL_SIZE = 8; // Pool size for simultaneous sound effects
    private Clip[] sfxPool = new Clip[SFX_POOL_SIZE];
    private int sfxPoolIndex = 0; // Round-robin index for pool
    private boolean audioInitialized = false;
    private boolean isInGameBGM = false; // Track if we're in game (for Easter egg BGM cycling)
    private boolean isFirstGameBGM = true; // Track if this is the first in-game BGM
    
    public SpaceGame() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        
        // Load Easter egg mode from file
        loadEasterEggMode();
        
        // Initialize audio system
        initializeAudio();
        
        // เริ่มต้นที่หน้าเมนู
        currentState = GameState.MENU;
        selectedMenuOption = 0;
        selectedOptionsItem = 0;
        
        initializeGame();
        loadSpacecraftMenuSprites();
        
        // Play menu BGM
        playBGM();
    }
    
    private void loadSpacecraftMenuSprites() {
        String[] menuSpriteNames = {
            "Menu_Spacecraft-Large.png",
            "Menu_Spacecraft-Medium_.png",
            "Menu_Spacecraft.png"
        };
        String[] basePaths = {
            "src/Pic/",
            "./src/Pic/",
            "Pic/",
            "./Pic/",
            "bin/Pic/"
        };
        
        for (int i = 0; i < menuSpriteNames.length; i++) {
            String name = menuSpriteNames[i];
            // Try classpath first
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/Pic/" + name);
                if (is != null) {
                    spacecraftMenuSprites[i] = ImageIO.read(is);
                    is.close();
                    continue;
                }
            } catch (IOException ex) {
                // continue
            }
            
            // Try filesystem paths
            for (String base : basePaths) {
                try {
                    File f = new File(base + name);
                    if (f.exists()) {
                        spacecraftMenuSprites[i] = ImageIO.read(f);
                        break;
                    }
                } catch (IOException ex) {
                    // continue
                }
            }
        }
    }
    
    private void initializeGame() {
        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);
        player2 = null; // Co-op player starts as null
        bullets2 = new ArrayList<>(); // Initialize bullets2 list
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        damagePopups = new ArrayList<>();
        explosionParticles = new ArrayList<>();
        random = new Random();
        boss = null;
        bossSpawned = false;
        type1MaxCap = 1 + random.nextInt(6); // Random 1-6
        lastCapUpdate = System.currentTimeMillis();
        
        // Initialize background stars
        initializeBackground();
        
        // Initialize camera
        cameraX = 0;
        cameraY = 0;
        score = 0;
        level = 1;
        enemySpawnRate = 1000; // milliseconds between spawns
        gameStartTime = System.currentTimeMillis();
        lastEnemySpawn = System.currentTimeMillis();
        gameRunning = true;
        
        timer = new Timer(DELAY, this);
        // Initialize options state
        initializeOptions();
    }
    
    public void startGame() {
        timer.start();
    }
    
    private void initializeBackground() {
        starX = new int[STAR_COUNT];
        starY = new int[STAR_COUNT];
        starZ = new int[STAR_COUNT];
        
        // Generate random stars across the entire world space
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextInt(WORLD_WIDTH);
            starY[i] = random.nextInt(WORLD_HEIGHT);
            starZ[i] = random.nextInt(3) + 1; // Size 1-3
        }
    }

    private void initializeOptions() {
        bgmVolume = 80; // default BGM volume
        sfxVolume = 80; // default SFX volume
        fullscreen = false;
        refreshOptionsItems();
    }

    private void refreshOptionsItems() {
        optionsMenuItems = new String[] {
            "BGM Volume: " + bgmVolume,
            "SFX Volume: " + sfxVolume,
            "Fullscreen: " + (fullscreen ? "ON" : "OFF"),
            "Back to Menu"
        };
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.GAME && gameRunning) {
            update();
        }
        repaint();
    }
    
    private void update() {
        // Compute delta time
        long now = System.currentTimeMillis();
        long delta = now - lastUpdateTimeMillis;
        if (delta < 0) delta = 0;
        lastUpdateTimeMillis = now;

    // Update player: movement & rotation
    updatePlayerMovement();
    // integrate movement with inertia
    player.integrateMovement(delta, WORLD_WIDTH, WORLD_HEIGHT);
    // update rotation smoothing
    player.update(delta);
    // update any timed special effects (double-fire expiration, etc.)
    player.updateSpecials();
    
    // Update player2 if co-op mode active
    if (coopMode && player2 != null) {
        updatePlayer2Movement();
        player2.integrateMovement(delta, WORLD_WIDTH, WORLD_HEIGHT);
        player2.update(delta);
        player2.updateSpecials();
    }
        
        // Update camera to follow player
        updateCamera();
        
        // Update TYPE1 max cap every 15 seconds
        if (System.currentTimeMillis() - lastCapUpdate > 15000) {
            type1MaxCap = 1 + random.nextInt(6); // Random 1-6
            lastCapUpdate = System.currentTimeMillis();
            System.out.println("TYPE1 max cap updated: " + type1MaxCap);
        }
        
        // Check for Boss spawn at 3.5 minutes
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        if (!bossSpawned && elapsedTime >= BOSS_SPAWN_TIME) {
            spawnBoss();
            bossSpawned = true;
        }
        
        // Spawn enemies (stop spawning when boss appears)
        if (!bossSpawned && System.currentTimeMillis() - lastEnemySpawn > enemySpawnRate) {
            spawnEnemy();
            lastEnemySpawn = System.currentTimeMillis();
            System.out.println("Enemy spawned! Total enemies: " + enemies.size());
        }
        
        // Update enemies
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update(player);
            
            // Remove if off screen
            if (enemy.isOffScreen(SCREEN_WIDTH, SCREEN_HEIGHT, cameraX, cameraY)) {
                enemyIterator.remove();
                System.out.println("Enemy removed (off screen). Total enemies: " + enemies.size());
            }
            
            // Check collision with player
            if (enemy.collidesWith(player)) {
                // consumeDamage respects shield first
                player.consumeDamage(10);
                enemyIterator.remove();
                System.out.println("Player hit! Health: " + player.getHealth());
                
                if (player.getHealth() <= 0) {
                    System.out.println("Game Over! Final Score: " + score);
                    gameRunning = false;
                    // ลบ Player2 ออกเมื่อ Game Over
                    if (coopMode && player2 != null) {
                        player2 = null;
                        coopMode = false;
                    }
                }
            }
            
            // Enemy shooting based on fire rate
            long currentTime = System.currentTimeMillis();
            if (currentTime - enemy.getLastShotTime() > enemy.getFireRate()) {
                shootEnemyBullets(enemy);
                enemy.setLastShotTime(currentTime);
            }
        }
        
        // Update bullets
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();
            
            // Remove if expired (1 minute)
            if (bullet.isExpired()) {
                bulletIterator.remove();
                System.out.println("Bullet expired! Total bullets: " + bullets.size());
                continue;
            }

            // Track whether this bullet was removed during collision handling
            boolean bulletRemoved = false;

            // Check bullet-enemy collision
            Iterator<Enemy> enemyIterator2 = enemies.iterator();
            while (enemyIterator2.hasNext()) {
                Enemy enemy = enemyIterator2.next();
                if (bullet.collidesWith(enemy)) {
                    int dmg = bullet.getDamage();
                    enemy.takeDamage(dmg);
                    damagePopups.add(new DamagePopup(enemy.getX(), enemy.getY(), dmg, Color.YELLOW));
                    bulletIterator.remove();
                    bulletRemoved = true;

                    if (enemy.isDead()) {
                        enemyIterator2.remove();
                        score += 10;
                        System.out.println("Enemy destroyed! Score: " + score);

                        // Chance to spawn power-up
                        if (random.nextInt(10) == 0) {
                            powerUps.add(new PowerUp(enemy.getX(), enemy.getY()));
                        }
                    } else {
                        System.out.println("Enemy hit! HP: " + enemy.getHealth() + "/" + enemy.getMaxHealth());
                    }
                    break;
                }
            }

            // If bullet was removed due to colliding with an enemy, skip further processing for this bullet
            if (bulletRemoved) continue;

            // Check bullet-boss collision
            if (boss != null && !boss.isDead() && bullet.collidesWith(boss)) {
                int dmg = bullet.getDamage();
                boss.takeDamage(dmg);
                damagePopups.add(new DamagePopup(boss.getX(), boss.getY(), dmg, Color.RED));
                bulletIterator.remove();
                System.out.println("Boss hit! HP: " + boss.getHealth() + "/" + boss.getMaxHealth());

                if (boss.isDead()) {
                    System.out.println("=== BOSS DEFEATED ===");
                    // Deactivate all boss lasers
                    for (BossLaser laser : bossLasers) {
                        laser.deactivate();
                    }
                    // Start boss death animation
                    if (!bossDeathAnimationActive) {
                        bossDeathAnimationActive = true;
                        bossDeathStartTime = System.currentTimeMillis();
                        bossFinalX = (int) boss.getX();
                        bossFinalY = (int) boss.getY();
                    }
                    score += 500;
                }
            }
        }
        
        // Update player2 bullets (co-op mode)
        if (coopMode && player2 != null) {
            Iterator<Bullet> bullet2Iterator = bullets2.iterator();
            while (bullet2Iterator.hasNext()) {
                Bullet bullet = bullet2Iterator.next();
                bullet.update();
                
                // Remove if expired
                if (bullet.isExpired()) {
                    bullet2Iterator.remove();
                    continue;
                }
                
                // Check bullet-enemy collision
                Iterator<Enemy> enemyIterator3 = enemies.iterator();
                while (enemyIterator3.hasNext()) {
                    Enemy enemy = enemyIterator3.next();
                    if (bullet.collidesWith(enemy)) {
                        int dmg = bullet.getDamage();
                        enemy.takeDamage(dmg);
                        damagePopups.add(new DamagePopup(enemy.getX(), enemy.getY(), dmg, new Color(255, 105, 180)));
                        bullet2Iterator.remove();
                        
                        if (enemy.isDead()) {
                            enemyIterator3.remove();
                            score += 10;
                            
                            if (random.nextInt(10) == 0) {
                                powerUps.add(new PowerUp(enemy.getX(), enemy.getY()));
                            }
                        }
                        break;
                    }
                }
                
                // Check bullet-boss collision
                if (boss != null && !boss.isDead() && bullet.collidesWith(boss)) {
                    int dmg = bullet.getDamage();
                    boss.takeDamage(dmg);
                    damagePopups.add(new DamagePopup((int)boss.getX(), (int)boss.getY(), dmg, new Color(255, 105, 180)));
                    bullet2Iterator.remove();
                    
                    if (boss.isDead()) {
                        System.out.println("=== BOSS DEFEATED (by Player2) ===");
                        // Deactivate all boss lasers
                        for (BossLaser laser : bossLasers) {
                            laser.deactivate();
                        }
                        // Start boss death animation
                        if (!bossDeathAnimationActive) {
                            bossDeathAnimationActive = true;
                            bossDeathStartTime = System.currentTimeMillis();
                            bossFinalX = (int) boss.getX();
                            bossFinalY = (int) boss.getY();
                        }
                        score += 500;
                    }
                }
            }
        }
        
        // Update enemy bullets
        Iterator<EnemyBullet> enemyBulletIterator = enemyBullets.iterator();
        while (enemyBulletIterator.hasNext()) {
            EnemyBullet enemyBullet = enemyBulletIterator.next();
            enemyBullet.update();
            
            // Remove if expired (1 minute)
            if (enemyBullet.isExpired()) {
                enemyBulletIterator.remove();
                System.out.println("Enemy bullet expired! Total enemy bullets: " + enemyBullets.size());
                continue;
            }
            
            // Check collision with player
            if (enemyBullet.collidesWith(player)) {
                int dmg = enemyBullet.getDamage();
                player.consumeDamage(dmg);
                damagePopups.add(new DamagePopup(player.getX(), player.getY(), dmg, Color.RED));
                enemyBulletIterator.remove();
                System.out.println("Player hit by bullet! Health: " + player.getHealth() + " (damage: " + dmg + ")");
                
                if (player.getHealth() <= 0) {
                    System.out.println("Game Over! Final Score: " + score);
                    gameRunning = false;
                    // ลบ Player2 ออกเมื่อ Game Over
                    if (coopMode && player2 != null) {
                        player2 = null;
                        coopMode = false;
                    }
                }
                continue;
            }
            
            // Check collision with player2 (co-op mode) - ใช้เลือดร่วมกับ Player1
            if (coopMode && player2 != null && enemyBullet.collidesWith(player2)) {
                int dmg = enemyBullet.getDamage();
                player.consumeDamage(dmg); // หัก HP จาก Player1 (ใช้เลือดร่วมกัน)
                damagePopups.add(new DamagePopup(player2.getX(), player2.getY(), dmg, Color.RED));
                enemyBulletIterator.remove();
                System.out.println("Player2 hit by bullet! Shared Health: " + player.getHealth() + " (damage: " + dmg + ")");
                
                if (player.getHealth() <= 0) {
                    // เลือดหมด = Game Over
                    System.out.println("Game Over! Final Score: " + score);
                    gameRunning = false;
                    // ลบ Player2 ออก
                    player2 = null;
                    coopMode = false;
                }
                continue;
            }
        }
        
        // Update Boss
        if (boss != null && !boss.isDead()) {
            boss.update(player, delta);
            
            // Boss attack phase system
            handleBossAttacks(delta);
            
            // Boss enemy spawn system (5-10 enemies, cooldown 10-15s)
            if (boss.shouldSpawnEnemies()) {
                spawnBossEnemies();
            }
            
            // Boss collision with player (10-20 damage per 0.5s)
            if (boss.collidesWith(player)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBossCollisionDamage >= BOSS_DAMAGE_COOLDOWN) {
                    int damage = 10 + random.nextInt(11); // 10-20
                    player.consumeDamage(damage);
                    damagePopups.add(new DamagePopup(player.getX(), player.getY(), damage, Color.RED)); // สีเดียวกับศัตรู
                    lastBossCollisionDamage = currentTime;
                    System.out.println("Player hit by Boss collision! Damage: " + damage + " Health: " + player.getHealth());
                    
                    if (player.getHealth() <= 0) {
                        System.out.println("Game Over! Final Score: " + score);
                        gameRunning = false;
                        // ลบ Player2 ออกเมื่อ Game Over
                        if (coopMode && player2 != null) {
                            player2 = null;
                            coopMode = false;
                        }
                    }
                }
            }
        }
        
        // Update boss bullets
        Iterator<BossBullet> bossBulletIterator = bossBullets.iterator();
        while (bossBulletIterator.hasNext()) {
            BossBullet bullet = bossBulletIterator.next();
            bullet.update(player);
            
            if (bullet.collidesWith(player)) {
                player.consumeDamage(bullet.getDamage());
                damagePopups.add(new DamagePopup(player.getX(), player.getY(), bullet.getDamage(), Color.RED)); // สีเดียวกับศัตรู
                bossBulletIterator.remove();
                System.out.println("Player hit by boss bullet! Health: " + player.getHealth());
                
                if (player.getHealth() <= 0) {
                    System.out.println("Game Over! Final Score: " + score);
                    gameRunning = false;
                    // ลบ Player2 ออกเมื่อ Game Over
                    if (coopMode && player2 != null) {
                        player2 = null;
                        coopMode = false;
                    }
                }
            } else if (bullet.isOffScreen(WORLD_WIDTH, WORLD_HEIGHT)) {
                bossBulletIterator.remove();
            }
        }
        
        // Update boss lasers
        double dt = delta / 1000.0;
        for (BossLaser laser : bossLasers) {
            if (laser.isActive() && boss != null && !boss.isDead()) {
                laser.updatePosition(boss.getX(), boss.getY());
                laser.update(dt);
                
                // Boss laser damage (17-30 per 0.5s)
                if (laser.collidesWith(player)) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBossLaserDamage >= BOSS_DAMAGE_COOLDOWN) {
                        int damage = 17 + random.nextInt(14); // 17-30
                        player.consumeDamage(damage);
                        damagePopups.add(new DamagePopup(player.getX(), player.getY(), damage, Color.RED)); // สีเดียวกับศัตรู
                        lastBossLaserDamage = currentTime;
                        System.out.println("Player hit by boss laser! Damage: " + damage + " Health: " + player.getHealth());
                        
                        if (player.getHealth() <= 0) {
                            System.out.println("Game Over! Final Score: " + score);
                            gameRunning = false;
                            // ลบ Player2 ออกเมื่อ Game Over
                            if (coopMode && player2 != null) {
                                player2 = null;
                                coopMode = false;
                            }
                        }
                    }
                }
            }
        }
        
        // Check laser hits from TYPE1 enemies
        for (Enemy enemy : enemies) {
            if (enemy.getType() == Enemy.EnemyType.TYPE1) {
                LaserBeam laser = enemy.getActiveLaser();
                if (laser != null && laser.hitsPlayer(player)) {
                    int dmg = laser.getDamage();
                    player.consumeDamage(dmg);
                    damagePopups.add(new DamagePopup(player.getX(), player.getY(), dmg, Color.ORANGE));
                    System.out.println("Player hit by laser! Health: " + player.getHealth() + " (damage: " + dmg + ")");
                    
                    if (player.getHealth() <= 0) {
                        System.out.println("Game Over! Final Score: " + score);
                        gameRunning = false;
                        // ลบ Player2 ออกเมื่อ Game Over
                        if (coopMode && player2 != null) {
                            player2 = null;
                            coopMode = false;
                        }
                    }
                }
            }
        }
        
        // Update damage popups
        Iterator<DamagePopup> popupIterator = damagePopups.iterator();
        while (popupIterator.hasNext()) {
            DamagePopup popup = popupIterator.next();
            popup.update(delta);
            if (popup.isExpired()) {
                popupIterator.remove();
            }
        }
        
        // Update explosion particles
        Iterator<ExplosionParticle> particleIterator = explosionParticles.iterator();
        while (particleIterator.hasNext()) {
            ExplosionParticle particle = particleIterator.next();
            particle.update();
            if (particle.isExpired()) {
                particleIterator.remove();
            }
        }
        
        // Update boss death animation
        if (bossDeathAnimationActive) {
            long elapsed = System.currentTimeMillis() - bossDeathStartTime;
            
            // Continuously spawn explosion particles during death animation
            for (int i = 0; i < 3; i++) { // Spawn 3 particles per frame
                spawnBossExplosionParticle();
            }
            
            // Fade boss sprite over 8 seconds
            bossDeathAlpha = Math.max(0f, 1f - ((float)elapsed / (float)BOSS_DEATH_DURATION));
            
            // End animation after duration
            if (elapsed >= BOSS_DEATH_DURATION) {
                bossDeathAnimationActive = false;
                boss = null; // Finally remove boss
            }
        }

        
        // Auto-shoot (adjusted for manual mode)
        long currentFireRate = player.getFireRate();
        if (manualControlMode) {
            // 1.5x faster in manual mode
            currentFireRate = (long) (currentFireRate / 1.5);
        }
        
        if (System.currentTimeMillis() - player.getLastShotTime() > currentFireRate) {
            shootBullet();
            player.setLastShotTime(System.currentTimeMillis());
            System.out.println("Bullet fired! Total bullets: " + bullets.size());
        }
        
        // Player2 auto-shoot (if co-op mode active) - ยิงอัตโนมัติเหมือน player1
        if (coopMode && player2 != null) {
            long p2FireRate = player2.getFireRate();
            if (manualControlMode) {
                p2FireRate = (long) (p2FireRate / 1.5);
            }
            
            if (System.currentTimeMillis() - player2.getLastShotTime() > p2FireRate) {
                shootBulletPlayer2();
                player2.setLastShotTime(System.currentTimeMillis());
            }
        }
        
        // Update power-ups
        Iterator<PowerUp> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();
            
            // Check collision with player1
            if (powerUp.collidesWith(player)) {
                applyPowerUp(powerUp, player);
                powerUpIterator.remove();
                System.out.println("Player 1 collected power-up: " + powerUp.getType());
            } 
            // Check collision with player2 (co-op mode)
            else if (coopMode && player2 != null && powerUp.collidesWith(player2)) {
                applyPowerUp(powerUp, player2);
                powerUpIterator.remove();
                System.out.println("Player 2 collected power-up: " + powerUp.getType());
            } 
            else if (powerUp.isExpired()) {
                powerUpIterator.remove();
            }
        }
        
        // Increase difficulty over time
        long currentElapsed = System.currentTimeMillis() - gameStartTime;
        int newLevel = (int) (currentElapsed / 20000) + 1; // New level every 20 seconds
        if (newLevel > level) {
            level = newLevel;
            System.out.println("Level up! Now at level: " + level);
        }
        enemySpawnRate = Math.max(200, 1000 - (level * 100)); // Faster spawning over time
    }
    
    private void updateCamera() {
        // Center camera on player
        cameraX = player.getX() - SCREEN_WIDTH / 2;
        cameraY = player.getY() - SCREEN_HEIGHT / 2;
        
        // Keep camera within world bounds
        if (cameraX < 0) cameraX = 0;
        if (cameraX > WORLD_WIDTH - SCREEN_WIDTH) cameraX = WORLD_WIDTH - SCREEN_WIDTH;
        if (cameraY < 0) cameraY = 0;
        if (cameraY > WORLD_HEIGHT - SCREEN_HEIGHT) cameraY = WORLD_HEIGHT - SCREEN_HEIGHT;
    }
    
    private void updatePlayerMovement() {
        if (manualControlMode) {
            // Manual mode: WASD = movement, Arrow keys = rotation
            int dirX = 0, dirY = 0;
            if (leftPressed) dirX -= 1;
            if (rightPressed) dirX += 1;

            if (upPressed) dirY -= 1;
            if (downPressed) dirY += 1;

            // Inform player of input direction
            player.move(dirX, dirY, WORLD_WIDTH, WORLD_HEIGHT);
            
            // Arrow keys control rotation manually
            if (arrowLeftPressed || arrowRightPressed || arrowUpPressed || arrowDownPressed) {
                double targetAngle = player.getFacingAngle();
                
                // Calculate target angle from arrow keys
                if (arrowUpPressed && !arrowDownPressed && !arrowLeftPressed && !arrowRightPressed) {
                    targetAngle = -Math.PI / 2; // Up
                } else if (arrowDownPressed && !arrowUpPressed && !arrowLeftPressed && !arrowRightPressed) {
                    targetAngle = Math.PI / 2; // Down
                } else if (arrowLeftPressed && !arrowRightPressed && !arrowUpPressed && !arrowDownPressed) {
                    targetAngle = Math.PI; // Left
                } else if (arrowRightPressed && !arrowLeftPressed && !arrowUpPressed && !arrowDownPressed) {
                    targetAngle = 0; // Right
                } else if (arrowUpPressed && arrowRightPressed) {
                    targetAngle = -Math.PI / 4; // Up-Right
                } else if (arrowUpPressed && arrowLeftPressed) {
                    targetAngle = -3 * Math.PI / 4; // Up-Left
                } else if (arrowDownPressed && arrowRightPressed) {
                    targetAngle = Math.PI / 4; // Down-Right
                } else if (arrowDownPressed && arrowLeftPressed) {
                    targetAngle = 3 * Math.PI / 4; // Down-Left
                }
                
                player.setFacingAngle(targetAngle);
            }
        } else {
            // Auto mode: WASD/Arrows = movement, auto-aim at enemies
            int dirX = 0, dirY = 0;
            if (leftPressed) dirX -= 1;
            if (rightPressed) dirX += 1;

            if (upPressed) dirY -= 1;
            if (downPressed) dirY += 1;

            // Inform player of input direction; actual movement integrated in Player.integrateMovement
            player.move(dirX, dirY, WORLD_WIDTH, WORLD_HEIGHT);
        }
    }
    
    private void updatePlayer2Movement() {
        if (player2 == null) return;
        
        // Player 2 controlled by arrow keys only (no Shift needed for movement)
        int dirX = 0, dirY = 0;
        if (arrowLeftPressed) dirX -= 1;
        if (arrowRightPressed) dirX += 1;

        if (arrowUpPressed) dirY -= 1;
        if (arrowDownPressed) dirY += 1;

        // Inform player2 of input direction
        player2.move(dirX, dirY, WORLD_WIDTH, WORLD_HEIGHT);
        
        // Player2 uses same auto-aim as player1 in auto mode
        // (rotation handled in player.update())
    }
    
    private void handleBossAttacks(long delta) {
        if (boss == null || boss.isDead()) return;
        
        Boss.AttackPhase phase = boss.getCurrentPhase();
        
        switch (phase) {
            case BARRAGE:
                // Shoot bullets in grid pattern toward player
                if (boss.getBarrageTimer() >= boss.getBarrageShotInterval()) {
                    int bx = (int)boss.getX();
                    int by = (int)boss.getY();
                    int px = player.getX();
                    int py = player.getY();
                    
                    // Create 3 bullets in a horizontal line
                    int spacing = 40;
                    for (int i = -1; i <= 1; i++) {
                        int offsetX = i * spacing;
                        bossBullets.add(new BossBullet(
                            bx + offsetX, by, 
                            px + offsetX * 2, py, 
                            15, 
                            Color.ORANGE
                        ));
                    }
                    
                    boss.resetBarrageTimer();
                    boss.incrementBarrageShots();
                    System.out.println("Boss barrage shot #" + boss.getBarrageShots());
                }
                break;
                
            case LASER_SPIN:
                // Create 6 lasers if not yet created
                if (!bossLasersCreated) {
                    bossLasers.clear();
                    for (int i = 0; i < 6; i++) {
                        double angle = (Math.PI * 2 / 6) * i;
                        BossLaser laser = new BossLaser(
                            boss.getX(), boss.getY(),
                            angle,
                            3000, // ยาวเท่าแมพ (WORLD_WIDTH)
                            25,   // damage เพิ่มเป็น 17-30/0.5s (avg ~23.5)
                            Color.CYAN,
                            boss.getHitboxRadius() // ใช้ hitbox ของบอส
                        );
                        bossLasers.add(laser);
                    }
                    
                    // Random rotation direction
                    laserRotationDirection = random.nextBoolean() ? 1.0 : -1.0;
                    System.out.println("Boss lasers created, rotation: " + (laserRotationDirection > 0 ? "clockwise" : "counter-clockwise"));
                    bossLasersCreated = true;
                }
                
                // Start rotation after 2 second warmup
                for (BossLaser laser : bossLasers) {
                    if (laser.isWarmedUp() && !laser.equals(bossLasers.get(0))) {
                        // Check if rotation already started
                    }
                    if (laser.isWarmedUp()) {
                        double rotationSpeed = laserRotationDirection * Math.toRadians(30); // 30 deg/s
                        laser.startRotation(rotationSpeed);
                    }
                }
                break;
                
            case HOMING:
                // Fire 3 homing bullets around boss every 2 seconds
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastHomingBulletSpawn >= HOMING_SPAWN_INTERVAL) {
                    // Spawn 3 bullets in circle around boss
                    for (int i = 0; i < 3; i++) {
                        double spawnAngle = (Math.PI * 2 / 3) * i; // 120 degrees apart
                        double spawnDist = 50; // ระยะจากบอส
                        double spawnX = boss.getX() + Math.cos(spawnAngle) * spawnDist;
                        double spawnY = boss.getY() + Math.sin(spawnAngle) * spawnDist;
                        
                        bossBullets.add(new BossBullet(
                            spawnX, spawnY,
                            10, // damage
                            Color.YELLOW,
                            true, // isHoming
                            1    // maxRedirects
                        ));
                    }
                    lastHomingBulletSpawn = currentTime;
                    System.out.println("Boss fired 3 homing bullets!");
                }
                break;
        }
        
        // Clear lasers when not in laser phase
        if (phase != Boss.AttackPhase.LASER_SPIN && bossLasersCreated) {
            bossLasers.clear();
            bossLasersCreated = false;
        }
    }
    
    private void spawnEnemy() {
        // Spawn enemies around the camera view (off screen)
        int side = random.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
        int x, y;
        
        switch (side) {
            case 0: // Top
                x = cameraX + random.nextInt(SCREEN_WIDTH);
                y = cameraY - 50;
                break;
            case 1: // Right
                x = cameraX + SCREEN_WIDTH + 50;
                y = cameraY + random.nextInt(SCREEN_HEIGHT);
                break;
            case 2: // Bottom
                x = cameraX + random.nextInt(SCREEN_WIDTH);
                y = cameraY + SCREEN_HEIGHT + 50;
                break;
            default: // Left
                x = cameraX - 50;
                y = cameraY + random.nextInt(SCREEN_HEIGHT);
                break;
        }
        
        // Make sure enemy is within world bounds
        if (x >= 0 && x < WORLD_WIDTH && y >= 0 && y < WORLD_HEIGHT) {
            // Count current TYPE1 enemies
            int type1Count = 0;
            for (Enemy e : enemies) {
                if (e.getType() == Enemy.EnemyType.TYPE1) {
                    type1Count++;
                }
            }
            
            // Weighted random: TYPE1 rare (5%), TYPE3 common (80%), TYPE2 (15%)
            int roll = random.nextInt(100);
            Enemy.EnemyType type;
            if (roll < 5 && type1Count < type1MaxCap) { // 5% chance and cap not reached
                type = Enemy.EnemyType.TYPE1;
            } else if (roll < 20) { // 15% TYPE2
                type = Enemy.EnemyType.TYPE2;
            } else { // 80% TYPE3
                type = Enemy.EnemyType.TYPE3;
            }
            // Use player's visible sprite size (player draws at 2x hitbox)
            int playerSpriteW = player.getWidth() * 2;
            int playerSpriteH = player.getHeight() * 2;
            Enemy spawned = new Enemy(x, y, type, playerSpriteW, playerSpriteH);
            enemies.add(spawned);
            System.out.println("Spawned enemy type=" + type + " size=" + spawned.getWidth() + "x" + spawned.getHeight() + " (TYPE1 count: " + (type == Enemy.EnemyType.TYPE1 ? type1Count + 1 : type1Count) + "/" + type1MaxCap + ")");
        }
    }
    
    private void spawnBoss() {
        System.out.println("=== BOSS SPAWNED ===");
        // Spawn boss near player
        int spawnX = (int)(player.getX() + 200 + random.nextDouble() * 300); // ด้านขวา player 200-500 px
        int spawnY = (int)(player.getY() - 200 + random.nextDouble() * 400); // บนหรือล่าง player ±200 px
        
        // จำกัดให้อยู่ในขอบเขตแมพ
        spawnX = Math.max(100, Math.min(WORLD_WIDTH - 100, spawnX));
        spawnY = Math.max(100, Math.min(WORLD_HEIGHT - 100, spawnY));
        
        boss = new Boss(spawnX, spawnY);
    }
    
    private void toggleCoopMode() {
        coopMode = !coopMode;
        
        if (coopMode) {
            // เปิด co-op mode: สร้าง player2 ด้านซ้ายของ player1 โดยมีสเตทเท่ากับ player1
            int p1Type = player.getSpacecraftType();
            int p1MaxHP = player.getMaxHealth();
            int p1Speed = player.getSpeed();
            int p1Firerate = player.getFireRateRPM(); // shots per minute
            
            // Player 2 มีสเตทเท่ากับ player 1 ทุกอย่าง
            int p2HP = p1MaxHP;
            int p2Speed = p1Speed;
            int p2Firerate = p1Firerate;
            
            // Spawn player2 ห่างจาก player1 50px ทางซ้าย
            player2 = new Player((int)player.getX() - 50, (int)player.getY(), 
                                p1Type, p2HP, p2Speed, p2Firerate);
            
            System.out.println("Co-op mode: ON (Player 2 created with same stats as Player 1)");
        } else {
            // ปิด co-op mode: รวม player2 กลับเข้า player1
            player2 = null;
            bullets2.clear();
            System.out.println("Co-op mode: OFF (Player 2 removed)");
        }
    }
    
    private void spawnBossExplosionParticle() {
        // Spawn explosion particles around boss death location (similar to old system)
        int spread = 60;
        int px = bossFinalX + random.nextInt(spread) - spread/2;
        int py = bossFinalY + random.nextInt(spread) - spread/2;
        
        explosionParticles.add(new ExplosionParticle(px, py));
    }
    
    private void spawnBossEnemies() {
        // Spawn 5-10 enemies, รวมทั้งสามประเภท, TYPE1 สูงสุด 2 ตัว
        int totalEnemies = 5 + random.nextInt(6); // 5-10
        int type1Count = 0;
        int maxType1 = 2;
        
        System.out.println("Boss spawning " + totalEnemies + " enemies!");
        
        for (int i = 0; i < totalEnemies; i++) {
            // สุ่มประเภท (0=TYPE1, 1=TYPE2, 2=TYPE3)
            int typeChoice;
            if (type1Count >= maxType1) {
                // ถ้า TYPE1 ครบแล้ว สุ่มเฉพาะ TYPE2 หรือ TYPE3
                typeChoice = 1 + random.nextInt(2); // 1 หรือ 2
            } else {
                typeChoice = random.nextInt(3); // 0, 1, หรือ 2
            }
            
            Enemy.EnemyType type;
            switch (typeChoice) {
                case 0:
                    type = Enemy.EnemyType.TYPE1;
                    type1Count++;
                    break;
                case 1:
                    type = Enemy.EnemyType.TYPE2;
                    break;
                default:
                    type = Enemy.EnemyType.TYPE3;
                    break;
            }
            
            // สุ่มตำแหน่ง spawn รอบๆ boss
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 150 + random.nextDouble() * 100; // 150-250 px จาก boss
            int spawnX = (int)(boss.getX() + Math.cos(angle) * distance);
            int spawnY = (int)(boss.getY() + Math.sin(angle) * distance);
            
            // จำกัดให้อยู่ในขอบเขตแมพ
            spawnX = Math.max(30, Math.min(WORLD_WIDTH - 30, spawnX));
            spawnY = Math.max(30, Math.min(WORLD_HEIGHT - 30, spawnY));
            
            enemies.add(new Enemy(spawnX, spawnY, type));
        }
        
        System.out.println("Spawned: TYPE1=" + type1Count + ", Total=" + totalEnemies);
    }
    
    private void shootEnemyBullets(Enemy enemy) {
        int ex = enemy.getX();
        int ey = enemy.getY();
        int px = player.getX();
        int py = player.getY();
        
        switch (enemy.getType()) {
            case TYPE1:
                // Start laser charging
                enemy.startLaser(px, py);
                System.out.println("Enemy TYPE1 started laser charging!");
                break;
            case TYPE3:
                // Shoot 1 bullet toward player
                enemyBullets.add(new EnemyBullet(ex, ey, px, py));
                System.out.println("Enemy TYPE3 fired 1 bullet! Total: " + enemyBullets.size());
                break;
                
            case TYPE2:
                // Shoot 6 bullets in circle pattern, each accelerating toward player
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2.0 / 6.0) * i;
                    enemyBullets.add(new EnemyBullet(ex, ey, angle, px, py));
                }
                System.out.println("Enemy TYPE2 fired 6 bullets! Total: " + enemyBullets.size());
                break;
        }
    }
    
    private void shootBullet() {
        if (manualControlMode) {
            // Manual mode: shoot in facing direction with 1.5x damage
            double fireAngle = player.getFacingAngle();
            double fireSpeed = 10.0;
            double damageMultiplier = 1.5;
            Bullet bullet = new Bullet(player.getX(), player.getY(), fireAngle, fireSpeed, damageMultiplier);
            bullet.setColor(new Color(0, 191, 255)); // สีฟ้า (Deep Sky Blue - RGB)
            bullets.add(bullet);
            // Play sound after creating bullet
            playBulletSound();
        } else {
            // Auto mode: aim at nearest target (enemy or boss)
            Object nearestTarget = null;
            double nearestDistance = Double.MAX_VALUE;
            
            // Check enemies
            for (Enemy enemy : enemies) {
                double distance = Math.sqrt(Math.pow(enemy.getX() - player.getX(), 2) + 
                                         Math.pow(enemy.getY() - player.getY(), 2));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = enemy;
                }
            }
            
            // Check boss
            if (boss != null && !boss.isDead()) {
                double distance = Math.sqrt(Math.pow(boss.getX() - player.getX(), 2) + 
                                         Math.pow(boss.getY() - player.getY(), 2));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = boss;
                }
            }
            
            if (nearestTarget != null) {
                double targetX, targetY;
                if (nearestTarget instanceof Enemy) {
                    Enemy enemy = (Enemy) nearestTarget;
                    targetX = enemy.getX();
                    targetY = enemy.getY();
                } else {
                    Boss bossTarget = (Boss) nearestTarget;
                    targetX = bossTarget.getX();
                    targetY = bossTarget.getY();
                }
                
                // Compute desired angle and set as player's target facing
                double desiredAngle = Math.atan2(targetY - player.getY(), targetX - player.getX());
                player.setFacingAngle(desiredAngle);

                // Fire using current facingAngle with Blue color (RGB)
                double fireAngle = player.getFacingAngle();
                double fireSpeed = 10.0;
                Bullet bullet = new Bullet(player.getX(), player.getY(), fireAngle, fireSpeed);
                bullet.setColor(new Color(0, 191, 255)); // สีฟ้า (Deep Sky Blue - RGB)
                bullets.add(bullet);
                // Play sound after creating bullet
                playBulletSound();
            }
        }
    }
    
    private void shootBulletPlayer2() {
        if (player2 == null) return;
        
        // Player2 always uses auto-aim (same as player1 in auto mode)
        Object nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        
        // Check enemies
        for (Enemy enemy : enemies) {
            double distance = Math.sqrt(Math.pow(enemy.getX() - player2.getX(), 2) + 
                                     Math.pow(enemy.getY() - player2.getY(), 2));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = enemy;
            }
        }
        
        // Check boss
        if (boss != null && !boss.isDead()) {
            double distance = Math.sqrt(Math.pow(boss.getX() - player2.getX(), 2) + 
                                     Math.pow(boss.getY() - player2.getY(), 2));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = boss;
            }
        }
        
        if (nearestTarget != null) {
            double targetX, targetY;
            if (nearestTarget instanceof Enemy) {
                Enemy enemy = (Enemy) nearestTarget;
                targetX = enemy.getX();
                targetY = enemy.getY();
            } else {
                Boss bossTarget = (Boss) nearestTarget;
                targetX = bossTarget.getX();
                targetY = bossTarget.getY();
            }
            
            // Compute desired angle and set as player2's target facing
            double desiredAngle = Math.atan2(targetY - player2.getY(), targetX - player2.getX());
            player2.setFacingAngle(desiredAngle);

            // Fire using current facingAngle with Pink-Red color (RGB)
            double fireAngle = player2.getFacingAngle();
            double fireSpeed = 10.0;
            Bullet bullet = new Bullet(player2.getX(), player2.getY(), fireAngle, fireSpeed);
            bullet.setColor(new Color(255, 38, 71)); // สีแดงออกชมพู (RGB)
            bullets2.add(bullet);
            // Play sound after creating bullet
            playBulletSound();
        }
    }
    
    private void applyPowerUp(PowerUp powerUp, Player targetPlayer) {
        switch (powerUp.getType()) {
            case HEALTH:
                targetPlayer.heal(20);
                break;
            case SPEED:
                targetPlayer.increaseSpeed(1);
                break;
            case FIRE_RATE:
                targetPlayer.increaseFireRate(50);
                break;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Apply scaling and centering if in fullscreen mode
        if (fullscreen) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            
            // Calculate scale to fit screen while maintaining aspect ratio
            double scaleX = (double) panelWidth / SCREEN_WIDTH;
            double scaleY = (double) panelHeight / SCREEN_HEIGHT;
            double scale = Math.min(scaleX, scaleY);
            
            // Calculate offset to center
            int offsetX = (int) ((panelWidth - SCREEN_WIDTH * scale) / 2);
            int offsetY = (int) ((panelHeight - SCREEN_HEIGHT * scale) / 2);
            
            // Apply transformations
            g2d.translate(offsetX, offsetY);
            g2d.scale(scale, scale);
        }
        
        switch (currentState) {
            case MENU:
                drawMainMenu(g2d);
                break;
            case SPACECRAFT_SELECT:
                drawSpacecraftSelect(g2d);
                break;
            case OPTIONS:
                drawOptionsMenu(g2d);
                break;
            case EXIT_CONFIRM:
                drawExitConfirm(g2d);
                break;
            case GAME:
                if (gameRunning) {
                    // Apply camera transformation
                    g2d.translate(-cameraX, -cameraY);
                    
                    // Draw stars background
                    drawStars(g2d);
                    
                    // Draw game objects in world space
                    player.draw(g2d);
                    
                    // Draw player2 if co-op mode active
                    if (coopMode && player2 != null) {
                        player2.draw(g2d);
                    }
                    
                    for (Enemy enemy : enemies) {
                        enemy.draw(g2d);
                    }
                    
                    for (Bullet bullet : bullets) {
                        bullet.draw(g2d);
                    }
                    
                    // Draw player2 bullets if co-op mode active
                    if (coopMode && player2 != null) {
                        for (Bullet bullet : bullets2) {
                            bullet.draw(g2d);
                        }
                    }
                    
                    for (EnemyBullet enemyBullet : enemyBullets) {
                        enemyBullet.draw(g2d);
                    }
                    
                    // Draw boss bullets
                    for (BossBullet bossBullet : bossBullets) {
                        bossBullet.draw(g2d);
                    }
                    
                    // Draw boss lasers
                    for (BossLaser laser : bossLasers) {
                        laser.draw(g2d);
                    }
                    
                    for (PowerUp powerUp : powerUps) {
                        powerUp.draw(g2d);
                    }
                    
                    // Draw Boss (with fade effect if dying)
                    if (boss != null) {
                        if (bossDeathAnimationActive) {
                            // Draw fading boss during death animation
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bossDeathAlpha));
                            boss.draw(g2d);
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        } else if (!boss.isDead()) {
                            boss.draw(g2d);
                        }
                    }
                    
                    // Draw damage popups
                    for (DamagePopup popup : damagePopups) {
                        popup.draw(g2d);
                    }
                    
                    // Draw explosion particles
                    for (ExplosionParticle particle : explosionParticles) {
                        particle.draw(g2d);
                    }
                    
                    // Reset camera transformation for UI
                    g2d.translate(cameraX, cameraY);
                    
                    // Draw UI (in screen space)
                    drawUI(g2d);
                } else {
                    // Game over - draw stars background (no camera offset)
                    drawStars(g2d);
                    // Draw game over screen in screen space (no translation needed)
                    drawGameOver(g2d);
                }
                break;
            
            case PAUSED:
                // Draw game in background (frozen)
                if (gameRunning || !gameRunning) { // Draw regardless
                    AffineTransform old = g2d.getTransform();
                    g2d.translate(-cameraX, -cameraY);
                    
                    // Draw stars background
                    drawStars(g2d);
                    
                    // Draw game objects in world space
                    player.draw(g2d);
                    
                    for (Enemy enemy : enemies) {
                        enemy.draw(g2d);
                    }
                    
                    for (Bullet bullet : bullets) {
                        bullet.draw(g2d);
                    }
                    
                    for (EnemyBullet enemyBullet : enemyBullets) {
                        enemyBullet.draw(g2d);
                    }
                    
                    // Draw boss bullets
                    for (BossBullet bossBullet : bossBullets) {
                        bossBullet.draw(g2d);
                    }
                    
                    // Draw boss lasers
                    for (BossLaser laser : bossLasers) {
                        laser.draw(g2d);
                    }
                    
                    for (PowerUp powerUp : powerUps) {
                        powerUp.draw(g2d);
                    }
                    
                    // Draw Boss (with fade effect if dying)
                    if (boss != null) {
                        if (bossDeathAnimationActive) {
                            // Draw fading boss during death animation
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bossDeathAlpha));
                            boss.draw(g2d);
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        } else if (!boss.isDead()) {
                            boss.draw(g2d);
                        }
                    }
                    
                    // Draw damage popups
                    for (DamagePopup popup : damagePopups) {
                        popup.draw(g2d);
                    }
                    
                    // Draw explosion particles
                    for (ExplosionParticle particle : explosionParticles) {
                        particle.draw(g2d);
                    }
                    
                    g2d.setTransform(old);
                }
                
                // Draw dark overlay
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
                
                // Draw pause menu
                drawPauseMenu(g2d);
                break;
        }
    }
    
    private void drawStars(Graphics2D g2d) {
        // Draw simple stars that are always visible
        g2d.setColor(Color.WHITE);
        
        for (int i = 0; i < STAR_COUNT; i++) {
            // Stars in world coordinates (will be affected by camera transform)
            int size = starZ[i];
            if (size == 1) {
                g2d.setColor(Color.GRAY);
            } else if (size == 2) {
                g2d.setColor(Color.LIGHT_GRAY); 
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillOval(starX[i], starY[i], size, size);
        }
    }
    
    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        if (coopMode && player2 != null) {
            // Co-op mode: แสดง UI ทั้ง 2 players แบบเหมือนกัน
            // Player 1 UI - ซ้ายบน
            drawPlayerUI(g2d, player, 10, true, "Player 1 (WASD)", "F");
            
            // Player 2 UI - ขวาบน (copy มาจาก Player 1)
            drawPlayerUI(g2d, player2, SCREEN_WIDTH - 220, false, "Player 2 (Arrows)", "R-Shift");
            
            // Timer countdown (กลาง) - 3.5 minutes = 210 seconds
            long elapsedMs = System.currentTimeMillis() - gameStartTime;
            long remainingMs = Math.max(0, BOSS_SPAWN_TIME - elapsedMs);
            int remainingSeconds = (int)(remainingMs / 1000);
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            
            if (!bossSpawned) {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String timerText = String.format("Boss in: %d:%02d", minutes, seconds);
                g2d.drawString(timerText, SCREEN_WIDTH / 2 - 70, 25);
            } else {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString("BOSS FIGHT!", SCREEN_WIDTH / 2 - 60, 25);
            }
        } else {
            // Solo mode: แสดง UI แบบเดิม
            // ข้อมูลด้านซ้ายบน (Player 1)
            g2d.drawString("Score: " + score, 10, 25);
            g2d.drawString("Level: " + level, 10, 45);
            g2d.drawString("Health: " + player.getHealth(), 10, 65);
            
            // Timer countdown (3.5 minutes = 210 seconds)
            long elapsedMs = System.currentTimeMillis() - gameStartTime;
            long remainingMs = Math.max(0, BOSS_SPAWN_TIME - elapsedMs);
            int remainingSeconds = (int)(remainingMs / 1000);
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            
            if (!bossSpawned) {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String timerText = String.format("Boss in: %d:%02d", minutes, seconds);
                g2d.drawString(timerText, SCREEN_WIDTH / 2 - 70, 25);
            } else {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString("BOSS FIGHT!", SCREEN_WIDTH / 2 - 60, 25);
            }
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.WHITE);
            
            // Health bar (scale based on player's max health)
            int hbX = 10;
            int hbY = 75;
            int hbW = 200;
            int hbH = 10;
            g2d.setColor(Color.RED);
            int healthW = (int) Math.round(((double)player.getHealth() / (double)player.getMaxHealth()) * hbW);
            g2d.fillRect(hbX, hbY, healthW, hbH);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(hbX, hbY, hbW, hbH);

            // Shield bar (draw only while shield currently > 0)
            if (player.getShieldCurrent() > 0 && player.getShieldMax() > 0) {
                int sx = 10;
                int sy = 90;
                int sw = 200;
                int sh = 8;
                g2d.setColor(Color.BLUE);
                int shieldW = (int) Math.round(((double)player.getShieldCurrent() / (double)player.getShieldMax()) * sw);
                g2d.fillRect(sx, sy, shieldW, sh);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(sx, sy, sw, sh);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Shield: " + player.getShieldCurrent() + "/" + player.getShieldMax(), sx + sw + 6, sy + sh);
            }

            // Special cooldown display
            long lastUse = player.getLastSpecialUseTime();
            long cd = player.getSpecialCooldownMs();
            long now = System.currentTimeMillis();
            long remaining = Math.max(0, cd - (now - lastUse));
            int cooldownSeconds = (int) Math.ceil(remaining / 1000.0);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.setColor(remaining > 0 ? Color.GRAY : Color.GREEN);
            g2d.drawString("Special (F): " + (remaining > 0 ? (cooldownSeconds + "s") : "Ready"), 10, 115);
            
            // Manual mode indicator
            if (manualControlMode) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("MANUAL MODE", 10, 140);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.setColor(Color.ORANGE);
                g2d.drawString("WASD: Move | Arrows: Aim", 10, 155);
                g2d.drawString("Damage x1.5 | Fire Rate x1.5", 10, 170);
            } else {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Press M for Manual Mode", 10, 140);
            }
        }
    }
    
    // Helper method to draw UI for one player
    private void drawPlayerUI(Graphics2D g2d, Player p, int startX, boolean isLeft, String playerName, String specialKey) {
        Color mainColor = isLeft ? Color.WHITE : new Color(255, 105, 180); // ขาว หรือ ชมพู
        Color healthBarColor = isLeft ? Color.RED : new Color(255, 50, 100);
        Color shieldBarColor = Color.BLUE;
        Color specialReadyColor = Color.GREEN;
        
        g2d.setColor(mainColor);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Player name & stats
        g2d.drawString(playerName, startX, 25);
        g2d.drawString("Score: " + score, startX, 45);
        g2d.drawString("Level: " + level, startX, 65);
        g2d.drawString("HP: " + p.getHealth() + "/" + p.getMaxHealth(), startX, 85);
        
        // Health bar
        int hbX = startX;
        int hbY = 95;
        int hbW = 200;
        int hbH = 10;
        g2d.setColor(healthBarColor);
        int healthW = (int) Math.round(((double)p.getHealth() / (double)p.getMaxHealth()) * hbW);
        g2d.fillRect(hbX, hbY, healthW, hbH);
        g2d.setColor(mainColor);
        g2d.drawRect(hbX, hbY, hbW, hbH);
        
        // Shield bar (if active)
        if (p.getShieldCurrent() > 0 && p.getShieldMax() > 0) {
            int sx = startX;
            int sy = 110;
            int sw = 200;
            int sh = 8;
            g2d.setColor(shieldBarColor);
            int shieldW = (int) Math.round(((double)p.getShieldCurrent() / (double)p.getShieldMax()) * sw);
            g2d.fillRect(sx, sy, shieldW, sh);
            g2d.setColor(mainColor);
            g2d.drawRect(sx, sy, sw, sh);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Shield: " + p.getShieldCurrent() + "/" + p.getShieldMax(), sx, sy + sh + 12);
        }
        
        // Special cooldown
        long lastUse = p.getLastSpecialUseTime();
        long cd = p.getSpecialCooldownMs();
        long now = System.currentTimeMillis();
        long remaining = Math.max(0, cd - (now - lastUse));
        int cooldownSeconds = (int) Math.ceil(remaining / 1000.0);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(remaining > 0 ? Color.GRAY : specialReadyColor);
        int specialY = (p.getShieldCurrent() > 0) ? 135 : 125;
        g2d.drawString("Special (" + specialKey + "): " + (remaining > 0 ? (cooldownSeconds + "s") : "Ready"), startX, specialY);
        
        g2d.setColor(Color.WHITE);
    }
    
    private void drawMainMenu(Graphics2D g2d) {
        // Draw background
        drawStars(g2d);
        
        // Draw title
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics titleFm = g2d.getFontMetrics();
    String title = "Beyond the Red Eclipse: Exodus";
        int titleX = (SCREEN_WIDTH - titleFm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 150);
        
        // Draw subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.ITALIC, 16));
        FontMetrics subtitleFm = g2d.getFontMetrics();
        String subtitle = "...";
        int subtitleX = (SCREEN_WIDTH - subtitleFm.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subtitleX, 180);
        
        // Draw menu options
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics menuFm = g2d.getFontMetrics();
        
        for (int i = 0; i < mainMenuOptions.length; i++) {
            if (i == selectedMenuOption) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("> " + mainMenuOptions[i] + " <", (SCREEN_WIDTH - menuFm.stringWidth("> " + mainMenuOptions[i] + " <")) / 2, 280 + i * 50);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.drawString(mainMenuOptions[i], (SCREEN_WIDTH - menuFm.stringWidth(mainMenuOptions[i])) / 2, 280 + i * 50);
            }
        }
        
        // Draw controls
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
    g2d.drawString("Use ↑↓ to navigate, ENTER to select/change, ESC to go back", 10, SCREEN_HEIGHT - 20);
    }
    
    private void drawOptionsMenu(Graphics2D g2d) {
        // Draw background
        drawStars(g2d);
        
        // Draw title
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics titleFm = g2d.getFontMetrics();
        String title = "OPTIONS";
        int titleX = (SCREEN_WIDTH - titleFm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 150);
        
        // Draw options
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics optionsFm = g2d.getFontMetrics();
        
        for (int i = 0; i < optionsMenuItems.length; i++) {
            if (i == selectedOptionsItem) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("> " + optionsMenuItems[i] + " <", (SCREEN_WIDTH - optionsFm.stringWidth("> " + optionsMenuItems[i] + " <")) / 2, 250 + i * 40);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.drawString(optionsMenuItems[i], (SCREEN_WIDTH - optionsFm.stringWidth(optionsMenuItems[i])) / 2, 250 + i * 40);
            }
        }
        
        // Draw controls
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Use ↑↓ to navigate, ←→ to adjust volume, ENTER to toggle fullscreen, ESC to back", 10, SCREEN_HEIGHT - 20);
    }
    
    private void drawExitConfirm(Graphics2D g2d) {
        // Draw background
        drawStars(g2d);
        
        // Draw confirmation message
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        FontMetrics titleFm = g2d.getFontMetrics();
        String title = "EXIT GAME?";
        int titleX = (SCREEN_WIDTH - titleFm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, SCREEN_HEIGHT / 2 - 50);
        
        // Draw options
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics optionsFm = g2d.getFontMetrics();
        
        String[] exitOptions = {"Yes", "No"};
        for (int i = 0; i < exitOptions.length; i++) {
            if (i == selectedMenuOption) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("> " + exitOptions[i] + " <", (SCREEN_WIDTH - optionsFm.stringWidth("> " + exitOptions[i] + " <")) / 2, SCREEN_HEIGHT / 2 + 20 + i * 40);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.drawString(exitOptions[i], (SCREEN_WIDTH - optionsFm.stringWidth(exitOptions[i])) / 2, SCREEN_HEIGHT / 2 + 20 + i * 40);
            }
        }
        
        // Draw controls
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Use ↑↓ to navigate, ENTER to confirm, ESC to cancel", 10, SCREEN_HEIGHT - 20);
    }
    
    private void drawPauseMenu(Graphics2D g2d) {
        // Draw title
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics titleFm = g2d.getFontMetrics();
        String title = "PAUSED";
        int titleX = (SCREEN_WIDTH - titleFm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, SCREEN_HEIGHT / 2 - 100);
        
        // Draw options
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics optionsFm = g2d.getFontMetrics();
        
        for (int i = 0; i < pauseMenuOptions.length; i++) {
            if (i == selectedMenuOption) {
                g2d.setColor(Color.YELLOW);
                g2d.drawString("> " + pauseMenuOptions[i] + " <", (SCREEN_WIDTH - optionsFm.stringWidth("> " + pauseMenuOptions[i] + " <")) / 2, SCREEN_HEIGHT / 2 + i * 50);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.drawString(pauseMenuOptions[i], (SCREEN_WIDTH - optionsFm.stringWidth(pauseMenuOptions[i])) / 2, SCREEN_HEIGHT / 2 + i * 50);
            }
        }
        
        // Draw controls
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Use ↑↓ to navigate, ENTER to select, ESC to resume", 10, SCREEN_HEIGHT - 20);
    }
    
    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String gameOverText = "GAME OVER";
        int x = (SCREEN_WIDTH - fm.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, x, SCREEN_HEIGHT / 2 - 50);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g2d.getFontMetrics();
        String scoreText = "Final Score: " + score;
        x = (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, x, SCREEN_HEIGHT / 2);
        
        String restartText = "Press R to Restart or ESC for Menu";
        x = (SCREEN_WIDTH - fm.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, x, SCREEN_HEIGHT / 2 + 50);
    }
    
    private void drawSpacecraftSelect(Graphics2D g2d) {
        // Draw background
        drawStars(g2d);
        
        // Draw title
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics titleFm = g2d.getFontMetrics();
        String title = "SELECT SPACECRAFT";
        int titleX = (SCREEN_WIDTH - titleFm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 100);
        
        // Draw spacecraft options horizontally
        String[] shipNames = {"LARGE", "MEDIUM", "SMALL"};
        int spacing = SCREEN_WIDTH / 4;
        int baseX = spacing;
        int baseY = 250;
        
        for (int i = 0; i < 3; i++) {
            int x = baseX + i * spacing;
            
            // Highlight selected spacecraft
            if (i == selectedSpacecraft) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(x - 85, baseY - 85, 170, 170);
                g2d.setStroke(new BasicStroke(1));
            }
            
            // Draw spacecraft sprite
            if (spacecraftMenuSprites[i] != null) {
                g2d.drawImage(spacecraftMenuSprites[i], x - 75, baseY - 75, 150, 150, null);
            } else {
                // Placeholder
                g2d.setColor(Color.GRAY);
                g2d.fillRect(x - 75, baseY - 75, 150, 150);
            }
            
            // Draw ship name
            g2d.setColor(i == selectedSpacecraft ? Color.YELLOW : Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics nameFm = g2d.getFontMetrics();
            g2d.drawString(shipNames[i], x - nameFm.stringWidth(shipNames[i])/2, baseY + 110);
        }
        
        // Draw stats for selected spacecraft
        int statsX = 100;
        int statsY = 450;
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        
        g2d.drawString("STATS:", statsX, statsY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("HP: " + spacecraftStats[0][selectedSpacecraft], statsX, statsY + 30);
        g2d.drawString("Speed: " + spacecraftStats[1][selectedSpacecraft], statsX, statsY + 55);
        g2d.drawString("Fire Rate: " + spacecraftStats[2][selectedSpacecraft] + " shots/min", statsX, statsY + 80);
        
        // Draw special ability
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("SPECIAL:", statsX, statsY + 120);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString(specialAbilityDescriptions[selectedSpacecraft], statsX, statsY + 145);
        
        // Draw controls
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Use ← → to select, ENTER to confirm, ESC to back", 10, SCREEN_HEIGHT - 20);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        switch (currentState) {
            case MENU:
                handleMenuInput(key);
                break;
            case SPACECRAFT_SELECT:
                handleSpacecraftSelectInput(key);
                break;
            case OPTIONS:
                handleOptionsInput(key);
                break;
            case EXIT_CONFIRM:
                handleExitConfirmInput(key);
                break;
            case GAME:
                handleGameInput(key, e);
                break;
            case PAUSED:
                handlePausedInput(key);
                break;
        }
    }
    
    private void handleMenuInput(int key) {
        // Track key sequence for Easter egg codes
        char keyChar = KeyEvent.getKeyText(key).toLowerCase().charAt(0);
        if (Character.isLetter(keyChar)) {
            keySequence += keyChar;
            // Keep only last 5 characters
            if (keySequence.length() > 5) {
                keySequence = keySequence.substring(keySequence.length() - 5);
            }
            
            // Check for secret codes
            if (keySequence.endsWith(SECRET_CODE_SPECIAL)) {
                saveEasterEggMode(1);
                playBGM(); // Restart BGM with special mode
                System.out.println("Special mode activated! (drifx entered)");
                keySequence = ""; // Reset
            } else if (keySequence.endsWith(SECRET_CODE_NORMAL)) {
                saveEasterEggMode(0);
                playBGM(); // Restart BGM with normal mode
                System.out.println("Normal mode activated! (brex entered)");
                keySequence = ""; // Reset
            }
        }
        
        if (key == KeyEvent.VK_UP) {
            selectedMenuOption = (selectedMenuOption - 1 + mainMenuOptions.length) % mainMenuOptions.length;
        } else if (key == KeyEvent.VK_DOWN) {
            selectedMenuOption = (selectedMenuOption + 1) % mainMenuOptions.length;
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedMenuOption) {
                case 0: // Play - go to spacecraft selection
                    currentState = GameState.SPACECRAFT_SELECT;
                    selectedSpacecraft = 0;
                    break;
                case 1: // Options
                    lastStateBeforeExitConfirm = GameState.MENU; // Track that we came from menu
                    currentState = GameState.OPTIONS;
                    selectedOptionsItem = 0;
                    break;
                case 2: // Exit
                    currentState = GameState.EXIT_CONFIRM;
                    selectedMenuOption = 1; // Default to "No"
                    break;
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            // Open exit confirmation from menu
            lastStateBeforeExitConfirm = currentState;
            currentState = GameState.EXIT_CONFIRM;
            selectedMenuOption = 1; // default 'No'
        }
    }
    
    private void handleSpacecraftSelectInput(int key) {
        if (key == KeyEvent.VK_LEFT) {
            selectedSpacecraft = (selectedSpacecraft - 1 + 3) % 3;
        } else if (key == KeyEvent.VK_RIGHT) {
            selectedSpacecraft = (selectedSpacecraft + 1) % 3;
        } else if (key == KeyEvent.VK_ENTER) {
            // Confirm selection and start game with selected spacecraft
            stopBGM(); // Stop menu BGM when starting game
            startNewGame();
        } else if (key == KeyEvent.VK_ESCAPE) {
            // Back to main menu
            currentState = GameState.MENU;
            selectedMenuOption = 0;
        }
    }
    
    private void handleOptionsInput(int key) {
        if (key == KeyEvent.VK_UP) {
            selectedOptionsItem = (selectedOptionsItem - 1 + optionsMenuItems.length) % optionsMenuItems.length;
        } else if (key == KeyEvent.VK_DOWN) {
            selectedOptionsItem = (selectedOptionsItem + 1) % optionsMenuItems.length;
        } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_MINUS) {
            // Decrease volume when on volume items
            if (selectedOptionsItem == 0) { // BGM Volume
                bgmVolume = Math.max(0, bgmVolume - 5);
                refreshOptionsItems();
                updateBGMVolume();
            } else if (selectedOptionsItem == 1) { // SFX Volume
                sfxVolume = Math.max(0, sfxVolume - 5);
                refreshOptionsItems();
            }
        } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_EQUALS) {
            // Increase volume
            if (selectedOptionsItem == 0) { // BGM Volume
                bgmVolume = Math.min(100, bgmVolume + 5);
                refreshOptionsItems();
                updateBGMVolume();
            } else if (selectedOptionsItem == 1) { // SFX Volume
                sfxVolume = Math.min(100, sfxVolume + 5);
                refreshOptionsItems();
            }
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedOptionsItem) {
                case 0:
                case 1:
                    // Nothing on enter for volume items
                    break;
                case 2: // Fullscreen toggle
                    fullscreen = !fullscreen;
                    toggleFullscreen(fullscreen);
                    refreshOptionsItems();
                    break;
                case 3: // Back
                    // Return to the state we came from (MENU or PAUSED)
                    if (lastStateBeforeExitConfirm == GameState.PAUSED) {
                        currentState = GameState.PAUSED;
                        selectedMenuOption = 0;
                    } else {
                        currentState = GameState.MENU;
                        selectedMenuOption = 0;
                    }
                    break;
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            // Return to the state we came from
            if (lastStateBeforeExitConfirm == GameState.PAUSED) {
                currentState = GameState.PAUSED;
                selectedMenuOption = 0;
            } else {
                currentState = GameState.MENU;
                selectedMenuOption = 0;
            }
        }
    }
    
    private void handleExitConfirmInput(int key) {
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            selectedMenuOption = 1 - selectedMenuOption; // Toggle between 0 and 1
        } else if (key == KeyEvent.VK_ENTER) {
            if (selectedMenuOption == 0) { // Yes
                System.exit(0);
            } else { // No
                currentState = GameState.MENU;
                selectedMenuOption = 0;
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            // Return to previous state
            if (lastStateBeforeExitConfirm == GameState.GAME) {
                currentState = GameState.GAME;
                gameRunning = true;
            } else {
                currentState = GameState.MENU;
                selectedMenuOption = 0;
            }
        }
    }
    
    private void handleGameInput(int key, KeyEvent e) {
        // If game over, ESC should go to menu, not pause
        if (key == KeyEvent.VK_ESCAPE) {
            if (!gameRunning) {
                // Game over - return to main menu
                currentState = GameState.MENU;
                selectedMenuOption = 0;
                isInGameBGM = false; // Exit in-game BGM mode
                playBGM(); // Play menu BGM when returning from game over
                return;
            } else {
                // Game running - pause game
                currentState = GameState.PAUSED;
                selectedMenuOption = 0; // Default to "Resume"
                return;
            }
        }
        
        // P key only pauses if game is running
        if (key == KeyEvent.VK_P && gameRunning) {
            currentState = GameState.PAUSED;
            selectedMenuOption = 0; // Default to "Resume"
            return;
        }
        
        // J key toggles co-op mode (ต้องอยู่โหมด Auto เท่านั้น)
        if (key == KeyEvent.VK_J && gameRunning) {
            if (!manualControlMode) {
                toggleCoopMode();
            } else {
                System.out.println("Cannot enter Co-op mode from Manual mode! Press M to switch to Auto mode first.");
            }
            return;
        }
        
        // F key for player1 special ability
        if (key == KeyEvent.VK_F && gameRunning) {
            fPressed = true;
            if (player != null) player.useSpecial(WORLD_WIDTH, WORLD_HEIGHT);
        }
        
        // Right Shift key for player2 special ability (co-op mode)
        if (key == KeyEvent.VK_SHIFT && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT && gameRunning && coopMode && player2 != null) {
            rightShiftPressed = true;
            player2.useSpecial(WORLD_WIDTH, WORLD_HEIGHT);
        }
        
        // M key toggles manual control mode (ต้องอยู่โหมด Auto เท่านั้น)
        if (key == KeyEvent.VK_M) {
            if (!coopMode) {
                manualControlMode = !manualControlMode;
                System.out.println("Manual control mode: " + (manualControlMode ? "ON" : "OFF"));
            } else {
                System.out.println("Cannot enter Manual mode from Co-op mode! Press J to exit Co-op mode first.");
            }
            return;
        }
        
        // Handle movement keys based on mode
        if (manualControlMode) {
            // Manual mode: WASD = movement only
            if (key == KeyEvent.VK_W) upPressed = true;
            if (key == KeyEvent.VK_S) downPressed = true;
            if (key == KeyEvent.VK_A) leftPressed = true;
            if (key == KeyEvent.VK_D) rightPressed = true;
            
            // Arrow keys = aiming/rotation
            if (key == KeyEvent.VK_UP) arrowUpPressed = true;
            if (key == KeyEvent.VK_DOWN) arrowDownPressed = true;
            if (key == KeyEvent.VK_LEFT) arrowLeftPressed = true;
            if (key == KeyEvent.VK_RIGHT) arrowRightPressed = true;
        } else if (coopMode) {
            // Co-op mode: WASD = Player1, Arrow keys = Player2
            if (key == KeyEvent.VK_W) upPressed = true;
            if (key == KeyEvent.VK_S) downPressed = true;
            if (key == KeyEvent.VK_A) leftPressed = true;
            if (key == KeyEvent.VK_D) rightPressed = true;
            
            // Arrow keys สำหรับ Player2 เท่านั้น (ไม่ควบคุม Player1)
            if (key == KeyEvent.VK_UP) arrowUpPressed = true;
            if (key == KeyEvent.VK_DOWN) arrowDownPressed = true;
            if (key == KeyEvent.VK_LEFT) arrowLeftPressed = true;
            if (key == KeyEvent.VK_RIGHT) arrowRightPressed = true;
        } else {
            // Auto mode (Solo): WASD only - arrow keys disabled
            if (key == KeyEvent.VK_W) upPressed = true;
            if (key == KeyEvent.VK_S) downPressed = true;
            if (key == KeyEvent.VK_A) leftPressed = true;
            if (key == KeyEvent.VK_D) rightPressed = true;
        }
        
        if (key == KeyEvent.VK_F) {
            // activate special ability (if player exists)
            if (player != null) player.useSpecial(WORLD_WIDTH, WORLD_HEIGHT);
        }

        if (key == KeyEvent.VK_R && !gameRunning) {
            restartGame();
        }
    }
    
    private void handlePausedInput(int key) {
        if (key == KeyEvent.VK_UP) {
            selectedMenuOption = (selectedMenuOption - 1 + pauseMenuOptions.length) % pauseMenuOptions.length;
        } else if (key == KeyEvent.VK_DOWN) {
            selectedMenuOption = (selectedMenuOption + 1) % pauseMenuOptions.length;
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedMenuOption) {
                case 0: // Resume
                    currentState = GameState.GAME;
                    break;
                case 1: // Settings
                    lastStateBeforeExitConfirm = GameState.PAUSED; // Track that we came from pause
                    currentState = GameState.OPTIONS;
                    selectedOptionsItem = 0;
                    break;
                case 2: // Return to Main Menu
                    currentState = GameState.MENU;
                    selectedMenuOption = 0;
                    gameRunning = false;
                    // Reset game state
                    enemies = new ArrayList<>();
                    bullets = new ArrayList<>();
                    enemyBullets = new ArrayList<>();
                    damagePopups = new ArrayList<>();
                    explosionParticles = new ArrayList<>();
                    boss = null;
                    bossSpawned = false;
                    player = null;
                    // Stop in-game BGM mode and play menu BGM
                    isInGameBGM = false;
                    playBGM();
                    break;
            }
        } else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_P) {
            // Resume game
            currentState = GameState.GAME;
        }
    }

    
    private void startNewGame() {
        currentState = GameState.GAME;
        gameRunning = true;
        // Reset game state
        score = 0;
        level = 1;
        enemySpawnRate = 1000;
        gameStartTime = System.currentTimeMillis();
        lastEnemySpawn = System.currentTimeMillis();
        lastUpdateTimeMillis = System.currentTimeMillis();
        
        // Create player with selected spacecraft stats
        int hp = spacecraftStats[0][selectedSpacecraft];
        int speed = spacecraftStats[1][selectedSpacecraft];
        int firerate = spacecraftStats[2][selectedSpacecraft];
        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, selectedSpacecraft, hp, speed, firerate);
        
        // Reset co-op mode and player2
        coopMode = false;
        player2 = null;
        bullets2 = new ArrayList<>();
        
        // Clear/reset all game objects
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        damagePopups = new ArrayList<>();
        explosionParticles = new ArrayList<>();
        boss = null;
        bossSpawned = false;
        type1MaxCap = 1 + random.nextInt(6);
        lastCapUpdate = System.currentTimeMillis();
        
        // Reset boss death animation
        bossDeathAnimationActive = false;
        bossDeathAlpha = 1.0f;
        
        // Reset boss attack systems
        bossBullets.clear();
        bossLasers.clear();
        bossLasersCreated = false;
        lastHomingBulletSpawn = 0;
        lastBossCollisionDamage = 0;
        lastBossLaserDamage = 0;
        
        // Reset camera
        cameraX = 0;
        cameraY = 0;
        
        // Reset manual mode
        manualControlMode = false;
        
        // Play in-game BGM
        isInGameBGM = true;
        isFirstGameBGM = true;
        playInGameBGM();
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Release special ability keys
        if (key == KeyEvent.VK_F) fPressed = false;
        
        // Check if it's Right Shift specifically
        if (key == KeyEvent.VK_SHIFT && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
            rightShiftPressed = false;
        }
        
        // Release movement keys
        if (key == KeyEvent.VK_W) upPressed = false;
        if (key == KeyEvent.VK_S) downPressed = false;
        if (key == KeyEvent.VK_A) leftPressed = false;
        if (key == KeyEvent.VK_D) rightPressed = false;
        
        // Release arrow keys
        if (key == KeyEvent.VK_UP) arrowUpPressed = false;
        if (key == KeyEvent.VK_DOWN) arrowDownPressed = false;
        if (key == KeyEvent.VK_LEFT) arrowLeftPressed = false;
        if (key == KeyEvent.VK_RIGHT) arrowRightPressed = false;
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    private void restartGame() {
        // Simply call startNewGame which already handles everything properly
        startNewGame();
    }

    private void toggleFullscreen(boolean enable) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof JFrame)) return;
        JFrame frame = (JFrame) w;

        frame.dispose();
        frame.setUndecorated(enable);
        if (enable) {
            // Get screen dimensions
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // Set panel to fill screen
            this.setPreferredSize(screenSize);
        } else {
            frame.setExtendedState(JFrame.NORMAL);
            // Reset to original size
            this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        frame.revalidate();
    }
    
    // Easter Egg System Methods
    private void loadEasterEggMode() {
        try {
            File file = new File(EASTER_EGG_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                reader.close();
                if (line != null && line.trim().equals("1")) {
                    easterEggMode = 1;
                    System.out.println("Special mode activated!");
                } else {
                    easterEggMode = 0;
                }
            } else {
                easterEggMode = 0;
            }
        } catch (IOException e) {
            easterEggMode = 0;
            System.out.println("Could not load Easter egg mode: " + e.getMessage());
        }
    }
    
    private void saveEasterEggMode(int mode) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(EASTER_EGG_FILE));
            writer.write(String.valueOf(mode));
            writer.close();
            easterEggMode = mode;
            System.out.println("Easter egg mode saved: " + mode);
        } catch (IOException e) {
            System.out.println("Could not save Easter egg mode: " + e.getMessage());
        }
    }
    
    // Audio System Methods
    private void initializeAudio() {
        try {
            // Pre-load sound effects pool for bullet sounds
            String soundPath = "src/Sound/SFX/Laser Beam.wav";
            File soundFile = new File(soundPath);
            
            if (soundFile.exists()) {
                for (int i = 0; i < SFX_POOL_SIZE; i++) {
                    try {
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                        sfxPool[i] = AudioSystem.getClip();
                        sfxPool[i].open(audioStream);
                        
                        // Set volume for each clip
                        if (sfxPool[i].isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                            FloatControl volumeControl = (FloatControl) sfxPool[i].getControl(FloatControl.Type.MASTER_GAIN);
                            float dB = (float) (Math.log(sfxVolume / 100.0) * 20.0);
                            dB = Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum()));
                            volumeControl.setValue(dB);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load SFX clip " + i + ": " + e.getMessage());
                    }
                }
                System.out.println("SFX pool initialized with " + SFX_POOL_SIZE + " clips");
            } else {
                System.out.println("SFX file not found: " + soundPath);
            }
            
            audioInitialized = true;
            System.out.println("Audio system initialized");
        } catch (Exception e) {
            System.out.println("Audio system initialization failed: " + e.getMessage());
            audioInitialized = false;
        }
    }
    
    private void playBGM() {
        if (!audioInitialized) return;
        
        try {
            // Stop current BGM if playing
            stopBGM();
            
            // Determine which BGM to play based on Easter egg mode and game state
            String bgmPath;
            if (easterEggMode == 1) {
                // Special mode - play dar_start.wav
                bgmPath = "src/Sound/BMG/dar_start.wav";
                // Try bin folder if not found in src
                File testFile = new File(bgmPath);
                if (!testFile.exists()) {
                    bgmPath = "bin/Sound/BMG/dar_start.wav";
                }
                System.out.println("Playing Special Mode BGM: dar_start.wav");
            } else {
                // Normal mode - play Start BGM.wav
                bgmPath = "src/Sound/BMG/Start BGM.wav";
                System.out.println("Playing Normal Mode BGM: Start BGM.wav");
            }
            
            File bgmFile = new File(bgmPath);
            if (!bgmFile.exists()) {
                System.out.println("BGM file not found: " + bgmPath);
                return;
            }
            
            // Load and play WAV file
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bgmFile);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            // Set volume
            updateBGMVolume();
            
            // Loop continuously
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
            System.out.println("BGM started successfully");
            
        } catch (Exception e) {
            System.out.println("Could not play BGM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void playInGameBGM() {
        if (!audioInitialized) return;
        if (easterEggMode != 1) return; // Only for Easter egg mode
        
        try {
            // Stop current BGM if playing
            stopBGM();
            
            String bgmPath;
            if (isFirstGameBGM) {
                // First time: always play Dar_ingame.wav
                bgmPath = "src/Sound/BMG/Dar_ingame.wav";
                System.out.println("Playing first in-game BGM: Dar_ingame.wav");
                isFirstGameBGM = false;
            } else {
                // After first song: randomly choose between Dar_ingame.wav and Dar_ingame2.wav
                int choice = random.nextInt(2);
                if (choice == 0) {
                    bgmPath = "src/Sound/BMG/Dar_ingame.wav";
                    System.out.println("Playing random in-game BGM: Dar_ingame.wav");
                } else {
                    bgmPath = "src/Sound/BMG/Dar_ingame2.wav";
                    System.out.println("Playing random in-game BGM: Dar_ingame2.wav");
                }
            }
            
            File bgmFile = new File(bgmPath);
            if (!bgmFile.exists()) {
                System.out.println("In-game BGM file not found: " + bgmPath);
                return;
            }
            
            // Load and play WAV file
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bgmFile);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            
            // Set volume
            updateBGMVolume();
            
            // Add listener to play next random track when this one ends
            bgmClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && isInGameBGM) {
                    // Song ended - play next random track
                    playInGameBGM();
                }
            });
            
            // Start playing (once - will loop via listener)
            bgmClip.start();
            System.out.println("In-game BGM started successfully");
            
        } catch (Exception e) {
            System.out.println("Could not play in-game BGM: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void stopBGM() {
        if (bgmClip != null) {
            if (bgmClip.isRunning()) {
                bgmClip.stop();
            }
            bgmClip.close();
            bgmClip = null;
            System.out.println("BGM stopped");
        }
    }
    
    private void updateBGMVolume() {
        if (bgmClip != null && bgmClip.isOpen()) {
            try {
                FloatControl volumeControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0-100 to decibels (range typically -80.0 to 6.0)
                float dB = (float) (Math.log(bgmVolume / 100.0) * 20.0);
                // Clamp to valid range
                dB = Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum()));
                volumeControl.setValue(dB);
                System.out.println("BGM volume set to: " + bgmVolume + "% (" + dB + " dB)");
            } catch (Exception e) {
                System.out.println("Could not adjust BGM volume: " + e.getMessage());
            }
        }
    }
    
    private void playSFX(String soundPath) {
        if (!audioInitialized) return;
        
        try {
            File soundFile = new File(soundPath);
            if (!soundFile.exists()) {
                System.out.println("SFX file not found: " + soundPath);
                return;
            }
            
            // Create a new clip for SFX (don't reuse to allow overlapping sounds)
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            // Set SFX volume
            try {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(sfxVolume / 100.0) * 20.0);
                dB = Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum()));
                volumeControl.setValue(dB);
            } catch (Exception e) {
                System.out.println("Could not adjust SFX volume: " + e.getMessage());
            }
            
            clip.start();
            
            // Clean up after playing
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            
        } catch (Exception e) {
            System.out.println("Could not play SFX: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void playBulletSound() {
        if (!audioInitialized || sfxPool == null) return;
        
        try {
            // Use round-robin pooling for non-blocking sound playback
            Clip clip = sfxPool[sfxPoolIndex];
            
            if (clip != null && clip.isOpen()) {
                // Stop and rewind the clip if it's playing
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.setFramePosition(0);
                
                // Update volume in case it changed
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log(sfxVolume / 100.0) * 20.0);
                    dB = Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum()));
                    volumeControl.setValue(dB);
                }
                
                // Play the sound
                clip.start();
                
                // Move to next clip in pool (round-robin)
                sfxPoolIndex = (sfxPoolIndex + 1) % SFX_POOL_SIZE;
            }
        } catch (Exception e) {
            System.out.println("Could not play bullet sound from pool: " + e.getMessage());
        }
    }
}

