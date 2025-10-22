import java.awt.*;
import java.util.Random;

public class ExplosionParticle {
    private double x, y;
    private double vx, vy;
    private Color color;
    private int size;
    private long creationTime;
    private static final long LIFETIME = 1500; // 1.5 seconds
    private static Random rand = new Random();
    
    public ExplosionParticle(double x, double y) {
        this.x = x;
        this.y = y;
        
        // Random velocity in circle
        double angle = rand.nextDouble() * Math.PI * 2;
        double speed = 2 + rand.nextDouble() * 6;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        
        // Random bright color
        int colorChoice = rand.nextInt(4);
        switch (colorChoice) {
            case 0: color = Color.YELLOW; break;
            case 1: color = Color.ORANGE; break;
            case 2: color = Color.RED; break;
            case 3: color = Color.WHITE; break;
        }
        
        this.size = 3 + rand.nextInt(5);
        this.creationTime = System.currentTimeMillis();
    }
    
    public void update() {
        x += vx;
        y += vy;
        
        // Slow down
        vx *= 0.98;
        vy *= 0.98;
    }
    
    public void draw(Graphics2D g2d) {
        long age = System.currentTimeMillis() - creationTime;
        float alpha = 1.0f - (float)age / (float)LIFETIME;
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
        g2d.setColor(c);
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);
        
        // Glow effect
        g2d.setColor(new Color(255, 255, 255, (int)(alpha * 100)));
        g2d.fillOval((int)x - size/4, (int)y - size/4, size/2, size/2);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > LIFETIME;
    }
}
