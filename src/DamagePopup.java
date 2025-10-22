import java.awt.*;

public class DamagePopup {
    private double x, y;
    private int damage;
    private long creationTime;
    private static final long LIFETIME = 1000; // 1 second
    private Color color;
    private double offsetY = 0;
    
    public DamagePopup(double x, double y, int damage, Color color) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.color = color;
        this.creationTime = System.currentTimeMillis();
    }
    
    public void update(long deltaMs) {
        // Float upward
        offsetY -= 0.5;
    }
    
    public void draw(Graphics2D g2d) {
        long age = System.currentTimeMillis() - creationTime;
        float alpha = 1.0f - (float)age / (float)LIFETIME;
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        
        // Draw damage number with fade
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
        g2d.setColor(c);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String text = String.valueOf(damage);
        int textX = (int)x - fm.stringWidth(text) / 2;
        int textY = (int)(y + offsetY);
        g2d.drawString(text, textX, textY);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > LIFETIME;
    }
}
