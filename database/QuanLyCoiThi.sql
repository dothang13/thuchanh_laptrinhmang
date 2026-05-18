CREATE DATABASE IF NOT EXISTS QuanLyCoiThi
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE QuanLyCoiThi;

CREATE TABLE IF NOT EXISTS danh_sach_gv (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ma_gv VARCHAR(100) NOT NULL,
    ho_ten VARCHAR(255) NOT NULL,
    ngay_sinh DATE,
    don_vi_cong_tac VARCHAR(255),
    INDEX idx_danh_sach_gv_ma_gv (ma_gv)
);

CREATE TABLE IF NOT EXISTS danh_sach_phong (
    phong_thi INT PRIMARY KEY,
    ghi_chu VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS lich_coi_thi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    so_phong INT NOT NULL,
    so_giam_thi INT NOT NULL,
    ghi_chu VARCHAR(255),
    ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS phan_cong_phong (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lich_id BIGINT NOT NULL,
    phong INT NOT NULL,
    ma_gt1 VARCHAR(100),
    ten_gt1 VARCHAR(255),
    ngay_sinh_gt1 VARCHAR(20),
    don_vi_gt1 VARCHAR(255),
    ma_gt2 VARCHAR(100),
    ten_gt2 VARCHAR(255),
    ngay_sinh_gt2 VARCHAR(20),
    don_vi_gt2 VARCHAR(255),
    FOREIGN KEY (lich_id) REFERENCES lich_coi_thi(id)
);

CREATE TABLE IF NOT EXISTS phan_cong_hanh_lang (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lich_id BIGINT NOT NULL,
    ma_gt VARCHAR(100),
    ten_gt VARCHAR(255),
    ngay_sinh VARCHAR(20),
    don_vi VARCHAR(255),
    tu_phong INT,
    den_phong INT,
    FOREIGN KEY (lich_id) REFERENCES lich_coi_thi(id)
);
