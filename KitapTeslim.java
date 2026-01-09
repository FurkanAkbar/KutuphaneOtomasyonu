import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class KitapTeslim extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Üye", "Kitap", "Ödünç Tarihi", "Son Teslim", "Gün"}, 0);
    private JButton btnTeslim = new JButton("KİTABI TESLİM AL (İADE)");
    private JTextField txtUyeAra, txtKitapAra, txtTarih;
    private JLabel lblDetaylar = new JLabel("");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    public KitapTeslim() {
        setTitle("Kitap İade Sistemi");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- ARAMA PANELİ ---
        JPanel aramaPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        aramaPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        txtUyeAra = new JTextField();
        txtUyeAra.setBorder(BorderFactory.createTitledBorder("Üye Ada Göre Filtrele"));
        txtKitapAra = new JTextField();
        txtKitapAra.setBorder(BorderFactory.createTitledBorder("Kitap Adına Göre Filtrele"));
        txtTarih = new JTextField();
        txtTarih.setBorder(BorderFactory.createTitledBorder("Son Teslim Tarihi Filtrele (YYYY-MM-DD)"));
        
        aramaPanel.add(txtUyeAra);
        aramaPanel.add(txtKitapAra);
        aramaPanel.add(txtTarih);
        add(aramaPanel, BorderLayout.NORTH);

        // --- TABLO PANELİ ---
        tablo.setModel(model);
        tablo.setRowHeight(25);
        tablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                detaylariGoster();
                btnTeslim.setEnabled(true);
            }
        });
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // --- DETAY VE BUTON PANELİ ---
        JPanel altPanel = new JPanel(new BorderLayout(10, 10));
        altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        lblDetaylar.setFont(new Font("Arial", Font.PLAIN, 12));
        lblDetaylar.setText("Teslim almak için yukarıdan bir kayıt seçin");
        lblDetaylar.setBorder(BorderFactory.createTitledBorder("Seçili Kayıt Detayları"));
        
        btnTeslim.setBackground(new Color(41, 128, 185));
        btnTeslim.setForeground(Color.WHITE);
        btnTeslim.setFont(new Font("Arial", Font.BOLD, 14));
        btnTeslim.setPreferredSize(new Dimension(0, 50));
        btnTeslim.setEnabled(false); // Başlangıçta disabled

        altPanel.add(lblDetaylar, BorderLayout.CENTER);
        altPanel.add(btnTeslim, BorderLayout.SOUTH);
        add(altPanel, BorderLayout.SOUTH);

        // --- ETKİLEŞİMLER ---
        btnTeslim.addActionListener(e -> teslimAl());
        txtUyeAra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { listeleAktifOdunc(); }
        });
        txtKitapAra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { listeleAktifOdunc(); }
        });
        txtTarih.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { listeleAktifOdunc(); }
        });
        
        listeleAktifOdunc();
    }

    private void listeleAktifOdunc() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            String sql = "SELECT OduncID, CONCAT(u.ad, ' ', u.soyad) as uye_adi, k.kitap_adi, OduncTarihi, SonTeslimTarihi, " +
                         "DATEDIFF(CURDATE(), SonTeslimTarihi) as gun_fark " +
                         "FROM odunc " +
                         "INNER JOIN uye u ON UyeID = u.uye_id " +
                         "INNER JOIN kitap k ON KitapID = k.kitap_id " +
                         "WHERE IadeTarihi IS NULL ";
            
            String uyeFiltre = txtUyeAra.getText().trim();
            String kitapFiltre = txtKitapAra.getText().trim();
            String tarihFiltre = txtTarih.getText().trim();
            
            if (!uyeFiltre.isEmpty()) {
                sql += " AND (u.ad LIKE ? OR u.soyad LIKE ?)";
            }
            if (!kitapFiltre.isEmpty()) {
                sql += " AND k.kitap_adi LIKE ?";
            }
            if (!tarihFiltre.isEmpty()) {
                sql += " AND DATE(SonTeslimTarihi) = ?";
            }
            sql += " ORDER BY SonTeslimTarihi ASC";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            int paramIndex = 1;
            
            if (!uyeFiltre.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + uyeFiltre + "%");
                pstmt.setString(paramIndex++, "%" + uyeFiltre + "%");
            }
            if (!kitapFiltre.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + kitapFiltre + "%");
            }
            if (!tarihFiltre.isEmpty()) {
                pstmt.setString(paramIndex++, tarihFiltre);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int gunFark = rs.getInt(6);
                String gunText = gunFark > 0 ? ("+" + gunFark + " (GECİKME)") : (gunFark + " gün");
                
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDate(4),
                    rs.getDate(5),
                    gunText
                });
            }
        } catch (SQLException ex) { 
            ex.printStackTrace();
        }
    }
    
    private void detaylariGoster() {
        int row = tablo.getSelectedRow();
        if (row == -1) {
            lblDetaylar.setText("Teslim almak için yukarıdan bir kayıt seçin");
            return;
        }
        
        String uyeAdi = (String) model.getValueAt(row, 1);
        String kitapAdi = (String) model.getValueAt(row, 2);
        java.sql.Date oduncTarihi = (java.sql.Date) model.getValueAt(row, 3);
        java.sql.Date sonTarihi = (java.sql.Date) model.getValueAt(row, 4);
        String gunBilgisi = (String) model.getValueAt(row, 5);
        
        String detaylar = "<html>" +
            "<b>Üye Adı:</b> " + uyeAdi + "<br/>" +
            "<b>Kitap Adı:</b> " + kitapAdi + "<br/>" +
            "<b>Ödünç Tarihi:</b> " + sdf.format(oduncTarihi) + "<br/>" +
            "<b>Son Teslim Tarihi:</b> " + sdf.format(sonTarihi) + "<br/>" +
            "<b>Durum:</b> <span style='color:red'>" + gunBilgisi + "</span>" +
            "</html>";
        
        lblDetaylar.setText(detaylar);
    }

    private void teslimAl() {
        int row = tablo.getSelectedRow();
        if (row == -1) { 
            JOptionPane.showMessageDialog(this, "Lütfen iade edilecek kitabı seçin!"); 
            return; 
        }
        
        int oduncId = (int) model.getValueAt(row, 0);
        String uyeAdi = (String) model.getValueAt(row, 1);
        String kitapAdi = (String) model.getValueAt(row, 2);
        
        // Ceza kontrolü: Ödenmemiş ceza varsa teslim alma işlemini engelle
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            String sqlCezaKontrol = "SELECT COUNT(*) FROM ceza WHERE OduncID = ?";
            PreparedStatement pstmtCeza = conn.prepareStatement(sqlCezaKontrol);
            pstmtCeza.setInt(1, oduncId);
            ResultSet rsCeza = pstmtCeza.executeQuery();
            
            if (rsCeza.next() && rsCeza.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, 
                    "❌ ÖDENMEMIŞ CEZA VAR!\n\n" +
                    "Bu kitap için ödenmemiş ceza bulunmaktadır.\n" +
                    "Teslim almadan önce cezayı ödemelisiniz.\n\n" +
                    "Lütfen Ceza Yönetim Sistemi'nde cezayı ödeyiniz.",
                    "Ceza Engeli", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            conn.setAutoCommit(false);
            
            // Ödünç bilgilerini al
            String sqlBilgi = "SELECT SonTeslimTarihi, UyeID, KitapID FROM odunc WHERE OduncID = ?";
            PreparedStatement pstmtBilgi = conn.prepareStatement(sqlBilgi);
            pstmtBilgi.setInt(1, oduncId);
            ResultSet rsBilgi = pstmtBilgi.executeQuery();
            
            java.sql.Date sonTeslimTarihi = null;
            int uyeId = 0;
            int kitapId = 0;
            if (rsBilgi.next()) {
                sonTeslimTarihi = rsBilgi.getDate(1);
                uyeId = rsBilgi.getInt(2);
                kitapId = rsBilgi.getInt(3);
            }
            
            // İade tarihini güncelle
            String sqlUpdate = "UPDATE odunc SET IadeTarihi = CURDATE() WHERE OduncID = ?";
            PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate);
            pstmtUpdate.setInt(1, oduncId);
            pstmtUpdate.executeUpdate();
            
            // Kitap stoğunu artır
            String sqlStok = "UPDATE kitap SET mevcut_adet = mevcut_adet + 1 WHERE kitap_id = ?";
            PreparedStatement pstmtStok = conn.prepareStatement(sqlStok);
            pstmtStok.setInt(1, kitapId);
            pstmtStok.executeUpdate();
            
            // Gecikme varsa ceza oluştur
            long gecikmeGun = 0;
            double cezaTutari = 0;
            if (sonTeslimTarihi != null) {
                gecikmeGun = calculateDaysDifference(sonTeslimTarihi);
                if (gecikmeGun > 0) {
                    cezaTutari = gecikmeGun * 5.0;
                    
                    // Ceza kaydı ekle
                    String sqlCeza = "INSERT INTO ceza (UyeID, OduncID, gecikme_gun, ceza_tutari) VALUES (?, ?, ?, ?)";
                    PreparedStatement pstmtCeza = conn.prepareStatement(sqlCeza);
                    pstmtCeza.setInt(1, uyeId);
                    pstmtCeza.setInt(2, oduncId);
                    pstmtCeza.setInt(3, (int)gecikmeGun);
                    pstmtCeza.setDouble(4, cezaTutari);
                    pstmtCeza.executeUpdate();
                    
                    // Üyenin borcunu artır
                    String sqlBorc = "UPDATE uye SET toplam_borc = toplam_borc + ? WHERE uye_id = ?";
                    PreparedStatement pstmtBorc = conn.prepareStatement(sqlBorc);
                    pstmtBorc.setDouble(1, cezaTutari);
                    pstmtBorc.setInt(2, uyeId);
                    pstmtBorc.executeUpdate();
                }
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            
            // Gecikme ve ceza bilgisini göster
            StringBuilder mesaj = new StringBuilder();
            mesaj.append("✓ İADE BAŞARIYLA ALINDI!\n");
            mesaj.append("─────────────────────\n");
            mesaj.append("Üye: ").append(uyeAdi).append("\n");
            mesaj.append("Kitap: ").append(kitapAdi).append("\n");
            mesaj.append("Kitap stokuna geri eklendi.\n");
            
            if (gecikmeGun > 0) {
                mesaj.append("\n⚠️ GECİKME UYARISI:\n");
                mesaj.append("─────────────────────\n");
                mesaj.append("Gecikme: ").append(gecikmeGun).append(" gün\n");
                mesaj.append("Ceza: ").append(cezaTutari).append(" TL\n");
                mesaj.append("(Üyenin borcuna eklendi)");
            } else {
                mesaj.append("\n✓ Zamanında teslim edildi!");
            }
            
            JOptionPane.showMessageDialog(this, mesaj.toString(), "İade İşlemi", JOptionPane.INFORMATION_MESSAGE);
            listeleAktifOdunc(); // Listeyi tazele
            lblDetaylar.setText("Teslim almak için yukarıdan bir kayıt seçin");
            
        } catch (SQLException ex) { 
            JOptionPane.showMessageDialog(this, "❌ Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE); 
        }
    }
    
    private long calculateDaysDifference(java.sql.Date sonTeslimTarihi) {
        java.util.Date today = new java.util.Date();
        java.util.Date tarih = new java.util.Date(sonTeslimTarihi.getTime());
        long diffTime = today.getTime() - tarih.getTime();
        return diffTime / (1000 * 60 * 60 * 24);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KitapTeslim().setVisible(true));
    }
}