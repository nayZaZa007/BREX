package managers;

import entities.GameObject;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all game entities (players, enemies, bullets, etc.)
 * Handles updates, drawing, and removal of entities
 */
public class EntityManager {
    private List<GameObject> entities;
    private List<GameObject> entitiesToAdd;
    private List<GameObject> entitiesToRemove;
    
    public EntityManager() {
        entities = new ArrayList<>();
        entitiesToAdd = new ArrayList<>();
        entitiesToRemove = new ArrayList<>();
    }
    
    /**
     * Add an entity to be added on next update
     */
    public void addEntity(GameObject entity) {
        entitiesToAdd.add(entity);
    }
    
    /**
     * Mark an entity for removal on next update
     */
    public void removeEntity(GameObject entity) {
        entitiesToRemove.add(entity);
    }
    
    /**
     * Update all entities
     */
    public void updateAll(long deltaMs) {
        // Process additions and removals
        entities.removeAll(entitiesToRemove);
        entities.addAll(entitiesToAdd);
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        
        // Update all entities
        for (GameObject entity : entities) {
            entity.update(deltaMs);
        }
    }
    
    /**
     * Draw all entities
     */
    public void drawAll(Graphics2D g2d) {
        for (GameObject entity : entities) {
            entity.draw(g2d);
        }
    }
    
    /**
     * Get all entities of a specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends GameObject> List<T> getEntitiesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (GameObject entity : entities) {
            if (type.isInstance(entity)) {
                result.add((T) entity);
            }
        }
        return result;
    }
    
    /**
     * Clear all entities
     */
    public void clear() {
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
    }
    
    /**
     * Get all entities
     */
    public List<GameObject> getAllEntities() {
        return new ArrayList<>(entities);
    }
    
    /**
     * Get entity count
     */
    public int getEntityCount() {
        return entities.size();
    }
}
