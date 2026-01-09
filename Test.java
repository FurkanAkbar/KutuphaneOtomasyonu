import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Test {
    public static void main(String[] args) {
        // XAMPP varsayılan bilgileri
        String url = "jdbc:mysql://localhost:3306/universite_kutuphanesi"; 
        String user = "root"; 
        String password = ""; 

        try {
            // Sürücüyü yükle (MySQL için)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Bağlantıyı kur
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Bağlantı Başarılı!");
            
            // Bağlantıyı kapat
            conn.close();
            
        } catch (ClassNotFoundException e) {
            System.out.println("Hata: MySQL Sürücüsü (JAR dosyası) projeye eklenmemiş!");
        } catch (SQLException e) {
            System.out.println("Veritabanı Hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Beklenmedik Hata: " + e.getMessage());
        }
    }
}