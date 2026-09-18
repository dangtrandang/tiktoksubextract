# TikTok SubExtract 🎙️ ➔ 📝

> **Công cụ bóc lời thoại cực nhanh từ video TikTok để đưa vào ChatGPT / Gemini chỉ trong vài giây.**  
> Thiết kế giao diện phong cách **iOS Glassmorphism (Kính mờ hiện đại)**, chạy hoàn toàn **Local/Serverless** không cần máy chủ trung gian.

---

## ✨ Điểm nổi bật (Key Features)

- ⚡ **Workflow 1-Tap Siêu Tốc:**
  - **Cách 1 (Share Sheet):** Bấm *Chia sẻ* trên TikTok ➔ Chọn *TikTok SubExtract* ➔ Popup BottomSheet kính mờ trượt lên bóc text ngay lập tức.
  - **Cách 2 (Thông báo thao tác nhanh):** Bấm *Sao chép liên kết* trên TikTok ➔ Vuốt thanh trạng thái chạm vào *⚡ Bóc ngay* ➔ Tự động lấy link từ Clipboard và bóc lời thoại (không cần bấm `...` tìm app).
- 💎 **Thiết kế iOS Glassmorphism:**
  - Mặt kính mờ bán trong suốt (Frosted Glass), viền ánh kim siêu mỏng, góc bo cong mềm mại 32dp/20dp chuẩn Apple Cupertino.
  - Bộ chuyển đổi **iOS Segmented Control** mượt mà giữa các định dạng đầu ra.
  - Mở dạng **BottomSheet Dialog nổi** trên nền video TikTok, không gián đoạn trải nghiệm xem.
- 🔄 **Tự động Đồng bộ & Tùy chọn Model Linh hoạt:**
  - Nút **`[ 🔄 Tải model mới ]`** kết nối thẳng Groq API để cập nhật tức thì danh sách model đang hoạt động.
  - Phân loại rõ ràng và cho phép người dùng tự do lựa chọn:
    - **Mô hình Bóc băng (Whisper):** `whisper-large-v3`, `whisper-large-v3-turbo`...
    - **Mô hình AI Sửa lỗi (LLM):** `groq/compound-mini`, `groq/compound`, `openai/gpt-oss-120b`, `qwen/qwen3.8-27b`...
  - Không lo lỗi cứng model khi Groq ra mắt phiên bản mới hoặc deprecate model cũ.
- 🧠 **Tùy chọn AI Sửa Lỗi Chính Tả & Trau Chuốt (Groq LLM Engine):**
  - Nút bấm **`[ ✨ AI Sửa Lỗi ]`** chủ động: Người dùng bấm khi cần kiểm tra ngữ cảnh.
  - Tự động phát hiện và sửa các từ đồng âm/gần âm phát âm sai, chuẩn hóa từ mượn tiếng Anh/công nghệ/tên riêng (ChatGPT, AI, TikTok, Marketing, Affiliate...).
  - Thêm dấu chấm phẩy ngắt đoạn tự nhiên, giữ nguyên 100% nội dung gốc của người nói (không tóm tắt, không bịa thêm).
  - Hỗ trợ chuyển đổi qua lại tức thì **`[ ↩️ Xem bản gốc ]`** ➔ **`[ ✨ Xem bản AI sửa ]`**.
  - Tốc độ siêu tốc (0.3s) và **100% Miễn phí** qua Groq Cloud.
- 🎯 **3 Chế độ Định dạng Transcript:**
  - **Văn bản thuần (Plain Text):** Ghép toàn bộ lời thoại tự nhiên để copy paste thẳng cho ChatGPT/Gemini đọc và phân tích.
  - **Mốc thời gian (Timestamp):** Dạng `[00:04] Lời thoại...` để đối chiếu từng phân đoạn trong video.
  - **Phụ đề (SRT):** Chuẩn SubRip Subtitle kèm timecode chi tiết dùng cho dựng video/CapCut/Premiere.
- 🚀 **Bóc tách Audio Native siêu nhẹ (`AudioExtractor`):**
  - Tích hợp module bóc tách âm thanh bằng `MediaExtractor` & `MediaMuxer` tích hợp sẵn của Android.
  - Trích xuất luồng âm thanh gốc (AAC/M4A) từ video trong **0.05 giây** mà không cần re-encode.
  - Giảm **90% – 95% dung lượng** (video 30MB ➔ file audio chỉ 1MB – 2MB), upload siêu tốc và **vĩnh viễn không lo lỗi quá 25MB (HTTP 413 Payload Too Large)** của Groq.
- 🛡️ **Khắc phục triệt để ảo giác Whisper ("Ghiền Mì Gõ"):**
  - Ưu tiên bóc tách từ video stream thực tế thay vì lấy nhầm nhạc nền thư viện TikTok.
  - Tự động gài `prompt` định hướng tiếng Việt giúp mô hình Whisper Large V3 nhận diện chính xác 100% giọng nói của người quay.
- 🔒 **Serverless & An toàn Bảo mật:**
  - Không cần máy chủ riêng, không database, không tài khoản đăng nhập.
  - Groq API Key được lưu hoàn toàn cục bộ trên thiết bị (`SharedPreferences`).

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

- **Ngôn ngữ:** Kotlin 1.9.24
- **Nền tảng:** Android SDK 34 (Hỗ trợ từ Android 8.0 trở lên, tối ưu cho Android 13/14+)
- **Build System:** Android Gradle Plugin 8.4.2, Gradle 8.8
- **UI & Architecture:** ViewBinding, Material Design 3, iOS Translucent Dialog Theme
- **Mạng & Xử lý:**
  - `OkHttp 4.12.0`: Xử lý HTTP request và Multipart upload
  - `Gson 2.10.1`: Parse JSON cấu trúc TikWM & Groq verbose_json
  - `Kotlin Coroutines`: Xử lý bất đồng bộ đa luồng
  - `Android Media APIs`: `MediaExtractor`, `MediaMuxer`, `MediaCodec`
- **Dịch vụ AI:** [Groq Cloud](https://console.groq.com) (`whisper-large-v3`, `whisper-large-v3-turbo`)

---

## 🚀 Hướng dẫn Cài đặt & Sử dụng

### 1. Chuẩn bị Groq API Key
1. Truy cập [console.groq.com/keys](https://console.groq.com/keys) (Miễn phí).
2. Tạo một API Key mới (dạng `gsk_...`).

### 2. Cài đặt App & Cấu hình lần đầu
1. Cài đặt file `app-debug.apk` vào điện thoại Android.
2. Mở app **TikTok SubExtract**:
   - Dán **Groq API Key** vào ô cấu hình.
   - Bấm **Kiểm tra kết nối Groq** để đảm bảo key hoạt động tốt.
   - Bật công tắc **"Thông báo thao tác nhanh (1-Tap)"** (Cấp quyền thông báo nếu được hỏi).
   - Bấm **Lưu cấu hình**.

### 3. Quy trình sử dụng hằng ngày
- **Cách dùng siêu tốc:**
  1. Mở video TikTok bất kỳ.
  2. Bấm **Chia sẻ ➔ Sao chép liên kết**.
  3. Vuốt nhẹ thanh thông báo xuống ➔ Chạm vào **⚡ Bóc ngay**.
  4. Xem tiến trình bóc băng (2–4 giây) ➔ Bấm **[ Sao chép ]** và paste vào ChatGPT!

---

## 📂 Cấu trúc Thư mục

```
tiktoksubextract/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml           # Khai báo permissions, Share Receiver, Foreground Service
│   │   ├── java/com/antigravity/tiktoksubextract/
│   │   │   ├── data/
│   │   │   │   ├── api/                  # TikWmService, GroqWhisperService
│   │   │   │   ├── model/                # TikWmResponse, WhisperResponse
│   │   │   │   └── pref/                 # AppPreferences
│   │   │   ├── service/                  # QuickNotificationService, QuickExtractTileService
│   │   │   ├── ui/
│   │   │   │   ├── main/                 # MainActivity (Settings & Manual Tester)
│   │   │   │   └── transcribe/           # TranscribeDialogActivity (iOS Glass Dialog)
│   │   │   └── util/                     # AudioExtractor, TranscriptFormatter, UrlExtractor
│   │   └── res/                          # iOS Glass Drawables, Layouts, Colors, Themes
│   └── src/test/                         # Unit Tests
└── README.md, CHANGELOG.md, build.gradle.kts
```

---

## 📄 Giấy phép (License)
Phát hành theo giấy phép [MIT License](LICENSE).
