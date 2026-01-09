import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Raporlar extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model;
    
    private JButton btnOduncRapor = new JButton("Ödünç Raporu");
    private JButton btnGecenRapor = new JButton("Gecen Kitaplar");
    private JButton btnEnCokRapor = new JButton("En Çok Ödünç Alınanlar");
    private JButton btnUyeRapor = new JButton("Üye Raporu");
    private JButton btnCezaRapor = new JButton("Ceza Raporu");
    
    private JLabel lblTarihBas = new JLabel("Başlangıç Tarihi:");
    private JLabel lblTarihBit = new JLabel("Bitiş Tarihi:");
    private JTextField txtTarihBas = new JTextField(10);
    private JTextField txtTarihBit = new JTextField(10);
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = "";

    public Raporlar() {
        setTitle("Kütüphane - Raporlar");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- ÜST PANEL: Tarih Filtreleri ve Butonlar ---
        JPanel ustPanel = new JPanel(new BorderLayout(10, 10));
        ustPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ustPanel.setBackground(new Color(60, 63, 65));
        
        // Tarih Paneli
        JPanel tarihPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tarihPanel.setOpaque(false);
        lblTarihBas.setForeground(Color.WHITE);
        lblTarihBit.setForeground(Color.WHITE);
        tarihPanel.add(lblTarihBas);
        tarihPanel.add(txtTarihBas);
        tarihPanel.add(lblTarihBit);
        tarihPanel.add(txtTarihBit);
        
        // Buton Paneli
        JPanel butonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        butonPanel.setOpaque(false);
        
        btnOduncRapor.setBackground(new Color(41, 128, 185));
        btnOduncRapor.setForeground(Color.WHITE);
        btnGecenRapor.setBackground(new Color(230, 126, 34));
        btnGecenRapor.setForeground(Color.WHITE);
        btnEnCokRapor.setBackground(new Color(155, 89, 182));
        btnEnCokRapor.setForeground(Color.WHITE);
        btnUyeRapor.setBackground(new Color(46, 204, 113));
        btnUyeRapor.setForeground(Color.WHITE);
        btnCezaRapor.setBackground(new Color(231, 76, 60));
        btnCezaRapor.setForeground(Color.WHITE);
        
        butonPanel.add(btnOduncRapor);
        butonPanel.add(btnGecenRapor);
        butonPanel.add(btnEnCokRapor);
        butonPanel.add(btnUyeRapor);
        butonPanel.add(btnCezaRapor);
        
        ustPanel.add(tarihPanel, BorderLayout.NORTH);
        ustPanel.add(butonPanel, BorderLayout.CENTER);
        
        add(ustPanel, BorderLayout.NORTH);

        // --- MERKEZ PANEL: Tablo ---
        model = new DefaultTableModel();
        tablo.setModel(model);
        tablo.setRowHeight(25);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // --- BUTON OLAYLARI ---
        btnOduncRapor.addActionListener(e -> oduncRaporu());
        btnGecenRapor.addActionListener(e -> gecenKitaplarRaporu());
        btnEnCokRapor.addActionListener(e -> enCokOduncAlinanRapor());
        btnUyeRapor.addActionListener(e -> uyeRaporu());
        btnCezaRapor.addActionListener(e -> cezaRaporu());

        oduncRaporu();
    }

    // Tarih kontrolü ve formatlama
    private String kontolEtTarih(String tarih) {
        if (tarih.trim().isEmpty()) return null;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy");
            java.util.Date date = sdf.parse(tarih);
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            return sqlDate.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // 1. Ödünç Raporu (Tarih aralığına göre)
    private void oduncRaporu() {
        model.setRowCount(0);
        model.setColumnCount(0);
        
        String tarihBas = kontolEtTarih(txtTarihBas.getText());
        String tarihBit = kontolEtTarih(txtTarihBit.getText());
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT o.OduncID, CONCAT(u.ad, ' ', u.soyad) AS uye, k.kitap_adi, o.OduncTarihi, o.SonTeslimTarihi, " +
                         "IF(o.IadeTarihi IS NULL, 'Beklemede', 'İade Eddi') AS durum " +
                         "FROM odunc o " +
                         "JOIN uye u ON o.UyeID = u.uye_id " +
                         "JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "WHERE 1=1";
            
            if (tarihBas != null) sql += " AND o.OduncTarihi >= '" + tarihBas + "'";
            if (tarihBit != null) sql += " AND o.OduncTarihi <= '" + tarihBit + "'";
            sql += " ORDER BY o.OduncTarihi DESC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            // Sütunları dinamik olarak ayarla
            model.addColumn("Ödünç ID");
            model.addColumn("Üye");
            model.addColumn("Kitap");
            model.addColumn("Ödünç Tarihi");
            model.addColumn("Son Tarih");
            model.addColumn("Durum");
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getTimestamp(4), rs.getDate(5), rs.getString(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // 2. Gecen Kitaplar Raporu
    private void gecenKitaplarRaporu() {
        model.setRowCount(0);
        model.setColumnCount(0);
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT o.OduncID, CONCAT(u.ad, ' ', u.soyad) AS uye, k.kitap_adi, o.SonTeslimTarihi, " +
                         "DATEDIFF(CURDATE(), o.SonTeslimTarihi) AS gecikme_gun, " +
                         "(DATEDIFF(CURDATE(), o.SonTeslimTarihi) * 5) AS tahmini_ceza " +
                         "FROM odunc o " +
                         "JOIN uye u ON o.UyeID = u.uye_id " +
                         "JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "WHERE o.IadeTarihi IS NULL AND o.SonTeslimTarihi < CURDATE() " +
                         "ORDER BY o.SonTeslimTarihi ASC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            model.addColumn("Ödünç ID");
            model.addColumn("Üye");
            model.addColumn("Kitap");
            model.addColumn("Son Tarih");
            model.addColumn("Gecikme (Gün)");
            model.addColumn("Tahmini Ceza (TL)");
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getDate(4), rs.getInt(5), rs.getDouble(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // 3. En Çok Ödünç Alınan Kitaplar
    private void enCokOduncAlinanRapor() {
        model.setRowCount(0);
        model.setColumnCount(0);
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT k.kitap_id, k.kitap_adi, COUNT(o.OduncID) AS odunc_sayisi, " +
                         "SUM(CASE WHEN o.IadeTarihi IS NULL THEN 1 ELSE 0 END) AS mevcut_odunc " +
                         "FROM kitap k " +
                         "LEFT JOIN odunc o ON k.kitap_id = o.KitapID " +
                         "GROUP BY k.kitap_id, k.kitap_adi " +
                         "ORDER BY odunc_sayisi DESC " +
                         "LIMIT 20";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            model.addColumn("Kitap ID");
            model.addColumn("Kitap Adı");
            model.addColumn("Toplam Ödünç");
            model.addColumn("Mevcut Ödünç");
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // 4. Üye Raporu (Detaylı)
    private void uyeRaporu() {
        model.setRowCount(0);
        model.setColumnCount(0);
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT u.uye_id, CONCAT(u.ad, ' ', u.soyad) AS uye, u.email, u.telefon, " +
                         "COUNT(DISTINCT o.OduncID) AS toplam_odunc, " +
                         "SUM(CASE WHEN o.IadeTarihi IS NULL THEN 1 ELSE 0 END) AS aktif_odunc, " +
                         "u.toplam_borc " +
                         "FROM uye u " +
                         "LEFT JOIN odunc o ON u.uye_id = o.UyeID " +
                         "GROUP BY u.uye_id " +
                         "ORDER BY u.toplam_borc DESC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            model.addColumn("Üye ID");
            model.addColumn("Üye Adı");
            model.addColumn("Email");
            model.addColumn("Telefon");
            model.addColumn("Toplam Ödünç");
            model.addColumn("Aktif Ödünç");
            model.addColumn("Borç (TL)");
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getInt(5), rs.getInt(6), rs.getDouble(7)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    // 5. Ceza Raporu
    private void cezaRaporu() {
        model.setRowCount(0);
        model.setColumnCount(0);
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT c.ceza_id, CONCAT(u.ad, ' ', u.soyad) AS uye, k.kitap_adi, " +
                         "c.gecikme_gun, c.ceza_tutari, c.ceza_tarihi " +
                         "FROM ceza c " +
                         "JOIN uye u ON c.UyeID = u.uye_id " +
                         "LEFT JOIN odunc o ON c.OduncID = o.OduncID " +
                         "LEFT JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "ORDER BY c.ceza_tarihi DESC";
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            model.addColumn("Ceza ID");
            model.addColumn("Üye");
            model.addColumn("Kitap");
            model.addColumn("Gecikme (Gün)");
            model.addColumn("Ceza (TL)");
            model.addColumn("Ceza Tarihi");
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), rs.getDouble(5), rs.getTimestamp(6)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Raporlar().setVisible(true));
    }
}
