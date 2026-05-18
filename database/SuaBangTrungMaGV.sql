USE QuanLyCoiThi;

-- Dung file nay neu ban da tao bang danh_sach_gv cu voi ma_gv la PRIMARY KEY.
-- Sau khi sua, bang se chap nhan nhieu dong trung ma_gv nhung khac ho ten/don vi.

ALTER TABLE danh_sach_gv DROP PRIMARY KEY;

ALTER TABLE danh_sach_gv
ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

CREATE INDEX idx_danh_sach_gv_ma_gv ON danh_sach_gv(ma_gv);
