import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class UyeYonetimi extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Ad", "Soyad", "Telefon", "Email", "Borç"}, 0);
    
    private JTextField txtAd = new JTextField();
    private JTextField txtSoyad = new JTextField();
    private JTextField txtTel = new JTextField();
    private JTextField txtEmail = new JTextField();
    
    // Filtreleme alanları
    private JTextField txtAraAd = new JTextField();
    private JTextField txtAraSoyad = new JTextField();
    private JTextField txtAraEmail = new JTextField();

    public UyeYonetimi() {
        setTitle("Üye Yönetim Sistemi");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- SOL PANEL (GİRİŞ FORMU) ---
        JPanel pnlSol = new JPanel();
        pnlSol.setLayout(null);
        pnlSol.setPreferredSize(new Dimension(350, 0));
        pnlSol.setBackground(new Color(45, 52, 54));

        JLabel lblAd = new JLabel("Ad:"); lblAd.setForeground(Color.WHITE);
        lblAd.setBounds(20, 30, 80, 25); pnlSol.add(lblAd);
        txtAd.setBounds(110, 30, 200, 25); pnlSol.add(txtAd);

        JLabel lblSoyad = new JLabel("Soyad:"); lblSoyad.setForeground(Color.WHITE);
        lblSoyad.setBounds(20, 70, 80, 25); pnlSol.add(lblSoyad);
        txtSoyad.setBounds(110, 70, 200, 25); pnlSol.add(txtSoyad);

        JLabel lblTel = new JLabel("Telefon:"); lblTel.setForeground(Color.WHITE);
        lblTel.setBounds(20, 110, 80, 25); pnlSol.add(lblTel);
        txtTel.setBounds(110, 110, 200, 25); pnlSol.add(txtTel);

        JLabel lblEmail = new JLabel("E-mail:"); lblEmail.setForeground(Color.WHITE);
        lblEmail.setBounds(20, 150, 80, 25); pnlSol.add(lblEmail);
        txtEmail.setBounds(110, 150, 200, 25); pnlSol.add(txtEmail);

        JButton btnEkle = new JButton("Ekle");
        btnEkle.setBackground(new Color(46, 204, 113)); btnEkle.setForeground(Color.WHITE);
        btnEkle.setBounds(20, 210, 90, 35); pnlSol.add(btnEkle);

        JButton btnGuncelle = new JButton("Güncelle");
        btnGuncelle.setBackground(new Color(52, 152, 219)); btnGuncelle.setForeground(Color.WHITE);
        btnGuncelle.setBounds(120, 210, 90, 35); pnlSol.add(btnGuncelle);

        JButton btnSil = new JButton("Sil");
        btnSil.setBackground(new Color(231, 76, 60)); btnSil.setForeground(Color.WHITE);
        btnSil.setBounds(220, 210, 90, 35); pnlSol.add(btnSil);

        JButton btnTemizle = new JButton("Temizle");
        btnTemizle.setBounds(20, 255, 190, 35); pnlSol.add(btnTemizle);

        // --- FILTRELEME BÖLÜMÜ ---
        JLabel lblFiltrele = new JLabel("FILTRELEME");
        lblFiltrele.setForeground(Color.WHITE);
        lblFiltrele.setFont(new Font("Arial", Font.BOLD, 12));
        lblFiltrele.setBounds(20, 310, 100, 25);
        pnlSol.add(lblFiltrele);

        JLabel lblAraAd = new JLabel("Ad:");
        lblAraAd.setForeground(Color.WHITE);
        lblAraAd.setBounds(20, 340, 80, 25);
        pnlSol.add(lblAraAd);
        txtAraAd.setBounds(110, 340, 200, 25);
        pnlSol.add(txtAraAd);

        JLabel lblAraSoyad = new JLabel("Soyad:");
        lblAraSoyad.setForeground(Color.WHITE);
        lblAraSoyad.setBounds(20, 375, 80, 25);
        pnlSol.add(lblAraSoyad);
        txtAraSoyad.setBounds(110, 375, 200, 25);
        pnlSol.add(txtAraSoyad);

        JLabel lblAraEmail = new JLabel("Email:");
        lblAraEmail.setForeground(Color.WHITE);
        lblAraEmail.setBounds(20, 410, 80, 25);
        pnlSol.add(lblAraEmail);
        txtAraEmail.setBounds(110, 410, 200, 25);
        pnlSol.add(txtAraEmail);

        JButton btnFiltrele = new JButton("Filtrele");
        btnFiltrele.setBackground(new Color(52, 152, 219));
        btnFiltrele.setForeground(Color.WHITE);
        btnFiltrele.setBounds(20, 445, 90, 35);
        pnlSol.add(btnFiltrele);

        JButton btnTumuGoster = new JButton("Tümünü Göster");
        btnTumuGoster.setBackground(new Color(149, 165, 166));
        btnTumuGoster.setForeground(Color.WHITE);
        btnTumuGoster.setBounds(120, 445, 190, 35);
        pnlSol.add(btnTumuGoster);

        add(pnlSol, BorderLayout.WEST);

        // --- SAĞ PANEL (TABLO) ---
        tablo.setModel(model);
        tablo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int satir = tablo.getSelectedRow();
                if (satir >= 0) {
                    txtAd.setText(model.getValueAt(satir, 1).toString());
                    txtSoyad.setText(model.getValueAt(satir, 2).toString());
                    txtTel.setText(model.getValueAt(satir, 3).toString());
                    txtEmail.setText(model.getValueAt(satir, 4).toString());
                }
            }
        });
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // Buton Olayları
        btnEkle.addActionListener(e -> uyeEkle());
        btnGuncelle.addActionListener(e -> uyeGuncelle());
        btnSil.addActionListener(e -> uyeSil());
        btnTemizle.addActionListener(e -> { txtAd.setText(""); txtSoyad.setText(""); txtTel.setText(""); txtEmail.setText(""); });
        btnFiltrele.addActionListener(e -> uyeFiltrele());
        btnTumuGoster.addActionListener(e -> verileriGetir());

        verileriGetir();
        this.revalidate();
        this.repaint();
        setLocationRelativeTo(null);
    }

    private void verileriGetir() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM uye");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("uye_id"),
                    rs.getString("ad"),
                    rs.getString("soyad"),
                    rs.getString("telefon"),
                    rs.getString("email"),
                    rs.getDouble("toplam_borc") + " TL"
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void uyeEkle() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            // Sütun ismi 'toplam_borc' olarak düzeltildi
            String sql = "INSERT INTO uye (ad, soyad, telefon, email, toplam_borc) VALUES (?, ?, ?, ?, 0.0)";
            PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, txtAd.getText());
            ps.setString(2, txtSoyad.getText());
            ps.setString(3, txtTel.getText());
            ps.setString(4, txtEmail.getText());
            ps.executeUpdate();
            
            // Log kaydı yaz
            java.sql.ResultSet generatedKeys = ps.getGeneratedKeys();
            int uyeId = 0;
            if (generatedKeys.next()) {
                uyeId = generatedKeys.getInt(1);
            }
            String sqlLog = "INSERT INTO log_islem (islem_tipi, uye_id, aciklama) VALUES (?, ?, ?)";
            PreparedStatement psLog = conn.prepareStatement(sqlLog);
            psLog.setString(1, "Üye Ekleme");
            psLog.setInt(2, uyeId);
            psLog.setString(3, "Ad: " + txtAd.getText() + " | Soyad: " + txtSoyad.getText() + " | Email: " + txtEmail.getText());
            psLog.executeUpdate();
            
            verileriGetir();
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage()); }
    }

    private void uyeGuncelle() {
        int satir = tablo.getSelectedRow();
        if (satir == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen güncellemek için bir üye seçin!");
            return;
        }
        
        if (txtAd.getText().isEmpty() || txtSoyad.getText().isEmpty() || 
            txtTel.getText().isEmpty() || txtEmail.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen tüm alanları doldurun!");
            return;
        }
        
        int id = (int) model.getValueAt(satir, 0);
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            String sql = "UPDATE uye SET ad = ?, soyad = ?, telefon = ?, email = ? WHERE uye_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, txtAd.getText());
            ps.setString(2, txtSoyad.getText());
            ps.setString(3, txtTel.getText());
            ps.setString(4, txtEmail.getText());
            ps.setInt(5, id);
            ps.executeUpdate();
            
            // Log kaydı yaz
            String sqlLog = "INSERT INTO log_islem (islem_tipi, uye_id, aciklama) VALUES (?, ?, ?)";
            PreparedStatement psLog = conn.prepareStatement(sqlLog);
            psLog.setString(1, "Üye Güncelleme");
            psLog.setInt(2, id);
            psLog.setString(3, "Güncellenmiş Ad: " + txtAd.getText() + " | Soyad: " + txtSoyad.getText() + " | Email: " + txtEmail.getText());
            psLog.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Üye başarıyla güncellendi!");
            verileriGetir();
            txtAd.setText(""); txtSoyad.setText(""); txtTel.setText(""); txtEmail.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
        }
    }

    private void uyeSil() {
        int satir = tablo.getSelectedRow();
        if (satir == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen silmek için bir üye seçin!");
            return;
        }
        
        int id = (int) model.getValueAt(satir, 0);
        int onay = JOptionPane.showConfirmDialog(this, "Bu üyeyi silmek istediğinize emin misiniz?", 
                                                  "Silme Onayı", JOptionPane.YES_NO_OPTION);
        
        if (onay == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
                String uyeAdi = model.getValueAt(satir, 1).toString();
                String uyeSoyadi = model.getValueAt(satir, 2).toString();
                
                // Trigger tarafından aktif ödünç ve borç kontrol edilecek
                String sql = "DELETE FROM uye WHERE uye_id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                
                // Log kaydı yaz
                String sqlLog = "INSERT INTO log_islem (islem_tipi, uye_id, aciklama) VALUES (?, ?, ?)";
                PreparedStatement psLog = conn.prepareStatement(sqlLog);
                psLog.setString(1, "Üye Silme");
                psLog.setInt(2, id);
                psLog.setString(3, "Silinen Üye: " + uyeAdi + " " + uyeSoyadi);
                psLog.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Üye başarıyla silindi!");
                verileriGetir();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
            }
        }
    }

    private void uyeFiltrele() {
        String ad = txtAraAd.getText().trim();
        String soyad = txtAraSoyad.getText().trim();
        String email = txtAraEmail.getText().trim();

        // En az bir filtreleme alanı dolu mu kontrol et
        if (ad.isEmpty() && soyad.isEmpty() && email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen en az bir filtreleme kriteri girin!");
            return;
        }

        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            String sql = "SELECT * FROM uye WHERE 1=1";
            
            if (!ad.isEmpty()) {
                sql += " AND ad LIKE ?";
            }
            if (!soyad.isEmpty()) {
                sql += " AND soyad LIKE ?";
            }
            if (!email.isEmpty()) {
                sql += " AND email LIKE ?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            int paramIndex = 1;
            
            if (!ad.isEmpty()) {
                ps.setString(paramIndex++, "%" + ad + "%");
            }
            if (!soyad.isEmpty()) {
                ps.setString(paramIndex++, "%" + soyad + "%");
            }
            if (!email.isEmpty()) {
                ps.setString(paramIndex++, "%" + email + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("uye_id"),
                    rs.getString("ad"),
                    rs.getString("soyad"),
                    rs.getString("telefon"),
                    rs.getString("email"),
                    rs.getDouble("toplam_borc") + " TL"
                });
            }
            
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Arama kriterine uygun üye bulunamadı!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UyeYonetimi().setVisible(true));
    }
}