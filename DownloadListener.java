import java.io.File;
public interface DownloadListener {
    void onProgressUpdate(DownloadProgress progress);
    void onDownloadCompleted(File downloadedFile);
    void onDownloadCancelled();
    void onDownloadFailed(Exception exception);
}
