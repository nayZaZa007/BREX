package managers;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralizes keyboard input handling
 */
public class InputManager implements KeyListener {
    private Set<Integer> pressedKeys;
    private Set<Integer> justPressedKeys;
    
    public InputManager() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
    }
    
    /**
     * Check if a key is currently pressed
     */
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    /**
     * Check if a key was just pressed this frame
     * (will return false on subsequent frames until released and pressed again)
     */
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }
    
    /**
     * Clear the just-pressed state (call this each frame after processing input)
     */
    public void clearJustPressed() {
        justPressedKeys.clear();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (!pressedKeys.contains(keyCode)) {
            justPressedKeys.add(keyCode);
        }
        pressedKeys.add(keyCode);
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    // Convenience methods for common keys
    public boolean isUpPressed() {
        return isKeyPressed(KeyEvent.VK_W) || isKeyPressed(KeyEvent.VK_UP);
    }
    
    public boolean isDownPressed() {
        return isKeyPressed(KeyEvent.VK_S) || isKeyPressed(KeyEvent.VK_DOWN);
    }
    
    public boolean isLeftPressed() {
        return isKeyPressed(KeyEvent.VK_A) || isKeyPressed(KeyEvent.VK_LEFT);
    }
    
    public boolean isRightPressed() {
        return isKeyPressed(KeyEvent.VK_D) || isKeyPressed(KeyEvent.VK_RIGHT);
    }
}
