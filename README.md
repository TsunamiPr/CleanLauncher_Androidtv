# TV Launcher (bản tùy chỉnh)

Launcher tách từ dự án *cleanyourtv* (thư mục `launcher/`), giao diện giữ như mặc định, thêm:

1. **Số app trên dock chỉnh được: 3 – 8** (cùng là số app mỗi hàng của lưới bên dưới).
2. **Chọn hình nền từ máy**: Cài đặt launcher → Hình nền → *Chọn ảnh từ thiết bị…* (ảnh được thu nhỏ ≤ 1920 px và lưu trong bộ nhớ của launcher, giữ 12 ảnh gần nhất).
3. **Hình dạng và icon**: hình dạng ô (16:9 / 4:3 / vuông / tròn), độ bo góc, nội dung ô (banner hoặc chỉ icon), kích thước icon (bình thường / lớn / **lấp đầy hình dạng**).

## Mở Cài đặt launcher
Trên Home, giữ OK (hoặc bấm giữ) vào nút **Cài đặt** ở thanh trên → chọn *Cài đặt launcher*.
Mọi lựa chọn áp dụng ngay khi quay lại Home. *Theo theme* sẽ xóa các lựa chọn về mặc định.

## Build APK (không cần cài gì trên máy tính)
1. Tạo repository mới trên GitHub (private cũng được), tải **toàn bộ thư mục này** lên (giữ nguyên `.github/` và `keystore/`).
2. Vào tab **Actions → Build APK → Run workflow** (hoặc cứ push là tự chạy).
3. Khi chạy xong, mở lần chạy đó, tải **TvLauncher-apk** ở mục *Artifacts*, giải nén ra `TvLauncher.apk`.

Hoặc build trên máy: mở thư mục bằng Android Studio (JDK 17, SDK 34) rồi chạy `./gradlew assembleRelease`;
file nằm ở `app/build/outputs/apk/release/app-release.apk`.

## Cài lên TV
```
adb connect <IP-của-TV>
adb install -r TvLauncher.apk
```
Sau đó vào *Cài đặt TV → Ứng dụng → Ứng dụng mặc định → Màn hình chính* và chọn launcher này (hoặc nhấn Home và chọn *Luôn luôn*).
Tùy chọn, để hiện tên Wi-Fi ở thanh trên: `adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION`.

Mọi bản build đều ký bằng cùng một khóa (`keystore/tvhome.jks`) nên cài đè được lên bản trước. Nếu TV đang chạy bản launcher gốc ký bằng khóa khác, hãy gỡ bản đó trước (`adb uninstall com.gathin.tvhome`).

## Ghi chú
- TV không có trình chọn tệp (một số Google TV): launcher sẽ báo thư mục để chép ảnh vào, `Android/data/com.gathin.tvhome/files/wallpapers`, ảnh ở đó tự xuất hiện trong danh sách Hình nền.
- Giấy phép: MIT (xem `LICENSE`), font Outfit theo OFL.
