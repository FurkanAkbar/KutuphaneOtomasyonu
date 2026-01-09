import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Kategori Yönetimi Ekranı
 * Kitap kategorilerini (Roman, Bilgisayar, Bilim vb.) ekle, güncelle, sil işlemleri yapabilir.
 */
public class KategoriYonetimi extends JFrame {
    private JTable tablo = new JTable();
    private DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Kategori Adı"}, 0);
    
    private JTextField txtKategoriAdi = new JTextField(25);
    private JButton btnEkle = new JButton("Ekle");
    private JButton btnGuncelle = new JButton("Güncelle");
    private JButton btnSil = new JButton("Sil");
    private JButton btnTemizle = new JButton("Temizle");
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = "";

    public KategoriYonetimi() {
        setTitle("Kategori Yönetimi");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- SOL PANEL (GİRİŞ FORMU) ---
        JPanel solPanel = new JPanel();
        solPanel.setLayout(new BoxLayout(solPanel, BoxLayout.Y_AXIS));
        solPanel.setPreferredSize(new Dimension(280, 0));
        solPanel.setBorder(BorderFactory.createTitledBorder("Kategori İşlemleri"));
        solPanel.setBackground(new Color(240, 240, 240));

        JLabel lblKategoriAdi = new JLabel("Kategori Adı:");
        lblKategoriAdi.setFont(new Font("Arial", Font.BOLD, 11));
        solPanel.add(lblKategoriAdi);
        solPanel.add(Box.createVerticalStrut(5));
        solPanel.add(txtKategoriAdi);
        solPanel.add(Box.createVerticalStrut(20));

        // Buton Paneli
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);

        btnEkle.setBackground(new Color(46, 204, 113));
        btnEkle.setForeground(Color.WHITE);
        btnEkle.setFont(new Font("Arial", Font.BOLD, 11));

        btnGuncelle.setBackground(new Color(52, 152, 219));
        btnGuncelle.setForeground(Color.WHITE);
        btnGuncelle.setFont(new Font("Arial", Font.BOLD, 11));

        btnSil.setBackground(new Color(231, 76, 60));
        btnSil.setForeground(Color.WHITE);
        btnSil.setFont(new Font("Arial", Font.BOLD, 11));

        btnTemizle.setBackground(new Color(149, 165, 166));
        btnTemizle.setForeground(Color.WHITE);
        btnTemizle.setFont(new Font("Arial", Font.BOLD, 11));

        btnPanel.add(btnEkle);
        btnPanel.add(btnGuncelle);
        btnPanel.add(btnSil);
        btnPanel.add(btnTemizle);

        solPanel.add(btnPanel);
        solPanel.add(Box.createVerticalGlue());

        add(solPanel, BorderLayout.WEST);

        // --- SAĞ PANEL (TABLO) ---
        tablo.setModel(model);
        tablo.setRowHeight(25);
        tablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablo.getSelectedRow() >= 0) {
                int row = tablo.getSelectedRow();
                txtKategoriAdi.setText(model.getValueAt(row, 1).toString());
            }
        });
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // --- BUTON OLAYLARI ---
        btnEkle.addActionListener(e -> kategoriEkle());
        btnGuncelle.addActionListener(e -> kategoriGuncelle());
        btnSil.addActionListener(e -> kategoriSil());
        btnTemizle.addActionListener(e -> {
            txtKategoriAdi.setText("");
            tablo.clearSelection();
        });

        kategorileriGetir();
        setVisible(true);
    }

    private void kategorileriGetir() {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT kategori_id, kategori_adi FROM kategori ORDER BY kategori_adi ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("kategori_id"),
                    rs.getString("kategori_adi")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
        }
    }

    private void kategoriEkle() {
        String kategoriAdi = txtKategoriAdi.getText().trim();
        
        if (kategoriAdi.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kategori adı boş olamaz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "INSERT INTO kategori (kategori_adi) VALUES (?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, kategoriAdi);
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "✓ Kategori başarıyla eklendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            txtKategoriAdi.setText("");
            kategorileriGetir();
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "Bu kategori zaten var!", "Hata", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void kategoriGuncelle() {
        int row = tablo.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen güncellemek için bir kategori seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int kategoriId = (int) model.getValueAt(row, 0);
        String yeniAdi = txtKategoriAdi.getText().trim();
        
        if (yeniAdi.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kategori adı boş olamaz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "UPDATE kategori SET kategori_adi = ? WHERE kategori_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, yeniAdi);
            pstmt.setInt(2, kategoriId);
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "✓ Kategori başarıyla güncellendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
            txtKategoriAdi.setText("");
            kategorileriGetir();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void kategoriSil() {
        int row = tablo.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Lütfen silmek için bir kategori seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int kategoriId = (int) model.getValueAt(row, 0);
        String kategoriAdi = model.getValueAt(row, 1).toString();
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "\"" + kategoriAdi + "\" kategorisini silmek istediğinize emin misiniz?\n(Bu kategoriyle ilişkili kitaplar önemli olabilir!)", 
            "Silme Onayı", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "DELETE FROM kategori WHERE kategori_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, kategoriId);
                pstmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "✓ Kategori başarıyla silindi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                txtKategoriAdi.setText("");
                kategorileriGetir();
            } catch (SQLException ex) {
                if (ex.getMessage().contains("foreign key")) {
                    JOptionPane.showMessageDialog(this, "Bu kategoriyle ilişkili kitaplar var! Önce onları silin.", "Hata", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KategoriYonetimi());
    }
}
