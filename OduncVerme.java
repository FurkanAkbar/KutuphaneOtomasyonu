import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class OduncVerme extends JFrame {
    private JTable tabloUyeler, tabloEmanetler, tabloKitaplar;
    private DefaultTableModel modelUye, modelEmanet, modelKitap;
    private JTextField txtUyeAra, txtKitapAra;
    private JLabel lblMevcutAdet;
    private int girisYapanKullaniciID;
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = ""; 

    public OduncVerme() {
        this(1); // Default: Admin kullanıcısı
    }
    
    public OduncVerme(int kullaniciID) {
        this.girisYapanKullaniciID = kullaniciID;
        setTitle("Kütüphane Sistemi - Ödünç Verme");
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        // --- ARAMA PANELİ ---
        JPanel ustPanel = new JPanel(new GridLayout(2, 2, 15, 10));
        ustPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        txtUyeAra = new JTextField();
        txtUyeAra.setBorder(BorderFactory.createTitledBorder("Üye Bul (Ad/Soyad)"));
        txtKitapAra = new JTextField();
        txtKitapAra.setBorder(BorderFactory.createTitledBorder("Kitap Bul (Kitap Adı)"));
        
        lblMevcutAdet = new JLabel("Seçili Kitapın Mevcut Adedi: -");
        lblMevcutAdet.setFont(new Font("Arial", Font.BOLD, 12));
        lblMevcutAdet.setBorder(BorderFactory.createTitledBorder("Stok Bilgisi"));
        
        ustPanel.add(txtUyeAra);
        ustPanel.add(txtKitapAra);
        ustPanel.add(lblMevcutAdet);
        add(ustPanel, BorderLayout.NORTH);

        // --- MERKEZ PANEL ---
        JPanel merkezPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        
        modelUye = new DefaultTableModel(new String[]{"ID", "Ad Soyad"}, 0);
        tabloUyeler = new JTable(modelUye);
        merkezPanel.add(panelOlustur("1. ADIM: Üye Seçin", tabloUyeler, null));

        modelEmanet = new DefaultTableModel(new String[]{"ID", "Üye", "Kitap", "Ödünç Tarihi", "Son Tarih"}, 0);
        tabloEmanetler = new JTable(modelEmanet);
        merkezPanel.add(panelOlustur("2. ADIM: Seçili Üyenin Aktif Ödünçleri (Geri Teslim Bekleniyor)", tabloEmanetler, null));

        modelKitap = new DefaultTableModel(new String[]{"ID", "Kitap Adı", "Stok"}, 0);
        tabloKitaplar = new JTable(modelKitap);
        tabloKitaplar.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mevcutAdetGuncelle();
        });
        JButton btnOdunc = new JButton("ÖDÜNÇ VER");
        btnOdunc.setBackground(new Color(34, 139, 34));
        btnOdunc.setForeground(Color.WHITE);
        merkezPanel.add(panelOlustur("3. ADIM: Kitap Seçin", tabloKitaplar, btnOdunc));

        add(merkezPanel, BorderLayout.CENTER);

        // --- OLAYLAR ---
        tabloUyeler.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                listeleEmanetler();
                mevcutAdetGuncelle();
            }
        });

        txtUyeAra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { uyeleriGetir(); }
        });

        txtKitapAra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { kitaplariGetir(); }
        });

        btnOdunc.addActionListener(e -> oduncVer());

        uyeleriGetir();
        kitaplariGetir();
    }

    private JPanel panelOlustur(String baslik, JTable tablo, JButton btn) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createTitledBorder(baslik));
        p.add(new JScrollPane(tablo), BorderLayout.CENTER);
        if (btn != null) p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    private void uyeleriGetir() {
        modelUye.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT uye_id, CONCAT(ad, ' ', soyad) FROM uye WHERE ad LIKE ? OR soyad LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + txtUyeAra.getText() + "%");
            pstmt.setString(2, "%" + txtUyeAra.getText() + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) modelUye.addRow(new Object[]{rs.getInt(1), rs.getString(2)});
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void kitaplariGetir() {
        modelKitap.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String aramaText = "%" + txtKitapAra.getText() + "%";
            String sql = "SELECT kitap_id, kitap_adi, mevcut_adet FROM kitap WHERE mevcut_adet > 0 AND kitap_adi LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, aramaText);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) modelKitap.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3)});
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void listeleEmanetler() {
        modelEmanet.setRowCount(0);
        int row = tabloUyeler.getSelectedRow();
        if (row == -1) return;
        
        int uyeId = (int) modelUye.getValueAt(row, 0);
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT o.OduncID, CONCAT(u.ad, ' ', u.soyad), k.kitap_adi, o.OduncTarihi, o.SonTeslimTarihi " +
                         "FROM odunc o " +
                         "JOIN uye u ON o.UyeID = u.uye_id " +
                         "JOIN kitap k ON o.KitapID = k.kitap_id " +
                         "WHERE o.IadeTarihi IS NULL AND o.UyeID = ?";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, uyeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                modelEmanet.addRow(new Object[]{
                    rs.getInt(1), 
                    rs.getString(2), 
                    rs.getString(3),
                    rs.getDate(4),
                    rs.getDate(5)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void mevcutAdetGuncelle() {
        int row = tabloKitaplar.getSelectedRow();
        if (row == -1) {
            lblMevcutAdet.setText("Seçili Kitapın Mevcut Adedi: -");
            return;
        }
        int mevcut = (int) modelKitap.getValueAt(row, 2);
        lblMevcutAdet.setText("Seçili Kitapın Mevcut Adedi: " + mevcut);
    }

    private void oduncVer() {
        int uRow = tabloUyeler.getSelectedRow();
        int kRow = tabloKitaplar.getSelectedRow();
        
        if (uRow == -1 || kRow == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen üye ve kitap seçin!");
            return;
        }

        int uId = (int) modelUye.getValueAt(uRow, 0);
        int kId = (int) modelKitap.getValueAt(kRow, 0);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            // PROSEDÜR ÇAĞRISI
            CallableStatement cs = conn.prepareCall("{CALL sp_YeniOduncVer(?, ?, ?)}");
            cs.setInt(1, uId);
            cs.setInt(2, kId);
            cs.setInt(3, girisYapanKullaniciID);
            cs.execute();
            
            JOptionPane.showMessageDialog(this, "✓ Kitap başarıyla verildi!\n(15 günlük ödünç süresi)");
            kitaplariGetir(); 
            listeleEmanetler();
            mevcutAdetGuncelle();
        } catch (SQLException e) { 
            String hataMsg = e.getMessage().toLowerCase();
            if (hataMsg.contains("5 kitaptan fazla")) {
                JOptionPane.showMessageDialog(this, "❌ Hata: Üye eş zamanlı 5'ten fazla kitap ödünç alamaz!");
            } else if (hataMsg.contains("stokta yok") || hataMsg.contains("stok")) {
                JOptionPane.showMessageDialog(this, "❌ Hata: Bu kitap stokta yok!");
            } else {
                JOptionPane.showMessageDialog(this, "❌ Hata: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OduncVerme().setVisible(true));
    }
}