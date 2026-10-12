# OnerSU

## 0.1.4

- The root log's Soft restart button is available after current-run manager registration and native cleanup succeed, including before the old app process receives its driver FD.
- Returning to the root screen refreshes live manager readiness.
- A rejected soft restart now reports failure instead of leaving the button stuck on "Restarting".
- Signed APK releases are published at https://github.com/oneone404/OnerSU/releases. The manager checks this repository for stable updates; use the same release signing certificate for in-place updates.

**Một APK chứa manager ReSukiSU/BakaSU và mã nguồn DFRoot**, với nút **Root bằng DFRoot** ngay trên trang chính. Tên hiển thị là **OnerSU**.

## Phạm vi bản 0.1.3

- Hồ sơ root: **OPPO PME110**, Android 16, ROM **16.0.9.402(CN01)**, kernel `android16-6.12`.
- Kiểm tra model, API, ABI và fingerprint trước khi cho chạy. Máy/ROM khác chỉ dùng được các chức năng manager tương thích; nút root bị khóa.
- Root **tạm thời**: khởi động lại hoàn toàn sẽ mất root; mở OnerSU và chạy DFRoot lại.
- Không tự chạy exploit khi khởi động máy. Không tự soft reboot. Không unlock bootloader.
- Luồng gộp đã nạp BakaSU trên PME110. Bản 0.1.1 gặp lỗi đăng ký manager do định dạng/chứng chỉ ký APK; bản 0.1.2 sửa các lỗi này và đã được kiểm tra nhận driver trong phiên root hiện có. Chưa chạy lại exploit từ một phiên khởi động sạch với bản 0.1.2.

## Dùng

Mở OnerSU → **Root bằng DFRoot** → **Chạy DFRoot**. Theo dõi nhật ký. Sau bước khôi phục và đăng ký APK, OnerSU mở lại màn hình nhật ký với nút **Khởi động lại phần mềm** ở góc dưới. Bấm nút này để nạp lại Android và script module như ReZygisk/HMA, giữ root tạm; các app đang mở sẽ đóng. Root dùng khai thác kernel, có thể làm máy treo hoặc khởi động lại; không mở lại/chạy lại trong khi thao tác còn đang diễn ra.

Bản 0.1.3 xác nhận kết quả đăng ký manager bằng token của đúng lần chạy sau khi cleanup hoàn tất. Hết thời gian chờ native 5 giây chỉ báo đang chờ xác nhận, không tự kết luận root thất bại. Một hook đã kích hoạt hoặc driver chưa cấp quyền manager cũng không được báo là root sẵn sàng. Nút soft restart chỉ hiện khi manager có quyền với driver và UAPI khớp; không tự reboot.

Ứng dụng dùng package riêng **`com.onersu.onersu`** và khóa ký riêng của OnerSU. Module bootstrap đã được build lại từ nguồn với đường dẫn mới, nên không cần cài DFRoot riêng. Namespace Java/JNI kế thừa được giữ để tương thích nội bộ; chúng không phải package ứng dụng đã cài. OnerSU không thay thế package AgnesSU; việc chuyển sang backend BakaSU cần thực hiện trong một phiên khởi động sạch.

Icon dùng ảnh logo do người dùng cung cấp, đóng gói thành adaptive icon. Launcher quyết định dạng bo góc hoặc tròn theo cài đặt của máy.
Logo có khoảng đệm theo tỷ lệ để giữ kích thước ảnh bình thường khi launcher áp dụng mặt nạ adaptive icon.

## Kiến trúc

- `manager/app/src/main/java/df/root`: runner Java/JNI, nguồn DFRoot được gộp trực tiếp.
- `manager/app/src/main/cpp/dfroot`: C, ASM, helper biên dịch từ nguồn và module bootstrap.
- `dfroot/dirtyfrag-lkm`: nguồn module bootstrap và Makefile.
- Repository → use case → ViewModel → màn hình Compose, dùng container Koin của manager.
- Runner hoạt động trong application scope để rời màn hình không hủy bước cleanup.
- Shim gọi backend với một package giả không cài đặt, để upstream không force-stop OnerSU quá sớm. Đăng ký chữ ký APK thật qua dynamic-manager; chỉ restart manager sau token cleanup của đúng lần chạy.
- Chức năng manager được giữ từ upstream. Màn hình DFRoot có tiếng Việt, Anh, Trung; các ngôn ngữ còn lại dùng tiếng Anh cho phần này.

## Build

Yêu cầu Java 21, Python 3, Android SDK `android-37.0`, build tools `36.1.0`, CMake `3.22.1`, NDK `30.0.16248370`.

```sh
python3 scripts/fetch-onersu-backend.py
python3 scripts/test-onersu-handoff.py
cd manager
sh gradlew spotlessCheck assembleRelease lintRelease
```

Windows: dùng `gradlew.bat`; khai báo SDK/NDK trong `manager/local.properties` hoặc cài NDK đúng phiên bản dưới SDK. Script kiểm tra handoff cần POSIX `sh`; có thể đặt `ONERSU_TEST_SH` trỏ tới `sh.exe` của Git. APK nằm trong `manager/app/build/outputs/apk/release`.

Repo giữ bản backend đã xác minh. Script fetch kiểm tra SHA-256 của binary có sẵn; nếu thiếu thì tải và kiểm tra cả ZIP lẫn `ksud`, không lấy backend mới tùy ý. Artifact upstream có thể hết hạn; nếu vậy cần khôi phục **đúng** artifact từ bản lưu đã xác minh hoặc rebuild backend từ commit được ghi trong [PROVENANCE.md](PROVENANCE.md). Không bỏ kiểm tra hash để lấy một binary khác.

Workflow GitHub chỉ chạy thủ công và xuất APK **chưa ký**. Ký bản cài đặt bằng khóa riêng giữ ngoài repo; khóa ký và mật khẩu không được commit. **Chỉ ký V2**, với chứng chỉ DER dài **256–1024 byte**: cả `ksud set-apk` và driver đều từ chối V1/V3/V3.1; driver còn giới hạn chứng chỉ ở 1024 byte. Dùng DN ngắn như `CN=OnerSU`. Dùng `scripts/sign-onersu-release.py` với đường dẫn build tools, keystore và hai tệp mật khẩu bên ngoài repo; script ký V2, kiểm tra chữ ký bằng apksigner và kiểm tra định dạng driver. Sau đó chạy `scripts/verify-onersu-apk.py APK AAPT2` để kiểm tra gói hoàn chỉnh. Bản 0.1.1 ký V3 và có chứng chỉ 1048 byte đã nạp root nhưng không đăng ký được manager; bản 0.1.2 dùng V2 và chứng chỉ 971 byte.

Chứng chỉ 0.1.2 được tạo từ cùng khóa riêng với DN ngắn hơn, nhưng Android coi đây là chữ ký APK mới. Khi chuyển từ 0.1.1 cần sao lưu dữ liệu ứng dụng trước khi cài lại; các bản tiếp theo giữ nguyên chứng chỉ 0.1.2 để cập nhật bình thường. Khi khôi phục dữ liệu sau khi UID thay đổi, phải sửa chủ sở hữu và dùng `restorecon -RFD` cho thư mục ứng dụng. Kiểm tra danh tính bản phát hành trong manager cũng được đổi sang đúng chứng chỉ OnerSU; trình kiểm tra APK xác nhận dữ liệu này khớp với chứng chỉ ký thực tế. Bản fork tự ký cần cập nhật danh tính bản phát hành tương ứng.

Package bootstrap là `com.onersu.onersu`; không đổi applicationId khi sử dụng module hiện tại. Workflow `build-bootstrap.yml` build module `android16-6.12` từ nguồn với DDK image được ghim theo digest. Các module KMI cũ được giữ làm nguồn tham khảo, không nhúng trong APK 0.1.2.

## Nguồn và giấy phép

Manager/backend dựa trên [Baka-SU/BakaSU](https://github.com/Baka-SU/BakaSU), dòng kế thừa ReSukiSU. DFRoot dựa trên [Applying1/DFRootOPLUS](https://github.com/Applying1/DFRootOPLUS) và bản sửa PME110 cục bộ. Các tác giả gốc gồm KernelSU/ReSukiSU/BakaSU, lsposed/lspromise, polygraphene/DFReroot, combeng6th/DirtyInit và các tác giả được ghi trong nguồn.

Giữ nguyên thông báo bản quyền và [LICENSE](LICENSE) của upstream (GPLv3); các tệp kế thừa có thông báo riêng tiếp tục giữ thông báo đó. [dfroot/UPSTREAM.md](dfroot/UPSTREAM.md) là README lịch sử của DFRoot, không phải hướng dẫn sử dụng OnerSU.
