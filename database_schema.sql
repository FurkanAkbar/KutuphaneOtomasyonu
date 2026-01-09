-- ============================================
-- ÜNIVERSITE KÜTÜPHANESI YÖNETIM SİSTEMİ
-- Veritabanı Şeması (MySQL)
-- ============================================

-- Veritabanı oluştur
CREATE DATABASE IF NOT EXISTS universite_kutuphanesi;
USE universite_kutuphanesi;

-- ============================================
-- 1. KULLANICI TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS kullanici (
    kullanici_id INT PRIMARY KEY AUTO_INCREMENT,
    kullanici_adi VARCHAR(50) UNIQUE NOT NULL,
    sifre VARCHAR(100) NOT NULL,
    rol ENUM('Admin', 'Görevli') NOT NULL,
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 2. UYE TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS uye (
    uye_id INT PRIMARY KEY AUTO_INCREMENT,
    ad VARCHAR(50) NOT NULL,
    soyad VARCHAR(50) NOT NULL,
    telefon VARCHAR(15),
    email VARCHAR(100),
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    toplam_borc DECIMAL(10, 2) DEFAULT 0.00
);

-- ============================================
-- 3. KATEGORİ TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS kategori (
    kategori_id INT PRIMARY KEY AUTO_INCREMENT,
    kategori_adi VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO kategori (kategori_adi) VALUES 
('Roman'), 
('Bilgisayar'), 
('Bilim');

-- ============================================
-- 4. KİTAP TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS kitap (
    kitap_id INT PRIMARY KEY AUTO_INCREMENT,
    kitap_adi VARCHAR(150) NOT NULL,
    yazar VARCHAR(100),
    kategori_id INT,
    yayinevi VARCHAR(100),
    basim_yili INT,
    toplam_adet INT DEFAULT 1,
    mevcut_adet INT DEFAULT 1,
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kategori_id) REFERENCES kategori(kategori_id)
);

-- ============================================
-- 5. ODUNC TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS odunc (
    OduncID INT PRIMARY KEY AUTO_INCREMENT,
    UyeID INT NOT NULL,
    KitapID INT NOT NULL,
    OduncTarihi DATETIME DEFAULT CURRENT_TIMESTAMP,
    SonTeslimTarihi DATE,
    IadeTarihi DATE,
    GorevilID INT,
    FOREIGN KEY (UyeID) REFERENCES uye(uye_id),
    FOREIGN KEY (KitapID) REFERENCES kitap(kitap_id),
    FOREIGN KEY (GorevilID) REFERENCES kullanici(kullanici_id)
);

-- ============================================
-- 6. CEZA TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS ceza (
    ceza_id INT PRIMARY KEY AUTO_INCREMENT,
    UyeID INT NOT NULL,
    OduncID INT,
    gecikme_gun INT DEFAULT 0,
    ceza_tutari DECIMAL(10, 2) DEFAULT 0.00,
    ceza_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UyeID) REFERENCES uye(uye_id),
    FOREIGN KEY (OduncID) REFERENCES odunc(OduncID)
);

-- ============================================
-- 7. LOG_ISLEM TABLOSU
-- ============================================
CREATE TABLE IF NOT EXISTS log_islem (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    islem_tipi VARCHAR(50), -- 'Ödünç Verme', 'Kitap İade', 'Ceza Oluşturma' vb.
    uye_id INT,
    kitap_id INT,
    kullanici_id INT,
    aciklama TEXT,
    islem_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uye_id) REFERENCES uye(uye_id),
    FOREIGN KEY (kitap_id) REFERENCES kitap(kitap_id),
    FOREIGN KEY (kullanici_id) REFERENCES kullanici(kullanici_id)
);

-- ============================================
-- STORED PROCEDURES
-- ============================================

-- 1. sp_YeniOduncVer: Yeni ödünç verme işlemi
DELIMITER $$

CREATE PROCEDURE sp_YeniOduncVer(
    IN p_UyeID INT,
    IN p_KitapID INT,
    IN p_IslemYapanKullaniciID INT
)
BEGIN
    DECLARE v_AktifOdunc INT;
    DECLARE v_MevcutAdet INT;
    DECLARE v_SonTeslim DATE;
    
    START TRANSACTION;
    
    -- Üyenin aktif ödünç sayısını kontrol et
    SELECT COUNT(*) INTO v_AktifOdunc 
    FROM odunc 
    WHERE UyeID = p_UyeID AND IadeTarihi IS NULL;
    
    IF v_AktifOdunc >= 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hata: Üye 5 kitaptan fazla ödünç alamaz!';
    END IF;
    
    -- Kitap stoğunu kontrol et
    SELECT mevcut_adet INTO v_MevcutAdet 
    FROM kitap 
    WHERE kitap_id = p_KitapID;
    
    IF v_MevcutAdet <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hata: Kitap stokta yok!';
    END IF;
    
    -- Son teslim tarihini hesapla (Bugün + 15 gün)
    SET v_SonTeslim = DATE_ADD(CURDATE(), INTERVAL 15 DAY);
    
    -- Ödünç kaydı ekle
    INSERT INTO odunc (UyeID, KitapID, SonTeslimTarihi, GorevilID)
    VALUES (p_UyeID, p_KitapID, v_SonTeslim, p_IslemYapanKullaniciID);
    
    -- Stoğu azalt
    UPDATE kitap SET mevcut_adet = mevcut_adet - 1 
    WHERE kitap_id = p_KitapID;
    
    COMMIT;
END$$

DELIMITER ;

-- 2. sp_KitapTeslimAl: Kitap iade alma işlemi
DELIMITER $$

CREATE PROCEDURE sp_KitapTeslimAl(
    IN p_OduncID INT,
    IN p_TeslimTarihi DATE
)
BEGIN
    DECLARE v_UyeID INT;
    DECLARE v_KitapID INT;
    DECLARE v_SonTeslimTarihi DATE;
    DECLARE v_GecikmeGun INT;
    DECLARE v_CezaTutari DECIMAL(10, 2);
    
    START TRANSACTION;
    
    -- Ödünç bilgilerini al
    SELECT UyeID, KitapID, SonTeslimTarihi 
    INTO v_UyeID, v_KitapID, v_SonTeslimTarihi
    FROM odunc 
    WHERE OduncID = p_OduncID;
    
    -- Teslim tarihini güncelle
    UPDATE odunc 
    SET IadeTarihi = p_TeslimTarihi 
    WHERE OduncID = p_OduncID;
    
    -- Kitap stoğunu artır
    UPDATE kitap 
    SET mevcut_adet = mevcut_adet + 1 
    WHERE kitap_id = v_KitapID;
    
    -- Gecikme günü hesapla (eğer varsa)
    IF p_TeslimTarihi > v_SonTeslimTarihi THEN
        SET v_GecikmeGun = DATEDIFF(p_TeslimTarihi, v_SonTeslimTarihi);
        SET v_CezaTutari = v_GecikmeGun * 5.00; -- Her gün 5 TL
        
        -- Ceza kaydı oluştur
        INSERT INTO ceza (UyeID, OduncID, gecikme_gun, ceza_tutari)
        VALUES (v_UyeID, p_OduncID, v_GecikmeGun, v_CezaTutari);
        
        -- Üyenin borcunu artır
        UPDATE uye 
        SET toplam_borc = toplam_borc + v_CezaTutari 
        WHERE uye_id = v_UyeID;
    END IF;
    
    -- Log kaydı yaz
    INSERT INTO log_islem (islem_tipi, uye_id, kitap_id, aciklama)
    VALUES ('Kitap İade', v_UyeID, v_KitapID, 
            CONCAT('Ödünç ID: ', p_OduncID, ' | Gecikme: ', IFNULL(v_GecikmeGun, 0), ' gün'));
    
    COMMIT;
END$$

DELIMITER ;

-- 3. sp_UyeOzetRapor: Üye özet raporu
DELIMITER $$

CREATE PROCEDURE sp_UyeOzetRapor(
    IN p_UyeID INT
)
BEGIN
    SELECT 
        u.uye_id,
        CONCAT(u.ad, ' ', u.soyad) AS uyead,
        u.email,
        u.telefon,
        COUNT(DISTINCT o.OduncID) AS toplam_odunc_adet,
        SUM(CASE WHEN o.IadeTarihi IS NULL THEN 1 ELSE 0 END) AS teslim_edilmemis_adet,
        COALESCE(SUM(c.ceza_tutari), 0) AS toplam_ceza_tutari,
        u.toplam_borc AS mevcut_borc
    FROM uye u
    LEFT JOIN odunc o ON u.uye_id = o.UyeID
    LEFT JOIN ceza c ON u.uye_id = c.UyeID
    WHERE u.uye_id = p_UyeID
    GROUP BY u.uye_id, u.ad, u.soyad, u.email, u.telefon, u.toplam_borc;
END$$

DELIMITER ;

-- 4. sp_KitapEkleVeyaGuncelle: Kitap ekleme veya güncelleme
DELIMITER $$

CREATE PROCEDURE sp_KitapEkleVeyaGuncelle(
    IN p_KitapID INT,
    IN p_KitapAdi VARCHAR(150),
    IN p_Yazar VARCHAR(100),
    IN p_KategoriID INT,
    IN p_Yayinevi VARCHAR(100),
    IN p_BasimYili INT,
    IN p_ToplamAdet INT,
    IN p_MevcutAdet INT
)
BEGIN
    -- Eğer KitapID 0 veya NULL ise yeni kitap ekle
    IF p_KitapID = 0 OR p_KitapID IS NULL THEN
        INSERT INTO kitap (kitap_adi, yazar, kategori_id, yayinevi, basim_yili, toplam_adet, mevcut_adet)
        VALUES (p_KitapAdi, p_Yazar, p_KategoriID, p_Yayinevi, p_BasimYili, p_ToplamAdet, p_MevcutAdet);
        
        -- Başarı mesajı döndür
        SELECT CONCAT('Kitap başarıyla eklendi! (ID: ', LAST_INSERT_ID(), ')') AS mesaj;
    ELSE
        -- Var olan kitabı güncelle
        UPDATE kitap
        SET kitap_adi = p_KitapAdi,
            yazar = p_Yazar,
            kategori_id = p_KategoriID,
            yayinevi = p_Yayinevi,
            basim_yili = p_BasimYili,
            toplam_adet = p_ToplamAdet,
            mevcut_adet = p_MevcutAdet
        WHERE kitap_id = p_KitapID;
        
        -- Başarı mesajı döndür
        SELECT CONCAT('Kitap ID ', p_KitapID, ' başarıyla güncellendi!') AS mesaj;
    END IF;
END$$

DELIMITER ;

-- 5. sp_KitapAra: Çeşitli filtreleme parametreleri ile kitap arama
DELIMITER $$

CREATE PROCEDURE sp_KitapAra(
    IN p_KitapAdi VARCHAR(150),
    IN p_Yazar VARCHAR(100),
    IN p_KategoriID INT,
    IN p_BasimYiliMin INT,
    IN p_BasimYiliMax INT,
    IN p_SadeceMevcut BOOLEAN
)
BEGIN
    SELECT 
        k.kitap_id,
        k.kitap_adi,
        k.yazar,
        kat.kategori_adi,
        k.yayinevi,
        k.basim_yili,
        k.mevcut_adet,
        k.toplam_adet,
        CONCAT(k.mevcut_adet, ' / ', k.toplam_adet) AS stok_durumu
    FROM kitap k
    LEFT JOIN kategori kat ON k.kategori_id = kat.kategori_id
    WHERE 
        (p_KitapAdi IS NULL OR p_KitapAdi = '' OR k.kitap_adi LIKE CONCAT('%', p_KitapAdi, '%'))
        AND (p_Yazar IS NULL OR p_Yazar = '' OR k.yazar LIKE CONCAT('%', p_Yazar, '%'))
        AND (p_KategoriID IS NULL OR p_KategoriID = 0 OR k.kategori_id = p_KategoriID)
        AND (p_BasimYiliMin IS NULL OR p_BasimYiliMin = 0 OR k.basim_yili >= p_BasimYiliMin)
        AND (p_BasimYiliMax IS NULL OR p_BasimYiliMax = 0 OR k.basim_yili <= p_BasimYiliMax)
        AND (p_SadeceMevcut = FALSE OR k.mevcut_adet > 0)
    ORDER BY k.kitap_adi ASC;
END$$

DELIMITER ;

-- ============================================
-- TRIGGERS
-- ============================================

-- 1. TR_ODUNC_INSERT: Ödünç verme tetikleyicisi
DELIMITER $$

CREATE TRIGGER TR_ODUNC_INSERT
AFTER INSERT ON odunc
FOR EACH ROW
BEGIN
    INSERT INTO log_islem (islem_tipi, uye_id, kitap_id, kullanici_id, aciklama)
    VALUES ('Ödünç Verme', NEW.UyeID, NEW.KitapID, NEW.GorevilID,
            CONCAT('Ödünç ID: ', NEW.OduncID, ' | Son Tarih: ', NEW.SonTeslimTarihi));
END$$

DELIMITER ;

-- 2. TR_ODUNC_UPDATE_TESLIM: Kitap iade tetikleyicisi
DELIMITER $$

CREATE TRIGGER TR_ODUNC_UPDATE_TESLIM
AFTER UPDATE ON odunc
FOR EACH ROW
BEGIN
    IF OLD.IadeTarihi IS NULL AND NEW.IadeTarihi IS NOT NULL THEN
        INSERT INTO log_islem (islem_tipi, uye_id, kitap_id, aciklama)
        VALUES ('Kitap İade', NEW.UyeID, NEW.KitapID, 
                CONCAT('Ödünç ID: ', NEW.OduncID, ' | İade Tarihi: ', NEW.IadeTarihi));
    END IF;
END$$

DELIMITER ;

-- 3. TR_CEZA_INSERT: Ceza oluşturma tetikleyicisi
DELIMITER $$

CREATE TRIGGER TR_CEZA_INSERT
AFTER INSERT ON ceza
FOR EACH ROW
BEGIN
    INSERT INTO log_islem (islem_tipi, uye_id, aciklama)
    VALUES ('Ceza Oluşturma', NEW.UyeID, 
            CONCAT('Ceza Tutarı: ', NEW.ceza_tutari, ' TL | Gecikme: ', NEW.gecikme_gun, ' gün'));
END$$

DELIMITER ;

-- 4. TR_UYE_DELETE_BLOCK: Üye silme engelleme tetikleyicisi
DELIMITER $$

CREATE TRIGGER TR_UYE_DELETE_BLOCK
BEFORE DELETE ON uye
FOR EACH ROW
BEGIN
    DECLARE v_AktifOdunc INT;
    DECLARE v_Borc DECIMAL(10, 2);
    
    -- Aktif ödünç kontrol
    SELECT COUNT(*) INTO v_AktifOdunc 
    FROM odunc 
    WHERE UyeID = OLD.uye_id AND IadeTarihi IS NULL;
    
    -- Borç kontrol
    SELECT toplam_borc INTO v_Borc FROM uye WHERE uye_id = OLD.uye_id;
    
    IF v_AktifOdunc > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hata: Üyenin henüz iade edilmemiş kitapları var!';
    END IF;
    
    IF v_Borc > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hata: Üyenin henüz ödenmemiş borcu var!';
    END IF;
END$$

DELIMITER ;

-- ============================================
-- TEST VERİLERİ
-- ============================================

-- Kullanıcı ekleme
INSERT INTO kullanici (kullanici_adi, sifre, rol) VALUES 
('admin', 'admin123', 'Admin'),
('gorevli', 'gorevli123', 'Görevli');

-- Üye ekleme
INSERT INTO uye (ad, soyad, telefon, email, toplam_borc) VALUES 
('Ahmet', 'Yılmaz', '05551234567', 'ahmet@example.com', 0.00),
('Fatma', 'Kaya', '05559876543', 'fatma@example.com', 0.00),
('Mehmet', 'Demir', '05558765432', 'mehmet@example.com', 15.00),
('Zeynep', 'Taş', '05557654321', 'zeynep@example.com', 0.00),
('Arda', 'Alık', '05556543210', 'arda@example.com', 25.00);

-- Kitap ekleme
INSERT INTO kitap (kitap_adi, yazar, kategori_id, yayinevi, basim_yili, toplam_adet, mevcut_adet) VALUES 
('Gurur ve Önyargı', 'Jane Austen', 1, 'İletişim Yayınları', 2015, 3, 2),
('Clean Code', 'Robert C. Martin', 2, 'Prentice Hall', 2008, 2, 1),
('Evrim', 'Richard Dawkins', 3, 'Tübitak', 2010, 2, 2),
('İhtiyaç Duyduğumuz Her Şey Öğretmek', 'Neil Gaiman', 1, 'Pegasus', 2019, 1, 1),
('Yapay Zeka Temelleri', 'Stuart Russell', 2, 'Prentice Hall', 2020, 2, 2);

-- Arda Alık'ın 5 gün geç teslim ettiği kitap kaydı (Devlet Ana - kategori 1 = Roman)
INSERT INTO odunc (UyeID, KitapID, OduncTarihi, SonTeslimTarihi, IadeTarihi, GorevilID) VALUES 
(5, 1, '2026-01-08', '2026-01-03', '2026-01-08', 2);

-- Arda Alık'ın 5 günlük cezası (5 gün * 5 TL/gün = 25 TL)
INSERT INTO ceza (UyeID, OduncID, gecikme_gun, ceza_tutari) VALUES 
(5, 1, 5, 25.00);

-- ============================================
-- İNDEKSLER (Performans için)
-- ============================================
CREATE INDEX idx_odunc_uye ON odunc(UyeID);
CREATE INDEX idx_odunc_kitap ON odunc(KitapID);
CREATE INDEX idx_odunc_iadetarihi ON odunc(IadeTarihi);
CREATE INDEX idx_ceza_uye ON ceza(UyeID);
CREATE INDEX idx_log_islem_uye ON log_islem(uye_id);
CREATE INDEX idx_kitap_kategori ON kitap(kategori_id);
