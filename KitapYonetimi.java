import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class KitapYonetimi extends JFrame {

    // Form Elemanları (Sol Panel)
    private JTextField txtKitapAdi, txtYazar, txtYayinevi, txtBasimYili, txtStokToplam, txtStokMevcut;
    private JComboBox<String> cbKategori;
    private JTable tablo;
    private DefaultTableModel model;

    // Veritabanı Bilgileri
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = ""; 

    public KitapYonetimi() {
        setTitle("Kitap Yönetimi Sistemi");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // --- SOL PANEL (Giriş Formu) ---
        JPanel solPanel = new JPanel();
        solPanel.setLayout(new BoxLayout(solPanel, BoxLayout.Y_AXIS));
        solPanel.setPreferredSize(new Dimension(320, 0));
        solPanel.setBorder(BorderFactory.createTitledBorder("Kitap Kayıt ve Filtreleme"));

        txtKitapAdi = new JTextField();
        txtYazar = new JTextField();
        // Kategori listesi (Filtreleme için 4-Hepsi seçeneği dahil)
        cbKategori = new JComboBox<>(new String[]{"1 (Roman)", "2 (Bilgisayar)", "3 (Bilim)", "4 (Hepsi)"});
        txtYayinevi = new JTextField();
        txtBasimYili = new JTextField();
        txtStokToplam = new JTextField();
        txtStokMevcut = new JTextField();

        solPanel.add(new JLabel(" Kitap Adı:"));
        solPanel.add(txtKitapAdi);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Yazar:"));
        solPanel.add(txtYazar);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Kategori:"));
        solPanel.add(cbKategori);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Yayınevi:"));
        solPanel.add(txtYayinevi);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Basım Yılı:"));
        solPanel.add(txtBasimYili);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Stok Adedi (Toplam):"));
        solPanel.add(txtStokToplam);
        solPanel.add(Box.createVerticalStrut(10));
        solPanel.add(new JLabel(" Stok Adedi (Mevcut):"));
        solPanel.add(txtStokMevcut);
        solPanel.add(Box.createVerticalStrut(25));

        // Butonlar
        JButton btnKaydet = new JButton("Kaydet / Ekle");
        JButton btnSil = new JButton("Seçiliyi Sil");
        JButton btnYenile = new JButton("Tabloyu Güncelle");
        JButton btnFiltrele = new JButton("Seçilenleri Filtrele"); // YENİ BUTON

        btnKaydet.setBackground(new Color(34, 139, 34)); 
        btnKaydet.setForeground(Color.WHITE);
        btnSil.setBackground(new Color(178, 34, 34)); 
        btnSil.setForeground(Color.WHITE);
        btnFiltrele.setBackground(new Color(70, 130, 180)); // Mavi tonu
        btnFiltrele.setForeground(Color.WHITE);

        solPanel.add(btnKaydet);
        solPanel.add(Box.createVerticalStrut(8));
        solPanel.add(btnSil);
        solPanel.add(Box.createVerticalStrut(8));
        solPanel.add(btnFiltrele); // Panele eklendi
        solPanel.add(Box.createVerticalStrut(8));
        solPanel.add(btnYenile);

        // --- SAĞ PANEL (Veri Tablosu) ---
        String[] kolonlar = {"ID", "Kitap Adı", "Yazar", "Kategori", "Yayınevi", "Yıl", "Mevcut / Toplam"};
        model = new DefaultTableModel(kolonlar, 0);
        tablo = new JTable(model);
        tablo.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(tablo);

        add(solPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        // --- ETKİLEŞİMLER ---
        btnYenile.addActionListener(e -> kitaplariGetir());
        btnKaydet.addActionListener(e -> kitapEkle());
        btnSil.addActionListener(e -> kitapSil());
        btnFiltrele.addActionListener(e -> kitaplariFiltrele()); // Filtrele butonu aktif

        kitaplariGetir();
    }

    // 1. VERİLERİ LİSTELEME
    private void kitaplariGetir() {
        model.setRowCount(0); 
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT k.kitap_id, k.kitap_adi, k.yazar, kat.kategori_adi, k.yayinevi, k.basim_yili, k.mevcut_adet, k.toplam_adet " +
                         "FROM kitap k " +
                         "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("kitap_id"),
                    rs.getString("kitap_adi"),
                    rs.getString("yazar"),
                    rs.getString("kategori_adi"),
                    rs.getString("yayinevi"),
                    rs.getInt("basim_yili"),
                    rs.getInt("mevcut_adet") + " / " + rs.getInt("toplam_adet")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veri çekme hatası: " + e.getMessage());
        }
    }

    // 2. KİTAP FİLTRELEME (Buton ile çalışır, Kategori dahildir)
    private void kitaplariFiltrele() {
        String kitapAdi = txtKitapAdi.getText().trim();
        String yazar = txtYazar.getText().trim();
        
        // ComboBox'tan ID çekme
        String seciliKategoriStr = cbKategori.getSelectedItem().toString();
        int kategoriId = Integer.parseInt(seciliKategoriStr.split(" ")[0]);

        model.setRowCount(0); 

        // SQL Başlangıcı
        String sql = "SELECT k.*, kat.kategori_adi FROM kitap k " +
                     "LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id " +
                     "WHERE 1=1";
        
        // Dinamik WHERE koşulları - boş alanlar kontrol edilir
        if (!kitapAdi.isEmpty()) {
            sql += " AND k.kitap_adi LIKE ?";
        }
        if (!yazar.isEmpty()) {
            sql += " AND k.yazar LIKE ?";
        }
        
        // Eğer "Hepsi" (ID 4) seçili değilse kategori şartı ekle
        if (kategoriId != 4) {
            sql += " AND k.kategori_id = " + kategoriId;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // PreparedStatement parametrelerini dinamik olarak set et
            int paramIndex = 1;
            if (!kitapAdi.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + kitapAdi + "%");
            }
            if (!yazar.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + yazar + "%");
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("kitap_id"),
                    rs.getString("kitap_adi"),
                    rs.getString("yazar"),
                    rs.getString("kategori_adi"),
                    rs.getString("yayinevi"),
                    rs.getInt("basim_yili"),
                    rs.getInt("mevcut_adet") + " / " + rs.getInt("toplam_adet")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Filtreleme hatası: " + e.getMessage());
        }
    }

    // 3. KİTAP EKLEME
    private void kitapEkle() {
        // Validasyon
        if (txtKitapAdi.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kitap adı boş olamaz!");
            return;
        }
        if (txtYazar.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Yazar adı boş olamaz!");
            return;
        }
        if (txtStokToplam.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Stok adedi boş olamaz!");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "INSERT INTO kitap (kitap_adi, yazar, kategori_id, yayinevi, basim_yili, toplam_adet, mevcut_adet) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            int seciliKategoriId = Integer.parseInt(cbKategori.getSelectedItem().toString().split(" ")[0]);
            int basimYili = Integer.parseInt(txtBasimYili.getText().isEmpty() ? "0" : txtBasimYili.getText());
            int stokToplam = Integer.parseInt(txtStokToplam.getText().trim());
            int stokMevcut = txtStokMevcut.getText().trim().isEmpty() ? stokToplam : Integer.parseInt(txtStokMevcut.getText().trim());

            pstmt.setString(1, txtKitapAdi.getText().trim());
            pstmt.setString(2, txtYazar.getText().trim());    
            pstmt.setInt(3, seciliKategoriId);         
            pstmt.setString(4, txtYayinevi.getText().trim()); 
            pstmt.setInt(5, basimYili);  
            pstmt.setInt(6, stokToplam); 
            pstmt.setInt(7, stokMevcut);

            pstmt.executeUpdate();
            
            // Log kaydı yaz
            String sqlLog = "INSERT INTO log_islem (islem_tipi, aciklama) VALUES (?, ?)";
            PreparedStatement psLog = conn.prepareStatement(sqlLog);
            psLog.setString(1, "Kitap Ekleme");
            psLog.setString(2, "Kitap: " + txtKitapAdi.getText().trim() + " | Yazar: " + txtYazar.getText().trim() + " | Stok: " + stokToplam);
            psLog.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Kitap başarıyla eklendi!");
            
            // Formu temizle
            txtKitapAdi.setText("");
            txtYazar.setText("");
            txtYayinevi.setText("");
            txtBasimYili.setText("");
            txtStokToplam.setText("");
            txtStokMevcut.setText("");
            cbKategori.setSelectedIndex(0);
            
            kitaplariGetir(); 
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Basım yılı ve stok adedi sayı olmalı!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Veritabanı hatası: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // 4. KİTAP SİLME
    private void kitapSil() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) {
            JOptionPane.showMessageDialog(this, "Silmek için tablodan bir kitap seçin!");
            return;
        }
        
        int id = (int) model.getValueAt(seciliSatir, 0);
        String kitapAdi = model.getValueAt(seciliSatir, 1).toString();
        int onay = JOptionPane.showConfirmDialog(this, "Emin misiniz?", "Silme Onayı", JOptionPane.YES_NO_OPTION);
        
        if (onay == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "DELETE FROM kitap WHERE kitap_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                
                // Log kaydı yaz
                String sqlLog = "INSERT INTO log_islem (islem_tipi, kitap_id, aciklama) VALUES (?, ?, ?)";
                PreparedStatement psLog = conn.prepareStatement(sqlLog);
                psLog.setString(1, "Kitap Silme");
                psLog.setInt(2, id);
                psLog.setString(3, "Silinen Kitap: " + kitapAdi);
                psLog.executeUpdate();
                
                kitaplariGetir(); 
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) { }
        SwingUtilities.invokeLater(() -> new KitapYonetimi().setVisible(true));
    }
}
