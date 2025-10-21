# Simple Class Diagram - BREX Space Game

## Quick Visual Reference

### Level 1: Interfaces (Foundation)
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Drawable   │    │  Updatable  │    │  Collidable │
│             │    │             │    │             │
│ + draw()    │    │ + update()  │    │ + collides()│
└─────────────┘    └─────────────┘    └─────────────┘
       △                  △                   △
       └──────────────────┴───────────────────┘
                          │
```

### Level 2: Base Class (implements all 3 interfaces)
```
                    ┌──────────────┐
                    │  GameObject  │ (abstract)
                    ├──────────────┤
                    │ - x, y       │
                    │ - width      │
                    │ - height     │
                    │ - radius     │
                    └──────────────┘
                          △
         ┌────────────────┼────────────────┐
         │                │                │
```

### Level 3: Game Entities (extend GameObject)
```
  ┌─────────┐      ┌─────────┐      ┌─────────────┐
  │ Player  │      │ Enemy   │      │   Bullet    │
  ├─────────┤      ├─────────┤      ├─────────────┤
  │-health  │      │-type    │      │-angle       │
  │-shield  │      │-sprite  │      │-speed       │
  │-special │      │-fireRate│      │-lifetime    │
  └─────────┘      └─────────┘      └─────────────┘

  ┌──────────────┐      ┌────────────┐
  │ EnemyBullet  │      │  PowerUp   │
  ├──────────────┤      ├────────────┤
  │-targetX,Y    │      │-type       │
  │-accel        │      │-duration   │
  └──────────────┘      └────────────┘
```

### Level 4: Manager Classes (separate from entity hierarchy)
```
┌─────────────────────────────────────────────────────┐
│                   SpaceGame                         │
│                (Main Controller)                    │
├─────────────────────────────────────────────────────┤
│  Contains (has-a relationship):                    │
│  ◆ GameStateManager  stateManager                  │
│  ◆ EntityManager     entityManager                 │
│  ◆ CollisionManager  collisionManager              │
│  ◆ InputManager      inputManager                  │
└─────────────────────────────────────────────────────┘
           │          │           │          │
           ▽          ▽           ▽          ▽
    ┌──────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐
    │GameState │ │Entity   │ │Collision│ │Input   │
    │Manager   │ │Manager  │ │Manager  │ │Manager │
    └──────────┘ └─────────┘ └─────────┘ └────────┘
```

## Relationships Key

- `△` or `───▽` : **Inheritance** (is-a)
  - Example: Player **is a** GameObject

- `◆` : **Composition** (has-a, strong ownership)
  - Example: SpaceGame **has a** GameStateManager
  - If SpaceGame is destroyed, its managers are too

- `○` : **Aggregation** (has-a, weak ownership)
  - Example: EntityManager **has many** GameObjects
  - GameObjects can exist independently

## How to Read This Diagram

1. **Start at the top** (Interfaces)
   - These define "contracts" that classes must fulfill
   - If a class implements Drawable, it must have a draw() method

2. **Middle layer** (GameObject)
   - Abstract base class that implements all interfaces
   - Provides common code for all entities
   - Cannot be instantiated directly

3. **Concrete entities** (Player, Enemy, etc.)
   - Real classes you can create instances of
   - Inherit everything from GameObject
   - Add their own specific features

4. **Managers** (separate branch)
   - Not part of entity hierarchy
   - Handle specific game systems
   - Used by main controller (SpaceGame)

## Drawing This in Tools

### For Hand-Drawing or Whiteboard:
1. Draw 3 boxes at top (interfaces)
2. Draw 1 box below connected to all 3 (GameObject)
3. Draw 5 boxes below that, connected to GameObject (entities)
4. Draw 4 boxes to the side (managers)
5. Draw 1 big box containing references to the 4 managers (SpaceGame)

### For UML Tools (draw.io, Lucidchart, etc.):
1. Use **interface** shapes for: Drawable, Updatable, Collidable
2. Use **abstract class** shape for: GameObject
3. Use **class** shapes for: Player, Enemy, Bullet, EnemyBullet, PowerUp
4. Use **class** shapes for: 4 managers
5. Use **class** shape for: SpaceGame
6. Connect with:
   - Dashed arrows (◁ ─ ─) for interface implementation
   - Solid arrows (◁ ───) for inheritance
   - Composition diamonds (◆ ───) for SpaceGame to managers

### Color Coding Suggestion:
- **Blue**: Interfaces
- **Yellow**: Abstract classes
- **Green**: Concrete entity classes
- **Orange**: Manager classes
- **Red**: Main controller (SpaceGame)

## Example: Adding a New Entity

Want to add a "Boss" enemy?

```
1. Create Boss.java that extends GameObject
2. It automatically implements Drawable, Updatable, Collidable
3. Add it to EntityManager
4. Done! Collision detection works automatically
```

```java
public class Boss extends GameObject {
    private int phase;
    private List<WeakPoint> weakPoints;
    
    public Boss(int x, int y) {
        super(x, y, 200, 200);  // Big boss!
        this.phase = 1;
    }
    
    @Override
    public void draw(Graphics2D g2d) {
        // Draw boss sprite
    }
    
    @Override
    public void update(long deltaMs) {
        // Boss AI behavior
    }
}

// In SpaceGame:
entityManager.addEntity(new Boss(1500, 1000));
// That's it! Collision works, drawing works, updates work
```

## Why This Structure is Good

### ✅ Single Responsibility
Each class has ONE job:
- GameObject: Common entity behavior
- EntityManager: Manage entities
- CollisionManager: Handle collisions
- etc.

### ✅ Easy to Test
```java
// Test collision detection alone
CollisionManager cm = new CollisionManager();
Player p = new Player(100, 100, 0, 100, 100, 60);
Enemy e = new Enemy(120, 100, EnemyType.TYPE1);
assert(p.collidesWith(e));  // Works!
```

### ✅ Easy to Extend
```java
// Add new entity type
public class Asteroid extends GameObject {
    // Just implement draw() and update()
    // Everything else inherited!
}
```

### ✅ Clear Dependencies
```
App → SpaceGame → Managers → Entities → Interfaces
```
No circular dependencies, easy to understand!

## Summary

- **3 Interfaces** define capabilities
- **1 Abstract Base** implements all interfaces
- **5 Entity Types** extend the base
- **4 Managers** handle game systems
- **1 Controller** (SpaceGame) orchestrates everything
- **1 Entry Point** (App) starts it all

Total: 14 main classes, clear relationships, easy to diagram!
