# Changelog

Tất cả những thay đổi nổi bật của dự án **TikTok SubExtract** sẽ được ghi lại trong tệp này.

---

## [1.0.0] - 2026-09-18

### 🚀 Tính năng mới (Added)
- **Hệ thống Nhận diện Share Sheet (`ACTION_SEND`):** Tự động bắt link TikTok từ Android Share Menu, lọc sạch văn bản và hashtag xung quanh.
- **Thông báo thao tác nhanh (Quick Notification 1-Tap):** Cho phép người dùng bấm "Sao chép liên kết" trên TikTok rồi chạm vào thanh thông báo để bóc ngay mà không cần tìm app trong menu chia sẻ.
- **Quick Settings Tile ("Bóc TikTok"):** Ô tiện ích ghim trên thanh công cụ cài đặt nhanh của Android.
- **Giao diện chuẩn iOS Glassmorphism (Kính mờ hiện đại):**
  - Mặt kính mờ bán trong suốt (Frosted Glass `#D01E1E28`) với viền mỏng `#33FFFFFF` và bo cong mềm mại 32dp/20dp.
  - Thanh chuyển đổi **iOS Segmented Control** cho 3 chế độ: Văn bản (Plain Text), Mốc giờ (Timestamp `[mm:ss]`), và SRT (SubRip Subtitle).
  - BottomSheet trượt lên dạng dialog nổi ngay trên video TikTok.
- **Pipeline hiển thị tiến trình 4 bước trực quan:**
  - `[✓] Nhận diện link TikTok`
  - `[✓] Trích xuất âm thanh (TikWM)`
  - `[●] Nhận diện giọng nói (Groq Whisper AI)`
  - `[✓] Hoàn tất`
- **Màn hình Cài đặt & Trích xuất thủ công:**
  - Lưu Groq API Key an toàn trong local `SharedPreferences`.
  - Nút kiểm tra kết nối API Key trực tiếp.
  - Lựa chọn mô hình: `whisper-large-v3` hoặc `whisper-large-v3-turbo`.
  - Lựa chọn ngôn ngữ (`Tự động`, `Tiếng Việt`, `English`).
  - Ô dán link thủ công để bóc tách trực tiếp mà không cần mở TikTok.

### 🛠️ Cải tiến & Sửa lỗi (Fixed & Optimized)
- **Sửa lỗi ảo giác Whisper ("Ghiền Mì Gõ"):**
  - Thay đổi cơ chế bóc âm thanh: Ưu tiên lấy video stream thực tế thay vì file nhạc nền từ kho nhạc của TikTok.
  - Thêm câu mồi `prompt` định hướng tiếng Việt để Whisper Large V3 không bị ảo giác sinh ra phụ đề rác.
- **Sửa lỗi quá tải dung lượng Groq API (`413 Request Entity Too Large`):**
  - Tích hợp module `AudioExtractor` sử dụng native `MediaExtractor` & `MediaMuxer`.
  - Tách luồng âm thanh M4A/AAC ra khỏi video trong 0.05 giây mà không cần re-encode.
  - Giảm dung lượng file từ 30MB xuống 1–2MB (giảm 95%), upload siêu tốc và hoàn toàn không bao giờ vượt trần 25MB.
- **Sửa lỗi không nhận diện được Clipboard trên Android 10-14 & MIUI:**
  - Thêm cơ chế Polling chờ Window Focus khi thanh trạng thái thu gọn.
  - Bổ sung nút bấm `[ 📋 Dán từ Clipboard ]` dự phòng trực tiếp trên giao diện.
