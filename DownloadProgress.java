public class DownloadProgress {
    private final long downloadedBytes;
    private final long totalBytes;
    private final int percent;
    private final double speedBytesPerSec;
    private final String statusText;
    private final String fileName;

    // Konstruktor untuk inisialisasi data progres
    public DownloadProgress(long downloadedBytes, long totalBytes, int percent,
                            double speedBytesPerSec, String statusText, String fileName) {
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.percent = percent;
        this.speedBytesPerSec = speedBytesPerSec;
        this.statusText = statusText;
        this.fileName = fileName;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getPercent() {
        return percent;
    }
    
    public double getSpeedBytesPerSec() {
        return speedBytesPerSec;
    }
    public String getStatusText() {
        return statusText;
    }

    public String getFileName() {
        return fileName;
    }
}
