import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class GirisEkrani extends JFrame {
    private JTextField txtKullanici = new JTextField();
    private JPasswordField txtSifre = new JPasswordField();
    private JButton btnGiris = new JButton("SİSTEME GİRİŞ YAP");

    public GirisEkrani() {
        setTitle("Kütüphane Otomasyonu | Giriş");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(new Color(245, 246, 250));

        txtKullanici.setBorder(BorderFactory.createTitledBorder("Kullanıcı Adı"));
        txtSifre.setBorder(BorderFactory.createTitledBorder("Şifre"));

        btnGiris.setBackground(new Color(41, 128, 185));
        btnGiris.setForeground(Color.WHITE);
        btnGiris.setFont(new Font("Segoe UI", Font.BOLD, 14));

        mainPanel.add(txtKullanici);
        mainPanel.add(txtSifre);
        mainPanel.add(btnGiris);

        add(mainPanel, BorderLayout.CENTER);
        btnGiris.addActionListener(e -> girisYap());
        setLocationRelativeTo(null);
    }

    private void girisYap() {
        String kadi = txtKullanici.getText();
        String sifre = new String(txtSifre.getPassword());

        if (kadi.isEmpty() || sifre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kullanıcı adı ve şifre boş olamaz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/universite_kutuphanesi", "root", "")) {
            String sql = "SELECT * FROM kullanici WHERE kullanici_adi=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, kadi);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String databaseSifre = rs.getString("sifre");
                
                // Hash şifreleme ile kontrol (opsiyonel - düz metin de çalışır)
                boolean sifreOk = false;
                
                // Eğer veritabanında hash varsa, hash ile kontrol et
                if (databaseSifre.length() > 20) { // Hash daha uzundur
                    sifreOk = SifreGuvenlik.sifreleriKarsilastir(sifre, databaseSifre);
                } else {
                    // Düz metin şifre karşılaştırması (eski veriler için)
                    sifreOk = sifre.equals(databaseSifre);
                }
                
                if (sifreOk) {
                    new AnaMenu(kadi, rs.getString("rol")).setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Hatalı Kullanıcı Adı veya Şifre!", "Hata", JOptionPane.ERROR_MESSAGE);
                    txtSifre.setText("");
                }
            } else {
                JOptionPane.showMessageDialog(this, "❌ Hatalı Kullanıcı Adı veya Şifre!", "Hata", JOptionPane.ERROR_MESSAGE);
                txtSifre.setText("");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Veritabanı Hatası: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GirisEkrani().setVisible(true));
    }
}