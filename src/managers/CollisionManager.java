package managers;

import interfaces.Collidable;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages collision detection between game objects
 */
public class CollisionManager {
    
    /**
     * Interface for collision handlers
     */
    public interface CollisionHandler<T extends Collidable, U extends Collidable> {
        void handleCollision(T first, U second);
    }
    
    /**
     * Check collisions between two lists of collidable objects
     * and invoke handler for each collision
     */
    public <T extends Collidable, U extends Collidable> void checkCollisions(
            List<T> firstList, 
            List<U> secondList, 
            CollisionHandler<T, U> handler) {
        
        List<T> toRemoveFirst = new ArrayList<>();
        List<U> toRemoveSecond = new ArrayList<>();
        
        for (T first : firstList) {
            if (toRemoveFirst.contains(first)) continue;
            
            for (U second : secondList) {
                if (toRemoveSecond.contains(second)) continue;
                
                if (first.collidesWith(second)) {
                    handler.handleCollision(first, second);
                    // Note: Handler is responsible for marking objects for removal
                }
            }
        }
    }
    
    /**
     * Check if a single object collides with any object in a list
     */
    public <T extends Collidable, U extends Collidable> U findCollision(
            T object, 
            List<U> list) {
        
        for (U other : list) {
            if (object.collidesWith(other)) {
                return other;
            }
        }
        return null;
    }
}
