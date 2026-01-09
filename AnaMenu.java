import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class AnaMenu extends JFrame {
    private String kullaniciAdi;
    private int kullaniciID;
    private String rol;
    
    private final String URL = "jdbc:mysql://localhost:3306/universite_kutuphanesi";
    private final String USER = "root";
    private final String PASS = "";
    
    public AnaMenu(String kullaniciAdi, String rol) {
        this.kullaniciAdi = kullaniciAdi;
        this.rol = rol;
        
        // Veritabanından kullanıcı ID'sini al
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT kullanici_id FROM kullanici WHERE kullanici_adi = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, kullaniciAdi);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                this.kullaniciID = rs.getInt(1);
            }
        } catch (SQLException e) {
            this.kullaniciID = 1; // Default fallback
        }
        
        // Pencere Ayarları
        setTitle("Kütüphane Otomasyonu - Ana Menü");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(60, 63, 65)); // Koyu gri arka plan
        setLayout(new BorderLayout());

        // --- ÜST PANEL: Karşılama Metni ---
        JLabel lblHosgeldin = new JLabel("Hoşgeldiniz, " + kullaniciAdi + " (" + rol + ")", SwingConstants.CENTER);
        lblHosgeldin.setForeground(new Color(88, 101, 242)); // Mavi tonlu yazı
        lblHosgeldin.setFont(new Font("Arial", Font.BOLD, 22));
        lblHosgeldin.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(lblHosgeldin, BorderLayout.NORTH);

        // --- ORTA PANEL: 3x3 Buton Izgarası ---
        JPanel pnlIzgara = new JPanel(new GridLayout(3, 3, 25, 25));
        pnlIzgara.setOpaque(false);
        pnlIzgara.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // Butonlar
        JButton btnUye = createMenuButton("Üye Yönetimi");
        JButton btnKitap = createMenuButton("Kitap Yönetimi");
        JButton btnKategori = createMenuButton("Kategori Yönetimi");
        JButton btnOdunc = createMenuButton("Ödünç İşlemleri");
        JButton btnTeslim = createMenuButton("Kitap Teslim Alma");
        JButton btnCezalar = createMenuButton("Cezalar");
        JButton btnRaporlar = createMenuButton("Dinamik Sorgu");
        JButton btnRaporlarStatik = createMenuButton("Raporlar");
        JButton btnKayitGoster = createMenuButton("İşlem Günlüğü");

        // --- BUTON OLAYLARI (Action Listeners) ---

        // 1. Üye Yönetimi
        btnUye.addActionListener(e -> {
            new UyeYonetimi().setVisible(true);
        });

        // 2. Kitap Yönetimi
        btnKitap.addActionListener(e -> {
            new KitapYonetimi().setVisible(true);
        });

        // 3. Kategori Yönetimi (BONUS)
        btnKategori.addActionListener(e -> {
            new KategoriYonetimi().setVisible(true);
        });

        // 4. Ödünç İşlemleri
        btnOdunc.addActionListener(e -> {
            new OduncVerme(this.kullaniciID).setVisible(true);
        });

        // 5. Kitap Teslim Alma
        btnTeslim.addActionListener(e -> {
            new KitapTeslim().setVisible(true);
        });

        // 6. Cezalar
        btnCezalar.addActionListener(e -> {
            new CezaYonetimi().setVisible(true);
        });

        // 7. Dinamik Sorgu
        btnRaporlar.addActionListener(e -> {
            new DinamikSorgu().setVisible(true);
        });

        // 8. Raporlar
        btnRaporlarStatik.addActionListener(e -> {
            new Raporlar().setVisible(true);
        });

        // 9. İşlem Günlüğü (Log gösterimi)
        btnKayitGoster.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, 
                "İşlem Günlüğü\n\nTüm işlemler veritabanının LOG_ISLEM tablosunda tutulmaktadır.\n" +
                "Üyenin ödünç verme, teslim alma, ceza oluşturma gibi işlemleri otomatik olarak kaydedilir.",
                "İşlem Günlüğü", JOptionPane.INFORMATION_MESSAGE);
        });
        
        pnlIzgara.add(btnUye); 
        pnlIzgara.add(btnKitap); 
        pnlIzgara.add(btnKategori);
        pnlIzgara.add(btnOdunc);
        pnlIzgara.add(btnTeslim);
        pnlIzgara.add(btnCezalar); 
        pnlIzgara.add(btnRaporlar);
        pnlIzgara.add(btnRaporlarStatik);
        pnlIzgara.add(btnKayitGoster);
        add(pnlIzgara, BorderLayout.CENTER);

        // --- ALT PANEL: Güvenli Çıkış ---
        JPanel pnlAlt = new JPanel();
        pnlAlt.setOpaque(false);
        pnlAlt.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        JButton btnCikis = new JButton("Güvenli Çıkış");
        btnCikis.setBackground(new Color(231, 76, 60)); // Kırmızı buton
        btnCikis.setForeground(Color.WHITE);
        btnCikis.setFont(new Font("Arial", Font.BOLD, 16));
        btnCikis.setPreferredSize(new Dimension(200, 45));
        btnCikis.setFocusPainted(false);
        btnCikis.addActionListener(e -> System.exit(0));
        
        pnlAlt.add(btnCikis);
        add(pnlAlt, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    // Görseldeki gibi koyu, karemsi butonlar oluşturur
    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(80, 80, 80));
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setFont(new Font("Arial", Font.PLAIN, 16));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        return btn;
    }
}