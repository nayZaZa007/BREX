# OOP Refactoring Guide

## Current vs. New Architecture

### Before (Current SpaceGame.java structure):
```java
public class SpaceGame extends JPanel {
    // All game logic mixed together
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Bullet> bullets;
    // ... dozens of fields
    
    public void update() {
        // Hundreds of lines mixing:
        // - State management
        // - Entity updates
        // - Collision detection
        // - Input handling
    }
}
```

### After (New OOP structure):
```java
public class SpaceGame extends JPanel {
    // Clean separation of concerns
    private GameStateManager stateManager;
    private EntityManager entityManager;
    private CollisionManager collisionManager;
    private InputManager inputManager;
    
    public void update() {
        // Clear, delegated responsibilities
        inputManager.clearJustPressed();
        
        if (stateManager.isInGame()) {
            entityManager.updateAll(deltaMs);
            checkAllCollisions();
        }
    }
    
    private void checkAllCollisions() {
        // Clean collision handling using managers
        collisionManager.checkCollisions(
            entityManager.getEntitiesOfType(Bullet.class),
            entityManager.getEntitiesOfType(Enemy.class),
            (bullet, enemy) -> {
                entityManager.removeEntity(bullet);
                entityManager.removeEntity(enemy);
                score += 10;
            }
        );
    }
}
```

## Step-by-Step Refactoring Plan

### Phase 1: Make Entities Extend GameObject ✓ (Ready to use)

**Current Player.java** should become:
```java
// Before
public class Player {
    private int x, y;
    private int width, height;
    // ... collision logic here
}

// After
import entities.GameObject;

public class Player extends GameObject {
    // Inherits: x, y, width, height, collisionRadius
    // Inherits: collidesWith(), getCenterX(), getCenterY()
    
    // Only Player-specific fields remain:
    private int health;
    private int speed;
    private double facingAngle;
    // ...
}
```

### Phase 2: Use EntityManager

**Current approach** (scattered ArrayLists):
```java
private ArrayList<Enemy> enemies = new ArrayList<>();
private ArrayList<Bullet> bullets = new ArrayList<>();

// Add
enemies.add(new Enemy(...));

// Update (repeated for each type)
for (Enemy e : enemies) {
    e.update(player);
}

// Draw (repeated for each type)
for (Enemy e : enemies) {
    e.draw(g2d);
}
```

**New approach** (centralized):
```java
private EntityManager entityManager = new EntityManager();

// Add
entityManager.addEntity(new Enemy(...));

// Update ALL at once
entityManager.updateAll(deltaMs);

// Draw ALL at once
entityManager.drawAll(g2d);

// Get specific types when needed
List<Enemy> enemies = entityManager.getEntitiesOfType(Enemy.class);
```

### Phase 3: Use CollisionManager

**Current approach** (nested loops everywhere):
```java
// Bullet-Enemy collision (in SpaceGame.update())
for (Bullet bullet : bullets) {
    for (Enemy enemy : enemies) {
        if (bullet.collidesWith(enemy)) {
            enemies.remove(enemy);
            bullets.remove(bullet);
            score += 10;
        }
    }
}

// Player-Enemy collision (elsewhere in update())
for (Enemy enemy : enemies) {
    if (enemy.collidesWith(player)) {
        player.takeDamage(10);
        enemies.remove(enemy);
    }
}
```

**New approach** (clear and reusable):
```java
// Bullet-Enemy collision
collisionManager.checkCollisions(
    entityManager.getEntitiesOfType(Bullet.class),
    entityManager.getEntitiesOfType(Enemy.class),
    (bullet, enemy) -> {
        entityManager.removeEntity(bullet);
        entityManager.removeEntity(enemy);
        score += 10;
    }
);

// Player-Enemy collision
List<Enemy> enemies = entityManager.getEntitiesOfType(Enemy.class);
Enemy hitEnemy = collisionManager.findCollision(player, enemies);
if (hitEnemy != null) {
    player.consumeDamage(10);
    entityManager.removeEntity(hitEnemy);
}
```

### Phase 4: Use InputManager

**Current approach** (flags scattered):
```java
private boolean upPressed, downPressed, leftPressed, rightPressed;

public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_W) upPressed = true;
    if (e.getKeyCode() == KeyEvent.VK_S) downPressed = true;
    // ...
}

public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_W) upPressed = false;
    // ...
}

// Somewhere in update()
if (upPressed) player.move(...);
```

**New approach** (centralized):
```java
private InputManager inputManager = new InputManager();

// In constructor
addKeyListener(inputManager);

// In update()
if (inputManager.isUpPressed()) {
    player.moveUp();
}

if (inputManager.isKeyJustPressed(KeyEvent.VK_F)) {
    player.useSpecial();
}

// Clear at end of frame
inputManager.clearJustPressed();
```

### Phase 5: Use GameStateManager

**Current approach**:
```java
private GameState currentState;
private GameState lastState;

// Scattered state changes
if (selectedMenuOption == 0) {
    currentState = GameState.SPACECRAFT_SELECT;
}
```

**New approach**:
```java
private GameStateManager stateManager = new GameStateManager();

// Clear state transitions
if (selectedMenuOption == 0) {
    stateManager.setState(GameState.SPACECRAFT_SELECT);
}

// Easy state queries
if (stateManager.isInGame()) {
    // Game logic
}
```

## Benefits Summary

### Code Quality
- **Before**: 1000+ line SpaceGame.java with everything mixed
- **After**: ~200 line SpaceGame.java + focused manager classes

### Testability
- **Before**: Can't test collision without entire game
- **After**: Test CollisionManager independently

### Understanding
- **Before**: "Where is collision logic?" → Search entire file
- **After**: "Where is collision logic?" → CollisionManager.java

### Extensibility
- **Before**: Add new entity → Modify SpaceGame in 5+ places
- **After**: Add new entity → Extend GameObject, add to EntityManager

## Next Steps

To complete the refactoring:

1. **Modify Player, Enemy, Bullet classes** to extend GameObject
2. **Replace ArrayLists in SpaceGame** with EntityManager
3. **Replace collision loops** with CollisionManager calls
4. **Replace input flags** with InputManager
5. **Replace state fields** with GameStateManager

The interfaces and managers are ready to use immediately!
