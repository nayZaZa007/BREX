package managers;

/**
 * Manages game states and transitions between them
 */
public class GameStateManager {
    
    public enum GameState {
        MENU,
        SPACECRAFT_SELECT,
        GAME,
        OPTIONS,
        EXIT_CONFIRM
    }
    
    private GameState currentState;
    private GameState lastState;
    
    public GameStateManager() {
        this.currentState = GameState.MENU;
        this.lastState = GameState.MENU;
    }
    
    public void setState(GameState newState) {
        this.lastState = this.currentState;
        this.currentState = newState;
    }
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public GameState getLastState() {
        return lastState;
    }
    
    public boolean isInGame() {
        return currentState == GameState.GAME;
    }
    
    public void returnToPreviousState() {
        GameState temp = currentState;
        currentState = lastState;
        lastState = temp;
    }
}
