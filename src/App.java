import javax.swing.*;

public class App {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Beyond the Red Eclipse: Exodus");
            SpaceGame game = new SpaceGame();
            
            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            // Apply fullscreen setting from loaded settings
            game.applyInitialFullscreen();
            
            game.startGame();
        });
    }
}
