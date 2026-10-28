import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

public class DownloadTask extends SwingWorker<File, DownloadProgress> {
    private final String urlString;
    private final File targetDir;
    private final DownloadListener listener;
    private File outputFile;

    // Konstruktor menerima URL unduhan, folder tujuan, dan listener callback
    public DownloadTask(String urlString, File targetDir, DownloadListener listener) {
        this.urlString = urlString;
        this.targetDir = targetDir;
        this.listener = listener;
    }

    // Method doInBackground dieksekusi pada background thread secara asynchronous
    @Override
    protected File doInBackground() throws Exception {
        InputStream in = null;
        FileOutputStream fOut = null;

        try {
            // Membuka koneksi jaringan menggunakan URLConnection berbasis URI standar
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            // Menangani HTTP Redirect (301, 302, 303, 307, 308) jika menggunakan protokol HTTP/HTTPS
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) connection;
                http.setInstanceFollowRedirects(true);
                int statusCode = http.getResponseCode();
                int redirects = 0;

                while ((statusCode == HttpURLConnection.HTTP_MOVED_PERM
                        || statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || statusCode == 307 || statusCode == 308) && redirects < 5) {
                    String newLocation = http.getHeaderField("Location");
                    if (newLocation == null) break;

                    http.disconnect();
                    url = url.toURI().resolve(newLocation).toURL();
                    connection = url.openConnection();
                    connection.setConnectTimeout(12000);
                    connection.setReadTimeout(20000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                    if (connection instanceof HttpURLConnection) {
                        http = (HttpURLConnection) connection;
                        statusCode = http.getResponseCode();
                    }
                    redirects++;
                }

                // Cek jika server mengembalikan kode error HTTP
                if (statusCode >= 400) {
                    throw new IOException("Server merespons kode HTTP error: " + statusCode + " (" + http.getResponseMessage() + ")");
                }
            }

            // Mendapatkan nama file yang sesuai dan menentukan target file lokal
            String fileName = DownloadUtils.resolveFileName(url, connection);
            outputFile = new File(targetDir, fileName);

            // Mendapatkan total ukuran file dari header Content-Length
            long totalBytes = connection.getContentLengthLong();

            // Membuka input stream dari koneksi dan output stream ke file lokal
            in = connection.getInputStream();
            fOut = new FileOutputStream(outputFile);

            // Menggunakan sistem buffer byte berukuran 4096 byte (4 KB) sesuai ketentuan tugas
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalDownloaded = 0;

            // Variabel untuk melacak perhitungan kecepatan unduh real-time
            long lastSpeedCalcTime = System.currentTimeMillis();
            long bytesSinceLastCalc = 0;
            double speed = 0;

            // Loop pembacaan data dari input stream ke buffer byte
            while ((bytesRead = in.read(buffer)) != -1) {
                if (isCancelled()) {
                    break;
                }

                fOut.write(buffer, 0, bytesRead);
                totalDownloaded += bytesRead;
                bytesSinceLastCalc += bytesRead;

                long currentTime = System.currentTimeMillis();
                long timeElapsed = currentTime - lastSpeedCalcTime;

                // Perbarui dan kirim data progres setiap minimal 250 milidetik
                if (timeElapsed >= 250) {
                    speed = (bytesSinceLastCalc * 1000.0) / timeElapsed;
                    bytesSinceLastCalc = 0;
                    lastSpeedCalcTime = currentTime;

                    int percent = (totalBytes > 0) ? (int) ((totalDownloaded * 100) / totalBytes) : -1;
                    publish(new DownloadProgress(totalDownloaded, totalBytes, percent, speed, "Mengunduh file...", fileName));
                }
            }

            // Jika dibatalkan oleh pengguna, tutup stream dan hapus file yang belum selesai diunduh
            if (isCancelled()) {
                try {
                    fOut.flush();
                    fOut.close();
                } catch (Exception ignored) {}
                fOut = null;

                if (outputFile != null && outputFile.exists()) {
                    outputFile.delete();
                }
                return null;
            }

            // Kirim data progres akhir saat unduhan tuntas (100%)
            int finalPercent = (totalBytes > 0) ? 100 : -1;
            publish(new DownloadProgress(totalDownloaded, totalBytes, finalPercent, speed, "Unduhan selesai!", fileName));

            return outputFile;

        } finally {
            // Blok finally memastikan semua stream ditutup dengan aman setelah selesai / terjadi error
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
            if (fOut != null) {
                try {
                    fOut.flush();
                    fOut.close();
                } catch (Exception ignored) {}
            }
        }
    }

    // Method process dieksekusi di Event Dispatch Thread (EDT) untuk pembaruan GUI yang thread-safe
    @Override
    protected void process(List<DownloadProgress> chunks) {
        if (chunks.isEmpty() || isCancelled()) return;
        DownloadProgress latest = chunks.get(chunks.size() - 1);
        if (listener != null) {
            listener.onProgressUpdate(latest);
        }
    }

    // Method done dipanggil otomatis saat background task selesai, dibatalkan, atau gagal
    @Override
    protected void done() {
        if (listener == null) return;

        try {
            File result = get();
            if (result != null && result.exists()) {
                listener.onDownloadCompleted(result);
            }
        } catch (CancellationException e) {
            listener.onDownloadCancelled();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            Exception ex = (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
            listener.onDownloadFailed(ex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
