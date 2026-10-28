import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DownloadFile {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            DownloadFrame frame = new DownloadFrame();
            frame.setVisible(true);
        });
    }
}