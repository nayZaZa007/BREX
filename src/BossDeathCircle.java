import java.awt.*;

public class BossDeathCircle {
    private float x, y;
    private float radius;
    private final float growth;
    private final Color color;
    private final float lifeTime;
    private float age = 0f;
    private float alpha = 1f;

    public BossDeathCircle(float x, float y, float startRadius, float growth, Color color, float lifeTime) {
        this.x = x;
        this.y = y;
        this.radius = startRadius;
        this.growth = growth;
        this.color = color;
        this.lifeTime = Math.max(0.05f, lifeTime);
        this.alpha = 1f;
    }

    public void update(float dt) {
        age += dt;
        radius += growth * dt;
        alpha = Math.max(0f, 1f - (age / lifeTime));
    }

    public boolean isAlive() {
        return alpha > 0f && age <= lifeTime * 1.2f;
    }

    public void draw(Graphics2D g2d) {
        float a = Math.max(0, Math.min(1, alpha));
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g2d.setColor(color);
        float r = radius;
        g2d.fillOval(Math.round(x - r), Math.round(y - r), Math.round(r * 2), Math.round(r * 2));
        
        // Draw ring outline
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.6f));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(Math.round(x - r), Math.round(y - r), Math.round(r * 2), Math.round(r * 2));
    }
}
