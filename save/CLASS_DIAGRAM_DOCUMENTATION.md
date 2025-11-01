# Class Diagram Documentation

## Overview
This document describes the architecture of the BREX Space Shooter game using Object-Oriented Programming principles.

## Architecture Layers

### 1. **Entry Point**
- **App**: Main entry point that creates the JFrame and SpaceGame instance

### 2. **Core Game Controller**
- **SpaceGame**: Central game controller extending JPanel
  - Manages game state (MENU, LEVEL_SELECT, GAME, PAUSED, LEVEL_UP, etc.)
  - Handles game loop at 60 FPS
  - Coordinates all entities (Player, Boss, Enemies, Bullets)
  - Manages two game levels with different mechanics
  - Implements collision detection and rendering

### 3. **Entity Classes**

#### Player Entity
- **Player**: Main playable character
  - Position, health, speed, fire rate attributes
  - Movement with inertia physics (vx, vy, acceleration, damping)
  - Shooting mechanics with target-based aiming
  - Special abilities (laser, shield, speed boost)
  - Upgrade system (health, speed, fire rate enhancements)

#### Enemy Entities
- **Enemy**: Standard enemies with 3 types
  - TYPE1: Common enemy with laser attacks
  - TYPE2: Rare enemy (1.5x size, 3 HP) with 6-way bullet pattern
  - TYPE3: Common enemy with standard bullets
  - Individual behaviors and fire rates

- **Boss**: Level 1 final boss
  - 10 HP with multi-phase attack system
  - 3 Attack Phases:
    - BARRAGE: Grid pattern bullet spray
    - LASER_SPIN: 6-way rotating lasers
    - HOMING: Tracking bullets that follow player
  - Spawns enemies during battle
  - Death animation with particle effects

#### Projectile Classes
- **Bullet**: Player projectiles
  - Random damage (8-12)
  - Target-based or angle-based firing
  - Lifetime management (60 seconds)

- **EnemyBullet**: Enemy projectiles
  - Straight-line movement
  - Fixed speed and damage

- **BossBullet**: Boss projectiles
  - Homing capability (tracks player)
  - Higher damage than enemy bullets

#### Support Classes
- **PowerUp**: Collectible power-ups
  - 3 types: HEALTH (green), SPEED (blue), FIRE_RATE (pink)
  - 10-second expiration timer
  - Drop rate: Level 1 = 10%, Level 2 = 5%

- **LaserBeam**: TYPE1 enemy laser attack
  - Continuous beam with duration
  - Collision detection along beam path

- **BossLaser**: Boss rotating laser attack
  - 6 lasers rotating in circle
  - Continuous collision detection

### 4. **Base Architecture**

#### Abstract Base Class
- **GameObject**: Abstract base for all game entities
  - Provides common properties (x, y, width, height, collisionRadius)
  - Implements all 3 core interfaces
  - Circular collision detection

#### Core Interfaces
- **Drawable**: Objects that render to screen
  - `draw(Graphics2D)` method

- **Updatable**: Objects that update each frame
  - `update(long deltaMs)` method

- **Collidable**: Objects that can collide
  - Center coordinates and collision radius
  - `collidesWith(Collidable)` method

### 5. **Manager Classes**

- **EntityManager**: Manages entity lifecycle
  - Maintains entity collections
  - Deferred add/remove to avoid concurrent modification
  - Type-based entity queries
  - Batch update and draw operations

- **CollisionManager**: Handles collision detection
  - Generic collision checking between lists
  - Collision handler callbacks
  - Single-object collision queries

- **GameStateManager**: Manages game states
  - State enumeration (MENU, GAME, PAUSED, etc.)
  - State transitions
  - Previous state tracking

- **InputManager**: Centralizes keyboard input
  - Pressed key tracking
  - Just-pressed detection (single-frame)
  - Frame-based cleanup

### 6. **Enumerations**

- **GameState**: Game flow states
  - MENU, LEVEL_SELECT, SPACECRAFT_SELECT, GAME
  - OPTIONS, PAUSED, LEVEL_UP, LEVEL1_WIN, EXIT_CONFIRM

- **EnemyType**: Enemy variants
  - TYPE1, TYPE2, TYPE3

- **AttackPhase**: Boss attack patterns
  - BARRAGE, LASER_SPIN, HOMING

- **PowerUpType**: Power-up types
  - HEALTH, SPEED, FIRE_RATE

## Key Design Patterns

### 1. **State Pattern**
- GameStateManager controls game flow
- Each state has dedicated rendering and input handling
- Clean transitions between states

### 2. **Object Pool Pattern**
- Sound effects use Clip pooling
- Prevents resource exhaustion
- Reuses audio objects

### 3. **Observer Pattern**
- CollisionHandler interface for collision callbacks
- Decouples collision detection from response

### 4. **Strategy Pattern**
- Boss uses different attack strategies per phase
- Enemy types have different behaviors
- Special abilities use strategy pattern

### 5. **Composition over Inheritance**
- Entities use interfaces (Drawable, Updatable, Collidable)
- Flexible composition of behaviors
- GameObject provides shared implementation

## Level System

### Level 1 (Boss Mode)
- Background: Black (#000000)
- Boss spawns after 25 seconds
- Win condition: Defeat the boss
- Unlocks Level 2 on completion

### Level 2 (Endless Mode)
- Background: Dark red (#2b0400)
- No boss, continuous enemy spawning
- Harder difficulty (5% PowerUp drop vs 10%)
- High score tracking
- Persistent progress saving

## Level Up System
- Triggers every 20 seconds in-game
- Pauses gameplay with overlay screen
- 3 upgrade choices:
  - Max HP +20%
  - Move Speed +15%
  - Fire Rate +10%
- Shows before → after values

## Relationships Summary

### Inheritance
- GameObject implements Drawable, Updatable, Collidable

### Composition (Has-A)
- SpaceGame contains Player, Boss
- SpaceGame has many Enemies, Bullets, PowerUps
- EntityManager has many GameObjects
- Enemy may have LaserBeam

### Association (Uses)
- Boss spawns Enemy instances
- CollisionManager uses Collidable interface
- App creates SpaceGame

### Dependencies
- All entities depend on Graphics2D for rendering
- Managers coordinate entity behaviors

## File Structure
```
src/
├── App.java                    # Entry point
├── SpaceGame.java             # Main game controller
├── Player.java                # Player entity
├── Enemy.java                 # Enemy entity
├── Boss.java                  # Boss entity
├── Bullet.java                # Player projectile
├── EnemyBullet.java          # Enemy projectile
├── BossBullet.java           # Boss projectile
├── PowerUp.java              # Power-up collectible
├── LaserBeam.java            # Enemy laser
├── BossLaser.java            # Boss laser
├── entities/
│   └── GameObject.java        # Abstract base class
├── interfaces/
│   ├── Drawable.java         # Rendering interface
│   ├── Updatable.java        # Update interface
│   └── Collidable.java       # Collision interface
└── managers/
    ├── EntityManager.java    # Entity lifecycle
    ├── CollisionManager.java # Collision detection
    ├── GameStateManager.java # State management
    └── InputManager.java     # Input handling
```

## UML Diagram

The full class diagram is available in `CLASS_DIAGRAM_FULL.puml`.

To view the diagram:
1. Install PlantUML extension in VS Code
2. Open `CLASS_DIAGRAM_FULL.puml`
3. Use Alt+D to preview

Or use online viewer: http://www.plantuml.com/plantuml/

## Key Features

### Physics System
- Delta-time based movement for frame-rate independence
- Inertia system with acceleration and damping
- Smooth player movement

### Collision System
- Circular hitbox collision detection
- Optimized with spatial partitioning
- Generic collision handlers

### Audio System
- Background music with fade effects
- Sound effect pooling (10 clips per effect)
- Boss-specific music

### Persistence
- Settings saved to `settings.dat` (BGM, SFX, fullscreen)
- Level 2 progress in `level2_progress.dat` (unlock, high score)
- Easter egg tracking in `easter_egg.dat`

### Visual Effects
- Damage popups with floating numbers
- Explosion particles
- Boss death animation (8 seconds)
- Expanding death circles
- Camera shake effects

## Design Principles Applied

1. **Single Responsibility**: Each class has one clear purpose
2. **Open/Closed**: Extensible through interfaces and inheritance
3. **Liskov Substitution**: GameObject subclasses are interchangeable
4. **Interface Segregation**: Small, focused interfaces
5. **Dependency Inversion**: Depends on abstractions (interfaces)

## Future Extensibility

The architecture supports easy addition of:
- New enemy types (add to EnemyType enum)
- New boss phases (add to AttackPhase enum)
- New power-ups (add to PowerUpType enum)
- New game states (add to GameState enum)
- New levels (extend level system)
- Co-op mode (Player2 already partially implemented)

---

**Note**: This diagram shows only the 10 most important members per class for clarity. Full implementations contain additional fields and methods.
