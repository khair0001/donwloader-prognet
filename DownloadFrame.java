import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class DownloadFrame extends JFrame implements DownloadListener {

    // Palet Warna Desain Modern Dark Mode
    private static final Color COLOR_BG = new Color(17, 24, 39);
    private static final Color COLOR_PANEL_BG = new Color(31, 41, 55);
    private static final Color COLOR_PANEL_BORDER = new Color(55, 65, 81);
    private static final Color COLOR_INPUT_BG = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_PRIMARY = new Color(243, 244, 246);
    private static final Color COLOR_TEXT_MUTED = new Color(156, 163, 175);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_DANGER = new Color(239, 68, 68);
    private static final Color COLOR_DANGER_HOVER = new Color(220, 38, 38);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_SUCCESS_HOVER = new Color(5, 150, 105);

    // Komponen Input Antarmuka
    private JTextField txtUrl;
    private JTextField txtFolder;
    private RoundedButton btnBrowse;

    // Komponen Tombol Aksi
    private RoundedButton btnDownload;
    private RoundedButton btnCancel;
    private RoundedButton btnOpenFolder;

    // Komponen Progress Bar, Grafik Kecepatan, & Stat Cards
    private ModernProgressBar progressBar;
    private SpeedGraphPanel speedGraph;
    private StatCard cardStatus;
    private StatCard cardFile;
    private StatCard cardSize;
    private StatCard cardSpeed;

    private DownloadTask currentTask;
    private File lastDownloadedFolder;

    public DownloadFrame() {
        super("Java GUI File Downloader");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(680, 640);
        setMinimumSize(new Dimension(640, 580));
        setLocationRelativeTo(null);

        // Listener konfirmasi keluar saat proses unduhan sedang berjalan
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentTask != null && !currentTask.isDone()) {
                    int confirm = JOptionPane.showConfirmDialog(
                            DownloadFrame.this,
                            "Unduhan sedang berjalan. Apakah Anda yakin ingin membatalkan dan keluar?",
                            "Konfirmasi Keluar",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (confirm == JOptionPane.YES_OPTION) {
                        currentTask.cancel(true);
                        dispose();
                        System.exit(0);
                    }
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBackground(COLOR_BG);

        // Header Banner dengan Gradien
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 58, 138), getWidth(), 0, new Color(67, 56, 202));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("File Downloader");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Tugas Kelompok 3");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(209, 213, 219));

        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(lblTitle);
        headerTextPanel.add(lblSubtitle);
        headerPanel.add(headerTextPanel, BorderLayout.WEST);

        rootPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Kartu Input Konfigurasi
        JPanel formCard = createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblUrl = createFormLabel("URL Unduhan:");
        formCard.add(lblUrl, gbc);

        txtUrl = createStyledTextField("https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3");
        txtUrl.setToolTipText("Masukkan tautan URL file yang ingin diunduh (http:// atau https://)");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formCard.add(txtUrl, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JLabel lblDest = createFormLabel("Folder Simpan:");
        formCard.add(lblDest, gbc);

        String defaultDir = System.getProperty("user.home") + File.separator + "Downloads";
        if (!new File(defaultDir).exists()) {
            defaultDir = System.getProperty("user.home");
        }
        txtFolder = createStyledTextField(defaultDir);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        formCard.add(txtFolder, gbc);

        btnBrowse = new RoundedButton("Browse...", new Color(75, 85, 99), new Color(107, 114, 128), new Color(55, 65, 81));
        btnBrowse.setPreferredSize(new Dimension(95, 32));
        btnBrowse.setToolTipText("Pilih folder penyimpanan file di komputer");
        btnBrowse.addActionListener(e -> browseFolder());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formCard.add(btnBrowse, gbc);

        contentPanel.add(formCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Baris Tombol Aksi
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttonPanel.setOpaque(false);

        btnDownload = new RoundedButton("Download", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, new Color(30, 64, 175));
        btnDownload.setPreferredSize(new Dimension(130, 36));
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDownload.addActionListener(e -> startDownload());

        btnCancel = new RoundedButton("Batal", COLOR_DANGER, COLOR_DANGER_HOVER, new Color(185, 28, 28));
        btnCancel.setPreferredSize(new Dimension(110, 36));
        btnCancel.setEnabled(false);
        btnCancel.addActionListener(e -> cancelDownload());

        btnOpenFolder = new RoundedButton("Buka Folder", COLOR_SUCCESS, COLOR_SUCCESS_HOVER, new Color(4, 120, 87));
        btnOpenFolder.setPreferredSize(new Dimension(130, 36));
        btnOpenFolder.setEnabled(false);
        btnOpenFolder.addActionListener(e -> openDownloadFolder());

        buttonPanel.add(btnDownload);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnOpenFolder);

        contentPanel.add(buttonPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Progress Bar Modern
        progressBar = new ModernProgressBar(0, 100);
        progressBar.setString("Siap untuk mengunduh");
        contentPanel.add(progressBar);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Grafik Kecepatan Unduh Real-time
        speedGraph = new SpeedGraphPanel();
        contentPanel.add(speedGraph);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Dashboard Stat Cards (Grid 2x2)
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 8));
        statsPanel.setOpaque(false);

        cardStatus = new StatCard("STATUS KONEKSI", "Menunggu unduhan...", new Color(147, 197, 253));
        cardFile = new StatCard("NAMA FILE", "-", COLOR_TEXT_PRIMARY);
        cardSize = new StatCard("UKURAN FILE", "0 B / 0 B", COLOR_TEXT_PRIMARY);
        cardSpeed = new StatCard("KECEPATAN TRANSFER", "0 KB/s", new Color(52, 211, 153));

        statsPanel.add(cardStatus);
        statsPanel.add(cardFile);
        statsPanel.add(cardSize);
        statsPanel.add(cardSpeed);

        contentPanel.add(statsPanel);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        footerPanel.setBackground(new Color(15, 23, 42));
        footerPanel.setBorder(new LineBorder(COLOR_PANEL_BORDER, 1));

        JLabel lblFooter = new JLabel("Prognet © 2026 • Java Swing Network Downloader");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFooter.setForeground(COLOR_TEXT_MUTED);
        footerPanel.add(lblFooter);

        rootPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_PANEL_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_PANEL_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(COLOR_TEXT_PRIMARY);
        return label;
    }

    private JTextField createStyledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBackground(COLOR_INPUT_BG);
        tf.setForeground(COLOR_TEXT_PRIMARY);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(new CompoundBorder(
                new LineBorder(COLOR_PANEL_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private void browseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Folder Tujuan Unduhan");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        File current = new File(txtFolder.getText().trim());
        if (current.exists() && current.isDirectory()) {
            chooser.setCurrentDirectory(current);
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            txtFolder.setText(selectedFolder.getAbsolutePath());
        }
    }

    private boolean validateInputs() {
        String urlText = txtUrl.getText().trim();
        String folderText = txtFolder.getText().trim();

        if (urlText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Kolom URL unduhan tidak boleh kosong!\nSilakan masukkan tautan unduhan.",
                    "Validasi Input", JOptionPane.WARNING_MESSAGE);
            txtUrl.requestFocus();
            return false;
        }

        try {
            URI uri = URI.create(urlText);
            URL url = uri.toURL();
            String protocol = url.getProtocol();
            if (protocol == null || (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https"))) {
                JOptionPane.showMessageDialog(this,
                        "Format URL tidak valid!\nTautan harus menggunakan protokol HTTP atau HTTPS (contoh: https://...).",
                        "Validasi Input", JOptionPane.WARNING_MESSAGE);
                txtUrl.requestFocus();
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Format URL tidak valid:\n" + e.getMessage(),
                    "Validasi Input", JOptionPane.WARNING_MESSAGE);
            txtUrl.requestFocus();
            return false;
        }

        if (folderText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Folder tujuan penyimpanan belum ditentukan!",
                    "Validasi Input", JOptionPane.WARNING_MESSAGE);
            btnBrowse.requestFocus();
            return false;
        }

        File folder = new File(folderText);
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "Folder tujuan penyimpanan tidak ditemukan atau bukan folder yang valid!",
                    "Validasi Input", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    private void startDownload() {
        if (!validateInputs()) {
            return;
        }

        String downloadUrl = txtUrl.getText().trim();
        File targetDir = new File(txtFolder.getText().trim());

        // Reset komponen tampilan status progres dan grafik
        progressBar.setValue(0);
        progressBar.setString("Menghubungkan...");
        progressBar.setIndeterminate(false);

        speedGraph.reset();

        cardStatus.setValue("Menghubungkan...");
        cardStatus.setValueColor(new Color(147, 197, 253));
        cardFile.setValue("Memeriksa...");
        cardSize.setValue("Menghitung...");
        cardSpeed.setValue("0 KB/s");
        btnOpenFolder.setEnabled(false);

        setUiDownloading(true);

        currentTask = new DownloadTask(downloadUrl, targetDir, this);
        currentTask.execute();
    }

    private void cancelDownload() {
        if (currentTask != null && !currentTask.isDone()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Apakah Anda yakin ingin membatalkan proses unduhan ini?",
                    "Konfirmasi Batal",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                currentTask.cancel(true);
            }
        }
    }

    private void openDownloadFolder() {
        DownloadUtils.openFolder(lastDownloadedFolder, this);
    }

    private void setUiDownloading(boolean isDownloading) {
        btnDownload.setEnabled(!isDownloading);
        btnCancel.setEnabled(isDownloading);
        btnBrowse.setEnabled(!isDownloading);
        txtUrl.setEnabled(!isDownloading);
        txtFolder.setEnabled(!isDownloading);
    }

    @Override
    public void onProgressUpdate(DownloadProgress progress) {
        cardFile.setValue(progress.getFileName());
        cardStatus.setValue(progress.getStatusText());
        cardSpeed.setValue(DownloadUtils.formatSpeed(progress.getSpeedBytesPerSec()));

        // Tambahkan sampel kecepatan ke grafik real-time
        speedGraph.addSpeedSample(progress.getSpeedBytesPerSec());

        if (progress.getTotalBytes() > 0) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progress.getPercent());
            progressBar.setString(progress.getPercent() + "%");
            cardSize.setValue(DownloadUtils.formatBytes(progress.getDownloadedBytes())
                    + " / " + DownloadUtils.formatBytes(progress.getTotalBytes()));
        } else {
            progressBar.setIndeterminate(true);
            progressBar.setString("Mengunduh...");
            cardSize.setValue(DownloadUtils.formatBytes(progress.getDownloadedBytes())
                    + " / Tidak Diketahui");
        }
    }

    @Override
    public void onDownloadCompleted(File downloadedFile) {
        setUiDownloading(false);
        lastDownloadedFolder = downloadedFile.getParentFile();
        btnOpenFolder.setEnabled(true);

        speedGraph.addSpeedSample(0.0);

        cardStatus.setValue("Selesai!");
        cardStatus.setValueColor(new Color(52, 211, 153));
        cardSpeed.setValue("0 KB/s");
        progressBar.setValue(100);
        progressBar.setString("100% Selesai");
        progressBar.setIndeterminate(false);

        JOptionPane.showMessageDialog(
                this,
                "File berhasil diunduh dengan sukses!\n\n"
                        + "Nama File: " + downloadedFile.getName() + "\n"
                        + "Lokasi: " + downloadedFile.getAbsolutePath() + "\n"
                        + "Ukuran: " + DownloadUtils.formatBytes(downloadedFile.length()),
                "Unduhan Berhasil",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void onDownloadCancelled() {
        setUiDownloading(false);
        speedGraph.addSpeedSample(0.0);

        cardStatus.setValue("Unduhan Dibatalkan");
        cardStatus.setValueColor(new Color(251, 191, 36));
        cardSpeed.setValue("0 KB/s");
        progressBar.setValue(0);
        progressBar.setString("Dibatalkan");
        progressBar.setIndeterminate(false);

        JOptionPane.showMessageDialog(
                this,
                "Proses pengunduhan file telah dibatalkan oleh pengguna.",
                "Unduhan Dibatalkan",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void onDownloadFailed(Exception exception) {
        setUiDownloading(false);
        speedGraph.addSpeedSample(0.0);

        cardStatus.setValue("Gagal");
        cardStatus.setValueColor(new Color(248, 113, 113));
        cardSpeed.setValue("0 KB/s");
        progressBar.setValue(0);
        progressBar.setString("Gagal");
        progressBar.setIndeterminate(false);

        JOptionPane.showMessageDialog(
                this,
                "Terjadi kesalahan saat mengunduh file:\n" + exception.getMessage(),
                "Kesalahan Unduhan",
                JOptionPane.ERROR_MESSAGE
        );
    }

    static class RoundedButton extends JButton {
        private final Color normalColor;
        private final Color hoverColor;
        private final Color pressedColor;
        private final Color disabledColor = new Color(55, 65, 81);
        private final int cornerRadius = 8;

        public RoundedButton(String text, Color normalColor, Color hoverColor, Color pressedColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            this.pressedColor = pressedColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(disabledColor);
            } else if (getModel().isPressed()) {
                g2.setColor(pressedColor);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(normalColor);
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ModernProgressBar extends JProgressBar {
        private final Color trackColor = new Color(15, 23, 42);
        private final Color startGradient = new Color(6, 182, 212);
        private final Color endGradient = new Color(59, 130, 246);

        public ModernProgressBar(int min, int max) {
            super(min, max);
            setStringPainted(true);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorderPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(getPreferredSize().width, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(trackColor);
            g2.fillRoundRect(0, 0, width, height, 8, 8);

            g2.setColor(new Color(55, 65, 81));
            g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);

            if (isIndeterminate()) {
                super.paintComponent(g);
                g2.dispose();
                return;
            }

            int fillWidth = (int) (width * getPercentComplete());
            if (fillWidth > 0) {
                GradientPaint gradient = new GradientPaint(0, 0, startGradient, width, 0, endGradient);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, fillWidth, height, 8, 8);
            }

            String text = getString();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (width - textWidth) / 2;
                int y = (height + textHeight) / 2 - 2;

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                g2.drawString(text, x, y);
            }

            g2.dispose();
        }
    }

    static class SpeedGraphPanel extends JPanel {
        private final LinkedList<Double> history = new LinkedList<>();
        private static final int MAX_SAMPLES = 48;
        private double maxSpeedSeen = 1024 * 512;

        public SpeedGraphPanel() {
            setPreferredSize(new Dimension(Integer.MAX_VALUE, 105));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
            setBackground(COLOR_PANEL_BG);
            setBorder(new CompoundBorder(
                    new LineBorder(COLOR_PANEL_BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
            reset();
        }

        public synchronized void reset() {
            history.clear();
            for (int i = 0; i < MAX_SAMPLES; i++) {
                history.add(0.0);
            }
            maxSpeedSeen = 1024 * 512;
            repaint();
        }

        public synchronized void addSpeedSample(double speedBytesPerSec) {
            if (history.size() >= MAX_SAMPLES) {
                history.removeFirst();
            }
            history.add(speedBytesPerSec);
            if (speedBytesPerSec > maxSpeedSeen) {
                maxSpeedSeen = speedBytesPerSec;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Background grafik
            g2.setColor(new Color(15, 23, 42));
            g2.fillRoundRect(0, 0, width, height, 8, 8);

            int padLeft = 14;
            int padRight = 14;
            int padTop = 24;
            int padBottom = 16;
            int plotW = width - padLeft - padRight;
            int plotH = height - padTop - padBottom;

            if (plotW <= 0 || plotH <= 0) {
                g2.dispose();
                return;
            }

            g2.setColor(new Color(30, 41, 59));
            for (int i = 0; i <= 3; i++) {
                int y = padTop + (plotH * i) / 3;
                g2.drawLine(padLeft, y, width - padRight, y);
            }

            // Label Header Grafik & Puncak Kecepatan
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("📈 GRAFIK KECEPATAN REAL-TIME", padLeft, 16);

            String peakStr = "Puncak: " + DownloadUtils.formatSpeed(maxSpeedSeen);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(peakStr, width - padRight - fm.stringWidth(peakStr), 16);

            // Titik data kurva
            Double[] data;
            synchronized (this) {
                data = history.toArray(new Double[0]);
            }

            int n = data.length;
            if (n < 2) {
                g2.dispose();
                return;
            }

            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            double scale = Math.max(maxSpeedSeen, 1024.0);

            for (int i = 0; i < n; i++) {
                xPoints[i] = padLeft + (i * plotW) / (n - 1);
                double val = data[i];
                double ratio = Math.min(1.0, val / scale);
                yPoints[i] = padTop + plotH - (int) (ratio * plotH);
            }

            Polygon poly = new Polygon();
            poly.addPoint(xPoints[0], padTop + plotH);
            for (int i = 0; i < n; i++) {
                poly.addPoint(xPoints[i], yPoints[i]);
            }
            poly.addPoint(xPoints[n - 1], padTop + plotH);

            GradientPaint fillGradient = new GradientPaint(
                    0, padTop, new Color(6, 182, 212, 100),
                    0, padTop + plotH, new Color(59, 130, 246, 15)
            );
            g2.setPaint(fillGradient);
            g2.fillPolygon(poly);

            g2.setColor(new Color(6, 182, 212));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                g2.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            int lastX = xPoints[n - 1];
            int lastY = yPoints[n - 1];
            g2.setColor(new Color(34, 211, 238));
            g2.fillOval(lastX - 4, lastY - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.fillOval(lastX - 2, lastY - 2, 4, 4);

            g2.dispose();
        }
    }

    static class StatCard extends JPanel {
        private final JLabel lblTitle;
        private final JLabel lblValue;

        public StatCard(String title, String initialValue, Color valueColor) {
            setLayout(new BorderLayout(4, 4));
            setBackground(COLOR_PANEL_BG);
            setBorder(new CompoundBorder(
                    new LineBorder(COLOR_PANEL_BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));

            lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblTitle.setForeground(COLOR_TEXT_MUTED);

            lblValue = new JLabel(initialValue);
            lblValue.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblValue.setForeground(valueColor != null ? valueColor : COLOR_TEXT_PRIMARY);

            add(lblTitle, BorderLayout.NORTH);
            add(lblValue, BorderLayout.CENTER);
        }

        public void setValue(String value) {
            lblValue.setText(value);
        }

        public void setValueColor(Color color) {
            lblValue.setForeground(color);
        }
    }

    public static void main(String[] args) {
        DownloadFile.main(args);
    }
}
