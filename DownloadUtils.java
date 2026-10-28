import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;

public class DownloadUtils {

    // Method untuk memformat ukuran byte ke teks yang mudah dibaca (B, KB, MB, GB)
    public static String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }

    // Method untuk memformat kecepatan transfer byte per detik ke satuan B/s, KB/s, atau MB/s
    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec < 1024) {
            return String.format("%.0f B/s", bytesPerSec);
        } else if (bytesPerSec < 1024 * 1024) {
            return String.format("%.2f KB/s", bytesPerSec / 1024.0);
        } else {
            return String.format("%.2f MB/s", bytesPerSec / (1024.0 * 1024.0));
        }
    }

    // Method untuk mengekstrak nama file yang valid dari header Content-Disposition atau path URL
    public static String resolveFileName(URL url, URLConnection connection) {
        String fileName = null;

        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
            int index = disposition.indexOf("filename=");
            if (index > 0) {
                fileName = disposition.substring(index + 9).trim();
                // Hilangkan tanda kutip ganda jika ada
                if (fileName.startsWith("\"") && fileName.endsWith("\"") && fileName.length() > 1) {
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
            }
        }

        if (fileName == null || fileName.isEmpty()) {
            String path = url.getPath();
            if (path != null && path.contains("/")) {
                fileName = path.substring(path.lastIndexOf('/') + 1);
                try {
                    fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {}
            }
        }

        if (fileName == null || fileName.trim().isEmpty() || fileName.equals("/")) {
            fileName = "download_" + System.currentTimeMillis();
        }
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return fileName;
    }

    // Method untuk membuka folder hasil unduhan menggunakan Desktop API
    public static void openFolder(File folder, Component parent) {
        if (folder != null && folder.exists()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(folder);
                } else {
                    JOptionPane.showMessageDialog(parent,
                            "Fitur Desktop OPEN tidak didukung pada sistem operasi ini.\nLokasi folder: " + folder.getAbsolutePath(),
                            "Informasi", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Gagal membuka folder: " + ex.getMessage(),
                        "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
