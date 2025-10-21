package interfaces;

/**
 * Interface for all objects that update each frame
 */
public interface Updatable {
    /**
     * Update this object's state
     * @param deltaMs Time elapsed since last update in milliseconds
     */
    void update(long deltaMs);
}
