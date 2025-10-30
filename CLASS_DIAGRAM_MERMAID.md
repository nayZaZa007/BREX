# Class Diagram (Mermaid Format)

```mermaid
classDiagram
    %% Entry Point
    class App {
        +main(String[])
    }
    
    %% Main Game Controller
    class SpaceGame {
        -player: Player
        -boss: Boss
        -enemies: ArrayList~Enemy~
        -bullets: ArrayList~Bullet~
        -enemyBullets: ArrayList~EnemyBullet~
        -currentState: GameState
        -currentLevel: int
        -level2Unlocked: boolean
        +actionPerformed(ActionEvent)
        +paintComponent(Graphics)
    }
    
    %% Core Entities
    class Player {
        -x, y: double
        -health: int
        -maxHealth: int
        -speed: double
        -fireRate: int
        -sprite: BufferedImage
        +move(dx, dy, delta)
        +shoot(targetX, targetY)
        +takeDamage(int)
        +increaseMaxHealth(int)
    }
    
    class Enemy {
        -x, y: double
        -health: int
        -type: EnemyType
        -speed: double
        -sprite: BufferedImage
        -activeLaser: LaserBeam
        +update(delta)
        +shoot() EnemyBullet
        +takeDamage(int)
        +draw(Graphics2D)
    }
    
    class Boss {
        -x, y: double
        -health: int
        -maxHealth: int
        -currentPhase: AttackPhase
        -phaseTimer: double
        -sprite: BufferedImage
        +update(delta, playerX, playerY)
        +shoot() BossBullet
        +spawnEnemy() Enemy
        +draw(Graphics2D)
    }
    
    %% Projectiles
    class Bullet {
        -x, y: double
        -dx, dy: double
        -damage: int
        -speed: double
        +update(delta)
        +draw(Graphics2D)
        +isExpired() boolean
    }
    
    class EnemyBullet {
        -x, y: double
        -dx, dy: double
        -speed: double
        +update(delta)
        +draw(Graphics2D)
    }
    
    class BossBullet {
        -x, y: double
        -targetX, targetY: int
        -isHoming: boolean
        +update(delta, playerX, playerY)
        +draw(Graphics2D)
    }
    
    %% Support Classes
    class PowerUp {
        -x, y: int
        -type: PowerUpType
        -color: Color
        -spawnTime: long
        +draw(Graphics2D)
        +isExpired() boolean
        +getType() PowerUpType
    }
    
    class LaserBeam {
        -startX, startY: int
        -endX, endY: int
        -active: boolean
        -startTime: long
        +update(delta)
        +draw(Graphics2D)
        +isActive() boolean
    }
    
    class BossLaser {
        -x, y: double
        -angle: double
        -length: int
        -rotationSpeed: double
        +update(delta)
        +draw(Graphics2D)
        +checkCollision(Player) boolean
    }
    
    %% Base Class & Interfaces
    class GameObject {
        <<abstract>>
        #x, y: double
        #width, height: int
        #collisionRadius: int
        +getCenterX() int
        +getCenterY() int
        +draw(Graphics2D)*
        +update(long)*
        +collidesWith(Collidable) boolean
    }
    
    class Drawable {
        <<interface>>
        +draw(Graphics2D)
    }
    
    class Updatable {
        <<interface>>
        +update(long)
    }
    
    class Collidable {
        <<interface>>
        +getCenterX() int
        +getCenterY() int
        +getCollisionRadius() int
        +collidesWith(Collidable) boolean
    }
    
    %% Managers
    class EntityManager {
        -entities: List~GameObject~
        -entitiesToAdd: List~GameObject~
        -entitiesToRemove: List~GameObject~
        +addEntity(GameObject)
        +removeEntity(GameObject)
        +updateAll(long)
        +drawAll(Graphics2D)
        +getEntitiesOfType(Class) List
        +clear()
    }
    
    class CollisionManager {
        +checkCollisions(List, List, handler)
        +findCollision(T, List) U
    }
    
    class GameStateManager {
        -currentState: GameState
        -lastState: GameState
        +setState(GameState)
        +getCurrentState() GameState
        +returnToPreviousState()
        +isInGame() boolean
    }
    
    class InputManager {
        -pressedKeys: Set~Integer~
        -justPressedKeys: Set~Integer~
        +isKeyPressed(int) boolean
        +isKeyJustPressed(int) boolean
        +clearJustPressed()
    }
    
    %% Enums
    class GameState {
        <<enumeration>>
        MENU
        LEVEL_SELECT
        SPACECRAFT_SELECT
        GAME
        OPTIONS
        PAUSED
        LEVEL_UP
        LEVEL1_WIN
        EXIT_CONFIRM
    }
    
    class EnemyType {
        <<enumeration>>
        TYPE1
        TYPE2
        TYPE3
    }
    
    class AttackPhase {
        <<enumeration>>
        BARRAGE
        LASER_SPIN
        HOMING
    }
    
    class PowerUpType {
        <<enumeration>>
        HEALTH
        SPEED
        FIRE_RATE
    }
    
    %% Relationships
    GameObject ..|> Drawable : implements
    GameObject ..|> Updatable : implements
    GameObject ..|> Collidable : implements
    
    SpaceGame *-- Player : contains
    SpaceGame *-- Boss : contains
    SpaceGame o-- Enemy : has many
    SpaceGame o-- Bullet : has many
    SpaceGame o-- EnemyBullet : has many
    SpaceGame o-- BossBullet : has many
    SpaceGame o-- PowerUp : has many
    
    SpaceGame --> GameState : uses
    Enemy --> EnemyType : uses
    Boss --> AttackPhase : uses
    PowerUp --> PowerUpType : uses
    Enemy o-- LaserBeam : may have
    
    EntityManager o-- GameObject : manages many
    CollisionManager ..> Collidable : uses
    GameStateManager --> GameState : manages
    
    App ..> SpaceGame : creates
    Boss ..> Enemy : spawns
```

## Quick View

This is a Mermaid class diagram version of the game architecture. 

**To view this diagram:**
1. Copy the mermaid code block above
2. Paste into [Mermaid Live Editor](https://mermaid.live/)
3. Or install Mermaid extension in VS Code

**Key Relationships:**
- **Implements** (dotted line with triangle): GameObject implements all 3 interfaces
- **Contains** (solid diamond): SpaceGame contains Player and Boss
- **Has many** (hollow diamond): SpaceGame has multiple Enemies, Bullets, PowerUps
- **Uses** (dashed arrow): Classes use enums and other dependencies
- **Creates** (dashed line): App creates SpaceGame instance

## Architecture Highlights

### 🎮 Main Components
- **SpaceGame**: Game loop, state management, rendering
- **Player/Enemy/Boss**: Core gameplay entities
- **Managers**: Modular systems for entities, collisions, state, input

### 🔧 Design Patterns
- **State Pattern**: GameStateManager
- **Strategy Pattern**: Boss AttackPhase system
- **Observer Pattern**: CollisionHandler callbacks
- **Composition**: GameObject + Interfaces

### 📦 Key Features
- 2 game levels (Boss mode, Endless mode)
- Level up system with 3 upgrade types
- 3 enemy types with unique behaviors
- Multi-phase boss with 3 attack patterns
- Physics-based movement with inertia
- Persistent progress saving

See `CLASS_DIAGRAM_DOCUMENTATION.md` for detailed explanation.
