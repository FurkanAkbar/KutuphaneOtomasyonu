import java.awt.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class DinamikSorgu extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model = new DefaultTableModel(
        new String[]{"Kitap ID", "Kitap Adı", "Yazar", "Kategori", "Yayınevi", "Basım Yılı", "Stok"}, 0);
    
    // Filtre Elemanları
    private JTextField txtKitapAdi = new JTextField(15);
    private JTextField txtYazar = new JTextField(15);
    private JComboBox<String> cbKategori = new JComboBox<>();
    private JTextField txtBasimYiliMin = new JTextField(8);
    private JTextField txtBasimYiliMax = new JTextField(8);
    private JCheckBox chkSadeceMevcut = new JCheckBox("Sadece Mevcut Kitaplar");
    private JComboBox<String> cbSirala = new JComboBox<>();
    
    private JButton btnAra = new JButton("ARA");
    private JButton btnSifirla = new JButton("SİFIRLA");
    private JButton btnExcel = new JButton("EXCEL İNDİR");
    private JButton btnPDF = new JButton("PDF İNDİR");
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = "";

    public DinamikSorgu() {
        setTitle("Dinamik Kitap Sorgu Sistemi");
        setSize(1400, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- SOL PANEL: FİLTRELER ---
        JPanel solPanel = new JPanel();
        solPanel.setLayout(new BoxLayout(solPanel, BoxLayout.Y_AXIS));
        solPanel.setPreferredSize(new Dimension(300, 0));
        solPanel.setBorder(BorderFactory.createTitledBorder("Dinamik Filtreler"));
        solPanel.setBackground(new Color(240, 240, 240));

        // Kategori ComboBox
        kategorileriYukle();
        
        // Sıralama ComboBox
        cbSirala.addItem("Kitap Adı (A→Z)");
        cbSirala.addItem("Kitap Adı (Z→A)");
        cbSirala.addItem("Yazar (A→Z)");
        cbSirala.addItem("Yazar (Z→A)");
        cbSirala.addItem("Basım Yılı (Eski→Yeni)");
        cbSirala.addItem("Basım Yılı (Yeni→Eski)");
        cbSirala.addItem("Stok Durumu (Az→Çok)");
        cbSirala.addItem("Stok Durumu (Çok→Az)");

        // Filtre Alanları
        solPanel.add(new JLabel("📚 Kitap Adı (LIKE):"));
        solPanel.add(txtKitapAdi);
        solPanel.add(Box.createVerticalStrut(10));
        
        solPanel.add(new JLabel("✍️ Yazar:"));
        solPanel.add(txtYazar);
        solPanel.add(Box.createVerticalStrut(10));
        
        solPanel.add(new JLabel("📂 Kategori:"));
        solPanel.add(cbKategori);
        solPanel.add(Box.createVerticalStrut(10));
        
        solPanel.add(new JLabel("📅 Basım Yılı Aralığı:"));
        JPanel yilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        yilPanel.setOpaque(false);
        yilPanel.add(new JLabel("Min:"));
        yilPanel.add(txtBasimYiliMin);
        yilPanel.add(new JLabel("Max:"));
        yilPanel.add(txtBasimYiliMax);
        solPanel.add(yilPanel);
        solPanel.add(Box.createVerticalStrut(10));
        
        chkSadeceMevcut.setOpaque(false);
        solPanel.add(chkSadeceMevcut);
        solPanel.add(Box.createVerticalStrut(15));
        
        solPanel.add(new JLabel("📊 Sırala:"));
        solPanel.add(cbSirala);
        solPanel.add(Box.createVerticalStrut(20));
        
        // Butonlar
        btnAra.setBackground(new Color(46, 204, 113));
        btnAra.setForeground(Color.WHITE);
        btnAra.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnSifirla.setBackground(new Color(149, 165, 166));
        btnSifirla.setForeground(Color.WHITE);
        btnSifirla.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnExcel.setBackground(new Color(52, 152, 219));
        btnExcel.setForeground(Color.WHITE);
        btnExcel.setFont(new Font("Arial", Font.BOLD, 11));
        
        btnPDF.setBackground(new Color(230, 126, 34));
        btnPDF.setForeground(Color.WHITE);
        btnPDF.setFont(new Font("Arial", Font.BOLD, 11));
        
        solPanel.add(btnAra);
        solPanel.add(Box.createVerticalStrut(8));
        solPanel.add(btnSifirla);
        solPanel.add(Box.createVerticalStrut(15));
        solPanel.add(btnExcel);
        solPanel.add(Box.createVerticalStrut(5));
        solPanel.add(btnPDF);
        solPanel.add(Box.createVerticalGlue());

        add(solPanel, BorderLayout.WEST);

        // --- SAĞ PANEL: TABLO ---
        tablo.setModel(model);
        tablo.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(tablo);
        add(scrollPane, BorderLayout.CENTER);

        // --- BUTON OLAYLARI ---
        btnAra.addActionListener(e -> dinamikSorgu());
        btnSifirla.addActionListener(e -> filtreleriSifirla());
        btnExcel.addActionListener(e -> excelIndir());
        btnPDF.addActionListener(e -> pdfIndir());

        dinamikSorgu(); // İlk yükleme
    }

    // Kategorileri veritabanından yükle
    private void kategorileriYukle() {
        cbKategori.addItem("-- Tümü --");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT kategori_id, kategori_adi FROM kategori";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cbKategori.addItem(rs.getInt(1) + " (" + rs.getString(2) + ")");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Kategori yükleme hatası: " + e.getMessage());
        }
    }

    // Dinamik sorgu - PreparedStatement ile SQL Injection'dan korunuyor
    private void dinamikSorgu() {
        model.setRowCount(0);

        String kitapAdi = txtKitapAdi.getText().trim();
        String yazar = txtYazar.getText().trim();
        String kategoriStr = (String) cbKategori.getSelectedItem();
        String basimYiliMin = txtBasimYiliMin.getText().trim();
        String basimYiliMax = txtBasimYiliMax.getText().trim();
        boolean sadeceMevcut = chkSadeceMevcut.isSelected();
        String siralamaStr = (String) cbSirala.getSelectedItem();

        // SQL Oluşturma (dinamik WHERE koşulları)
        StringBuilder sql = new StringBuilder(
            "SELECT k.kitap_id, k.kitap_adi, k.yazar, IFNULL(kat.kategori_adi, '-'), k.yayinevi, k.basim_yili, " +
            "CONCAT(k.mevcut_adet, ' / ', k.toplam_adet) AS stok " +
            "FROM kitap k " +
            "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id " +
            "WHERE 1=1"
        );
        
        java.util.List<Object> parametreler = new java.util.ArrayList<>();

        // Boş olmayan filtreler ekleniyor (PreparedStatement parametreleri ile)
        if (!kitapAdi.isEmpty()) {
            sql.append(" AND k.kitap_adi LIKE ?");
            parametreler.add("%" + kitapAdi + "%");
        }

        if (!yazar.isEmpty()) {
            sql.append(" AND k.yazar LIKE ?");
            parametreler.add("%" + yazar + "%");
        }

        // Kategori (ID'ye göre)
        if (!kategoriStr.equals("-- Tümü --")) {
            try {
                int kategoriId = Integer.parseInt(kategoriStr.split(" ")[0]);
                sql.append(" AND k.kategori_id = ?");
                parametreler.add(kategoriId);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Kategori hatası!");
                return;
            }
        }

        // Basım Yılı Aralığı
        if (!basimYiliMin.isEmpty()) {
            try {
                int min = Integer.parseInt(basimYiliMin);
                sql.append(" AND k.basim_yili >= ?");
                parametreler.add(min);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Basım Yılı Min geçersiz!");
                return;
            }
        }

        if (!basimYiliMax.isEmpty()) {
            try {
                int max = Integer.parseInt(basimYiliMax);
                sql.append(" AND k.basim_yili <= ?");
                parametreler.add(max);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Basım Yılı Max geçersiz!");
                return;
            }
        }

        // Sadece Mevcut
        if (sadeceMevcut) {
            sql.append(" AND k.mevcut_adet > 0");
        }

        // Sıralama (dinamik)
        String orderBy = switch (siralamaStr) {
            case "Kitap Adı (A→Z)" -> " ORDER BY k.kitap_adi ASC";
            case "Kitap Adı (Z→A)" -> " ORDER BY k.kitap_adi DESC";
            case "Yazar (A→Z)" -> " ORDER BY k.yazar ASC";
            case "Yazar (Z→A)" -> " ORDER BY k.yazar DESC";
            case "Basım Yılı (Eski→Yeni)" -> " ORDER BY k.basim_yili ASC";
            case "Basım Yılı (Yeni→Eski)" -> " ORDER BY k.basim_yili DESC";
            case "Stok Durumu (Az→Çok)" -> " ORDER BY k.mevcut_adet ASC";
            case "Stok Durumu (Çok→Az)" -> " ORDER BY k.mevcut_adet DESC";
            default -> " ORDER BY k.kitap_adi ASC";
        };
        sql.append(orderBy);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement pstmt = conn.prepareStatement(sql.toString());
            
            // Parametreleri ata
            for (int i = 0; i < parametreler.size(); i++) {
                Object param = parametreler.get(i);
                if (param instanceof Integer intVal) {
                    pstmt.setInt(i + 1, intVal);
                } else if (param instanceof String strVal) {
                    pstmt.setString(i + 1, strVal);
                }
            }
            
            ResultSet rs = pstmt.executeQuery();

            int satirSayisi = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getString(7)
                });
                satirSayisi++;
            }

            JOptionPane.showMessageDialog(this, "✓ " + satirSayisi + " sonuç bulundu.", "Arama Sonucu", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "❌ Sorgu hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Filtreleri sıfırla
    private void filtreleriSifirla() {
        txtKitapAdi.setText("");
        txtYazar.setText("");
        cbKategori.setSelectedIndex(0);
        txtBasimYiliMin.setText("");
        txtBasimYiliMax.setText("");
        chkSadeceMevcut.setSelected(false);
        cbSirala.setSelectedIndex(0);
        dinamikSorgu();
    }
    
    // Excel formatında indir
    private void excelIndir() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "İndirilecek veri yok! Önce arama yapın.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            String dosyaAdi = "Kitaplar_" + System.currentTimeMillis() + ".csv";
            try (FileWriter fw = new FileWriter(dosyaAdi);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                
                // Başlık satırı
                for (int i = 0; i < model.getColumnCount(); i++) {
                    bw.write(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) bw.write(";");
                }
                bw.newLine();
                
                // Veri satırları
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object value = model.getValueAt(i, j);
                        bw.write(value != null ? value.toString() : "");
                        if (j < model.getColumnCount() - 1) bw.write(";");
                    }
                    bw.newLine();
                }
            }
            
            JOptionPane.showMessageDialog(this, "✓ Veriler başarıyla " + dosyaAdi + " dosyasına kaydedildi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Dosya yazma hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // PDF formatında indir (Basit metin tablosu)
    private void pdfIndir() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "İndirilecek veri yok! Önce arama yapın.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            String dosyaAdi = "Kitaplar_" + System.currentTimeMillis() + ".txt";
            try (FileWriter fw = new FileWriter(dosyaAdi);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                
                bw.write("=".repeat(120));
                bw.newLine();
                bw.write("KÜTÜPHANEDEKİ KİTAPLAR - RAPOR");
                bw.newLine();
                bw.write("Tarih: " + new java.util.Date());
                bw.newLine();
                bw.write("=".repeat(120));
                bw.newLine();
                bw.newLine();
                
                // Başlık satırı
                bw.write(String.format("%-5s | %-30s | %-20s | %-15s | %-20s | %-8s | %-15s", 
                    "ID", "Kitap Adı", "Yazar", "Kategori", "Yayınevi", "Yıl", "Stok"));
                bw.newLine();
                bw.write("-".repeat(120));
                bw.newLine();
                
                // Veri satırları
                for (int i = 0; i < model.getRowCount(); i++) {
                    bw.write(String.format("%-5d | %-30s | %-20s | %-15s | %-20s | %-8s | %-15s",
                        model.getValueAt(i, 0),
                        model.getValueAt(i, 1).toString().substring(0, Math.min(30, model.getValueAt(i, 1).toString().length())),
                        model.getValueAt(i, 2).toString().substring(0, Math.min(20, model.getValueAt(i, 2).toString().length())),
                        model.getValueAt(i, 3).toString().substring(0, Math.min(15, model.getValueAt(i, 3).toString().length())),
                        model.getValueAt(i, 4).toString().substring(0, Math.min(20, model.getValueAt(i, 4).toString().length())),
                        model.getValueAt(i, 5),
                        model.getValueAt(i, 6)));
                    bw.newLine();
                }
                
                bw.write("-".repeat(120));
                bw.newLine();
                bw.write("Toplam Kitap: " + model.getRowCount());
                bw.newLine();
                bw.write("=".repeat(120));
            }
            
            JOptionPane.showMessageDialog(this, "✓ Rapor başarıyla " + dosyaAdi + " dosyasına kaydedildi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Dosya yazma hatası: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DinamikSorgu().setVisible(true));
    }
}
