# Class Diagram Documentation - BREX Space Game

## Overview
This document describes the OOP architecture of the BREX space shooter game, designed for easy class diagram creation.

## Package Structure

```
brex/
├── interfaces/          # Core interfaces
│   ├── Drawable
│   ├── Updatable
│   └── Collidable
├── entities/           # Game entities
│   └── GameObject (abstract)
├── managers/           # Game managers
│   ├── GameStateManager
│   ├── EntityManager
│   ├── CollisionManager
│   └── InputManager
└── [root]             # Main game classes
    ├── App
    ├── SpaceGame
    ├── Player
    ├── Enemy
    ├── Bullet
    ├── EnemyBullet
    └── PowerUp
```

## Class Diagram (Text Representation)

### Interfaces (Top Level)
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Drawable   │     │  Updatable  │     │  Collidable │
├─────────────┤     ├─────────────┤     ├─────────────┤
│+draw(g2d)   │     │+update(dt)  │     │+getCenterX()│
└─────────────┘     └─────────────┘     │+getCenterY()│
                                        │+getRadius() │
                                        │+collidesWith│
                                        └─────────────┘
       ▲                   ▲                    ▲
       │                   │                    │
       └───────────────────┴────────────────────┘
                           │
                    ┌──────────────┐
                    │  GameObject  │
                    │  (abstract)  │
                    ├──────────────┤
                    │-x, y         │
                    │-width, height│
                    │-collisionRad │
                    └──────────────┘
                           ▲
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────────┐      ┌─────────┐     ┌──────────┐
    │ Player  │      │ Enemy   │     │  Bullet  │
    └─────────┘      └─────────┘     └──────────┘
```

### Managers (Composition Relationships)
```
┌──────────────────────────────────────┐
│           SpaceGame                  │
│         (Main Controller)            │
├──────────────────────────────────────┤
│ Contains:                            │
│  - GameStateManager stateManager     │◄───┐
│  - EntityManager entityManager       │◄───┤
│  - CollisionManager collisionManager │◄───┤  Composition
│  - InputManager inputManager         │◄───┤  (Has-A)
├──────────────────────────────────────┤    │
│ +update()                            │    │
│ +render()                            │    │
│ +handleInput()                       │    │
└──────────────────────────────────────┘    │
                                            │
    ┌───────────────────┬─────────────────┼─────────────────┐
    │                   │                 │                 │
┌───────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────┐
│GameState      │ │Entity        │ │Collision     │ │Input       │
│Manager        │ │Manager       │ │Manager       │ │Manager     │
├───────────────┤ ├──────────────┤ ├──────────────┤ ├────────────┤
│-currentState  │ │-entities[]   │ │+check        │ │-pressedKeys│
│-lastState     │ │+addEntity()  │ │ Collisions() │ │+isPressed()│
│+setState()    │ │+removeEntity │ │+findCollision│ │+isJust     │
│+getState()    │ │+updateAll()  │ │              │ │ Pressed()  │
└───────────────┘ │+drawAll()    │ └──────────────┘ └────────────┘
                  │+getEntities  │
                  │ OfType()     │
                  └──────────────┘
```

## Key Relationships

### Inheritance Hierarchy
1. **GameObject** (abstract base class)
   - Implements: Drawable, Updatable, Collidable
   - Extended by: Player, Enemy, Bullet, EnemyBullet, PowerUp

### Composition (Has-A)
1. **SpaceGame** contains:
   - GameStateManager (1:1)
   - EntityManager (1:1)
   - CollisionManager (1:1)
   - InputManager (1:1)

2. **EntityManager** manages:
   - List<GameObject> (1:many)

### Key Design Patterns

1. **Strategy Pattern**: GameStateManager allows different behaviors based on current state
2. **Manager Pattern**: Centralized management of entities, collisions, and input
3. **Template Method**: GameObject provides common implementation, subclasses override specifics
4. **Observer-like**: CollisionManager uses handlers (callbacks) for collision events

## Benefits of This Architecture

1. **Single Responsibility**: Each class has one clear purpose
2. **Easy to Extend**: Add new entity types by extending GameObject
3. **Testable**: Managers can be tested independently
4. **Clear Dependencies**: Easy to understand what depends on what
5. **Simple Diagrams**: This structure maps directly to UML class diagrams

## How to Draw Class Diagram

### For UML Tools (PlantUML, draw.io, etc.):

**Core Interfaces**:
- 3 interface boxes: Drawable, Updatable, Collidable
- Each with their method signatures

**Abstract Base**:
- GameObject implements all 3 interfaces (dashed arrows up)
- Show fields and methods

**Concrete Entities**:
- Player, Enemy, Bullet extend GameObject (solid arrow up)
- Show specific fields/methods for each

**Managers**:
- 4 separate manager classes
- Show they are used by SpaceGame (composition diamonds)

**Main Controller**:
- SpaceGame at top
- Contains 4 managers
- Has methods: update(), render(), handleInput()
