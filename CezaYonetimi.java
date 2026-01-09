import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CezaYonetimi extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model = new DefaultTableModel(
        new String[]{"Ceza ID", "Üye Ad Soyad", "Kitap", "Gecikme (Gün)", "Ceza (TL)", "Ceza Tarihi"}, 0);
    
    private JButton btnGoster = new JButton("Cezaları Göster");
    private JButton btnGecenKitaplar = new JButton("Gecen Kitapları Göster");
    private JButton btnOyat = new JButton("Cezayı Ödedi");
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = "";

    public CezaYonetimi() {
        setTitle("Ceza Yönetim Sistemi");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- ÜST PANEL: Butonlar ---
        JPanel ustPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        ustPanel.setBackground(new Color(60, 63, 65));
        
        btnGoster.setBackground(new Color(41, 128, 185));
        btnGoster.setForeground(Color.WHITE);
        btnGoster.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnGecenKitaplar.setBackground(new Color(230, 126, 34));
        btnGecenKitaplar.setForeground(Color.WHITE);
        btnGecenKitaplar.setFont(new Font("Arial", Font.BOLD, 12));
        
        btnOyat.setBackground(new Color(46, 204, 113));
        btnOyat.setForeground(Color.WHITE);
        btnOyat.setFont(new Font("Arial", Font.BOLD, 12));
        
        ustPanel.add(btnGoster);
        ustPanel.add(btnGecenKitaplar);
        ustPanel.add(btnOyat);
        
        add(ustPanel, BorderLayout.NORTH);

        // --- MERKEZ PANEL: Tablo ---
        tablo.setModel(model);
        tablo.setRowHeight(25);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // --- BUTON OLAYLARI ---
        btnGoster.addActionListener(e -> cezalariGoster());
        btnGecenKitaplar.addActionListener(e -> gecenKitaplariGoster());
        btnOyat.addActionListener(e -> cezaOyat());

        cezalariGoster();
    }

    // Tüm cezaları göster
    private void cezalariGoster() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT c.ceza_id, CONCAT(u.ad, ' ', u.soyad), k.kitap_adi, c.gecikme_gun, c.ceza_tutari, c.ceza_tarihi " +
                         "FROM ceza c " +
                         "JOIN uye u ON c.UyeID = u.uye_id " +
                         "LEFT JOIN odunc o ON c.OduncID = o.OduncID " +
                         "LEFT JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "ORDER BY c.ceza_tarihi DESC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getDouble(5),
                    rs.getTimestamp(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // Gecen kitapları göster (son teslim tarihi geçmiş, iade edilmemiş)
    private void gecenKitaplariGoster() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT c.ceza_id, CONCAT(u.ad, ' ', u.soyad), k.kitap_adi, " +
                         "DATEDIFF(CURDATE(), o.SonTeslimTarihi) AS gecikme_gun, " +
                         "(DATEDIFF(CURDATE(), o.SonTeslimTarihi) * 5) AS ceza_tutari, " +
                         "o.SonTeslimTarihi " +
                         "FROM odunc o " +
                         "JOIN uye u ON o.UyeID = u.uye_id " +
                         "JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "LEFT JOIN ceza c ON o.OduncID = c.OduncID " +
                         "WHERE o.IadeTarihi IS NULL AND o.SonTeslimTarihi < CURDATE() " +
                         "ORDER BY o.SonTeslimTarihi ASC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getDouble(5),
                    rs.getDate(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // Cezayı ödedi (seçilen satırı sil/işle ve borcu düş)
    private void cezaOyat() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen ödenen cezayı seçin!");
            return;
        }
        
        int cezaId = (int) model.getValueAt(seciliSatir, 0);
        String uyeAdi = (String) model.getValueAt(seciliSatir, 1);
        double cezaTutari = (double) model.getValueAt(seciliSatir, 4);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            uyeAdi + " üyesinin " + cezaTutari + " TL cezasını ödedi olarak işaretleyecek misiniz?",
            "Ceza Ödeme Onayı", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                conn.setAutoCommit(false);
                
                // Ceza kaydından üye ID'sini al
                String sqlGetUyeId = "SELECT UyeID FROM ceza WHERE ceza_id = ?";
                PreparedStatement pstmtGetId = conn.prepareStatement(sqlGetUyeId);
                pstmtGetId.setInt(1, cezaId);
                ResultSet rs = pstmtGetId.executeQuery();
                
                int uyeId = 0;
                if (rs.next()) {
                    uyeId = rs.getInt(1);
                }
                
                // 1. Ceza kaydını sil
                String sqlDeleteCeza = "DELETE FROM ceza WHERE ceza_id = ?";
                PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteCeza);
                pstmtDelete.setInt(1, cezaId);
                pstmtDelete.executeUpdate();
                
                // 2. Üyenin borcunu düş
                if (uyeId > 0) {
                    String sqlUpdateBorc = "UPDATE uye SET toplam_borc = toplam_borc - ? WHERE uye_id = ?";
                    PreparedStatement pstmtBorc = conn.prepareStatement(sqlUpdateBorc);
                    pstmtBorc.setDouble(1, cezaTutari);
                    pstmtBorc.setInt(2, uyeId);
                    pstmtBorc.executeUpdate();
                }
                
                // 3. Log kaydı yaz
                String sqlLog = "INSERT INTO log_islem (islem_tipi, uye_id, aciklama) VALUES (?, ?, ?)";
                PreparedStatement pstmtLog = conn.prepareStatement(sqlLog);
                pstmtLog.setString(1, "Ceza Ödeme");
                pstmtLog.setInt(2, uyeId);
                pstmtLog.setString(3, "Ceza ID: " + cezaId + " | Tutar: " + cezaTutari + " TL | Ödendi olarak işaretlendi");
                pstmtLog.executeUpdate();
                
                conn.commit();
                conn.setAutoCommit(true);
                
                JOptionPane.showMessageDialog(this, "✓ " + cezaTutari + " TL ceza başarıyla ödendi!\nÜyenin borcu güncellendi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                cezalariGoster();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CezaYonetimi().setVisible(true));
    }
}
