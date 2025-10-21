import java.awt.*;

public class PowerUp {
    public enum PowerUpType {
        HEALTH, SPEED, FIRE_RATE
    }
    
    private int x, y;
    private int width, height;
    private PowerUpType type;
    private Color color;
    private long spawnTime;
    private static final long EXPIRE_TIME = 10000; // 10 seconds
    
    public PowerUp(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 20;
        this.height = 20;
        this.spawnTime = System.currentTimeMillis();
        
        // Random power-up type
        int typeChoice = (int)(Math.random() * 3);
        switch(typeChoice) {
            case 0: 
                type = PowerUpType.HEALTH;
                color = Color.GREEN;
                break;
            case 1: 
                type = PowerUpType.SPEED;
                color = Color.BLUE;
                break;
            default: 
                type = PowerUpType.FIRE_RATE;
                color = Color.PINK;
                break;
        }
    }
    
    public void draw(Graphics2D g2d) {
        // Draw simple power-up placeholder
        g2d.setColor(color);
        g2d.fillRect(x - width/2, y - height/2, width, height);
        
        // Draw border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x - width/2, y - height/2, width, height);
        
        // Draw type letter
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        
        String letter = "";
        switch(type) {
            case HEALTH: letter = "H"; break;
            case SPEED: letter = "S"; break;
            case FIRE_RATE: letter = "F"; break;
        }
        
        int textX = x - fm.stringWidth(letter)/2;
        int textY = y + fm.getHeight()/3;
        g2d.drawString(letter, textX, textY);
    }
    
    public boolean collidesWith(Player player) {
        return x - width/2 < player.getX() + player.getWidth()/2 &&
               x + width/2 > player.getX() - player.getWidth()/2 &&
               y - height/2 < player.getY() + player.getHeight()/2 &&
               y + height/2 > player.getY() - player.getHeight()/2;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > EXPIRE_TIME;
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public PowerUpType getType() { return type; }
}