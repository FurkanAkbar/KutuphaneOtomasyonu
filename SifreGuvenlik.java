import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Şifre güvenliği için SHA-256 hash fonksiyonları
 */
public class SifreGuvenlik {
    
    /**
     * SHA-256 ile şifreyi hashle
     * @param sifre Açık şifre
     * @return Base64 encoded hash
     */
    public static String sifreyiHashle(String sifre) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sifre.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algoritması bulunamadı", e);
        }
    }
    
    /**
     * Girilen şifreyi hash ile karşılaştır
     * @param sifreAcik Kullanıcı tarafından girilen açık şifre
     * @param sifreHash Veritabanından alınan hash
     * @return Eşleşiyorsa true
     */
    public static boolean sifreleriKarsilastir(String sifreAcik, String sifreHash) {
        String hashlenmisSifre = sifreyiHashle(sifreAcik);
        return hashlenmisSifre.equals(sifreHash);
    }
}
