import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class SpaceGame extends JPanel implements ActionListener, KeyListener {
    
    // Game States
    public enum GameState {
        MENU, SPACECRAFT_SELECT, GAME, OPTIONS, EXIT_CONFIRM
    }
    
    private static final int SCREEN_WIDTH = 1000;  // หน้าจอที่เห็น
    private static final int SCREEN_HEIGHT = 700;  // หน้าจอที่เห็น
    private static final int WORLD_WIDTH = 3000;   // โลกทั้งหมด (3x ใหญ่กว่า)
    private static final int WORLD_HEIGHT = 2100;  // โลกทั้งหมด (3x ใหญ่กว่า)
    private static final int DELAY = 16; // ~60 FPS
    
    private Timer timer;
    private Player player;
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
    private static final long BOSS_SPAWN_TIME = 210000; // 3.5 minutes in milliseconds
    
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

    // Options state
    private int volume; // 0-100
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
    
    public SpaceGame() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        
        // เริ่มต้นที่หน้าเมนู
        currentState = GameState.MENU;
        selectedMenuOption = 0;
        selectedOptionsItem = 0;
        
        initializeGame();
        loadSpacecraftMenuSprites();
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
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        damagePopups = new ArrayList<>();
        explosionParticles = new ArrayList<>();
        random = new Random();
        boss = null;
        bossSpawned = false;
        
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
        volume = 80; // default
        fullscreen = false;
        refreshOptionsItems();
    }

    private void refreshOptionsItems() {
        optionsMenuItems = new String[] {
            "Volume: " + volume,
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
        
        // Update camera to follow player
        updateCamera();
        
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
            
            // Check bullet-enemy collision
            Iterator<Enemy> enemyIterator2 = enemies.iterator();
            while (enemyIterator2.hasNext()) {
                Enemy enemy = enemyIterator2.next();
                if (bullet.collidesWith(enemy)) {
                    int dmg = bullet.getDamage();
                    enemy.takeDamage(dmg);
                    damagePopups.add(new DamagePopup(enemy.getX(), enemy.getY(), dmg, Color.YELLOW));
                    bulletIterator.remove();
                    
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
            
            // Check bullet-boss collision
            if (boss != null && !boss.isDead() && bullet.collidesWith(boss)) {
                int dmg = bullet.getDamage();
                boss.takeDamage(dmg);
                damagePopups.add(new DamagePopup(boss.getX(), boss.getY(), dmg, Color.RED));
                bulletIterator.remove();
                System.out.println("Boss hit! HP: " + boss.getHealth() + "/" + boss.getMaxHealth());
                
                if (boss.isDead()) {
                    System.out.println("=== BOSS DEFEATED ===");
                    // Create explosion particles
                    for (int i = 0; i < 50; i++) {
                        explosionParticles.add(new ExplosionParticle(boss.getX(), boss.getY()));
                    }
                    score += 500;
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
                }
            }
        }
        
        // Update Boss
        if (boss != null && !boss.isDead()) {
            boss.update(player, delta);
            
            // Boss shooting
            long currentTime = System.currentTimeMillis();
            if (currentTime - boss.getLastShotTime() > boss.getFireRate()) {
                // Boss fires bullets at player
                int bx = (int)boss.getX();
                int by = (int)boss.getY();
                int px = player.getX();
                int py = player.getY();
                enemyBullets.add(new EnemyBullet(bx, by, px, py));
                boss.setLastShotTime(currentTime);
                System.out.println("Boss fired bullet!");
            }
            
            // Boss collision with player
            if (boss.collidesWith(player)) {
                player.consumeDamage(20);
                damagePopups.add(new DamagePopup(player.getX(), player.getY(), 20, Color.RED));
                System.out.println("Player hit by Boss! Health: " + player.getHealth());
                
                if (player.getHealth() <= 0) {
                    System.out.println("Game Over! Final Score: " + score);
                    gameRunning = false;
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
        
        // Auto-shoot
        if (System.currentTimeMillis() - player.getLastShotTime() > player.getFireRate()) {
            shootBullet();
            player.setLastShotTime(System.currentTimeMillis());
            System.out.println("Bullet fired! Total bullets: " + bullets.size());
        }
        
        // Update power-ups
        Iterator<PowerUp> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();
            
            if (powerUp.collidesWith(player)) {
                applyPowerUp(powerUp);
                powerUpIterator.remove();
                System.out.println("Power-up collected: " + powerUp.getType());
            } else if (powerUp.isExpired()) {
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
        int dirX = 0, dirY = 0;
        if (leftPressed) dirX = -1;
        else if (rightPressed) dirX = 1;

        if (upPressed) dirY = -1;
        else if (downPressed) dirY = 1;

        // Inform player of input direction; actual movement integrated in Player.integrateMovement
        player.move(dirX, dirY, WORLD_WIDTH, WORLD_HEIGHT);
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
            // Weighted random: TYPE1 and TYPE3 are common (40% each), TYPE2 is rare (20%)
            int roll = random.nextInt(100);
            Enemy.EnemyType type;
            if (roll < 40) {
                type = Enemy.EnemyType.TYPE1;
            } else if (roll < 60) {
                type = Enemy.EnemyType.TYPE2;
            } else {
                type = Enemy.EnemyType.TYPE3;
            }
            // Use player's visible sprite size (player draws at 2x hitbox)
            int playerSpriteW = player.getWidth() * 2;
            int playerSpriteH = player.getHeight() * 2;
            Enemy spawned = new Enemy(x, y, type, playerSpriteW, playerSpriteH);
            enemies.add(spawned);
            System.out.println("Spawned enemy type=" + type + " size=" + spawned.getWidth() + "x" + spawned.getHeight());
        }
    }
    
    private void spawnBoss() {
        System.out.println("=== BOSS SPAWNED ===");
        // Spawn boss in the center of the world
        boss = new Boss(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);
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
        if (!enemies.isEmpty()) {
            // Find nearest enemy
            Enemy nearestEnemy = null;
            double nearestDistance = Double.MAX_VALUE;
            
            for (Enemy enemy : enemies) {
                double distance = Math.sqrt(Math.pow(enemy.getX() - player.getX(), 2) + 
                                         Math.pow(enemy.getY() - player.getY(), 2));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEnemy = enemy;
                }
            }
            
            if (nearestEnemy != null) {
                // Compute desired angle and set as player's target facing
                double desiredAngle = Math.atan2(nearestEnemy.getY() - player.getY(), nearestEnemy.getX() - player.getX());
                player.setFacingAngle(desiredAngle);

                // Fire using current facingAngle (so rotation affects aim over time)
                double fireAngle = player.getFacingAngle();
                double fireSpeed = 10.0; // same as previous default
                bullets.add(new Bullet(player.getX(), player.getY(), fireAngle, fireSpeed));
            }
        }
    }
    
    private void applyPowerUp(PowerUp powerUp) {
        switch (powerUp.getType()) {
            case HEALTH:
                player.heal(20);
                break;
            case SPEED:
                player.increaseSpeed(1);
                break;
            case FIRE_RATE:
                player.increaseFireRate(50);
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
                    
                    for (Enemy enemy : enemies) {
                        enemy.draw(g2d);
                    }
                    
                    for (Bullet bullet : bullets) {
                        bullet.draw(g2d);
                    }
                    
                    for (EnemyBullet enemyBullet : enemyBullets) {
                        enemyBullet.draw(g2d);
                    }
                    
                    for (PowerUp powerUp : powerUps) {
                        powerUp.draw(g2d);
                    }
                    
                    // Draw Boss
                    if (boss != null && !boss.isDead()) {
                        boss.draw(g2d);
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
                handleGameInput(key);
                break;
        }
    }
    
    private void handleMenuInput(int key) {
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
            // Decrease volume when on volume item
            if (selectedOptionsItem == 0) {
                volume = Math.max(0, volume - 5);
                refreshOptionsItems();
            }
        } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_EQUALS) {
            // Increase volume
            if (selectedOptionsItem == 0) {
                volume = Math.min(100, volume + 5);
                refreshOptionsItems();
            }
        } else if (key == KeyEvent.VK_ENTER) {
            switch (selectedOptionsItem) {
                case 0:
                    // Nothing on enter for volume
                    break;
                case 1: // Fullscreen toggle
                    fullscreen = !fullscreen;
                    toggleFullscreen(fullscreen);
                    refreshOptionsItems();
                    break;
                case 2: // Back to Menu
                    currentState = GameState.MENU;
                    selectedMenuOption = 0;
                    break;
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            currentState = GameState.MENU;
            selectedMenuOption = 0;
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
    
    private void handleGameInput(int key) {
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) upPressed = true;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) downPressed = true;
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) rightPressed = true;
        
        if (key == KeyEvent.VK_F) {
            // activate special ability (if player exists)
            if (player != null) player.useSpecial(WORLD_WIDTH, WORLD_HEIGHT);
        }

        if (key == KeyEvent.VK_R && !gameRunning) {
            restartGame();
        } else if (key == KeyEvent.VK_ESCAPE) {
            // Pause and confirm exit
            lastStateBeforeExitConfirm = currentState;
            currentState = GameState.EXIT_CONFIRM;
            selectedMenuOption = 1; // default No
            gameRunning = false;
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
        
        // Create player with selected spacecraft stats
        int hp = spacecraftStats[0][selectedSpacecraft];
        int speed = spacecraftStats[1][selectedSpacecraft];
        int firerate = spacecraftStats[2][selectedSpacecraft];
        player = new Player(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, selectedSpacecraft, hp, speed, firerate);
        
        // Clear all game objects
        enemies.clear();
        bullets.clear();
        enemyBullets.clear();
        powerUps.clear();
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) upPressed = false;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) downPressed = false;
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) rightPressed = false;
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    private void restartGame() {
        timer.stop();
        initializeGame();
        timer.start();
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
}
