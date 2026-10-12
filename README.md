# OnerSU

**Một APK chứa manager ReSukiSU/BakaSU và mã nguồn DFRoot**, với nút **Root bằng DFRoot** ngay trên trang chính. Tên hiển thị là **OnerSU**.

## Phạm vi bản 0.1.1

- Hồ sơ root: **OPPO PME110**, Android 16, ROM **16.0.9.402(CN01)**, kernel `android16-6.12`.
- Kiểm tra model, API, ABI và fingerprint trước khi cho chạy. Máy/ROM khác chỉ dùng được các chức năng manager tương thích; nút root bị khóa.
- Root **tạm thời**: khởi động lại hoàn toàn sẽ mất root; mở OnerSU và chạy DFRoot lại.
- Không tự chạy exploit khi khởi động máy. Không tự soft reboot. Không unlock bootloader.
- DFRoot đã hoạt động trên cấu hình này trong APK riêng. **Luồng gộp OnerSU mới chưa được chạy thử trên điện thoại**; đã build, lint và kiểm tra luồng bàn giao bằng chương trình mô phỏng trên PC.

## Dùng

Mở OnerSU → **Root bằng DFRoot** → **Chạy DFRoot**. Theo dõi nhật ký. Manager sẽ mở lại sau khi DFRoot kết thúc bước khôi phục và bộ root đăng ký APK xong. Root dùng khai thác kernel, có thể làm máy treo hoặc khởi động lại; không mở lại/chạy lại trong khi thao tác còn đang diễn ra.

Ứng dụng dùng package riêng **`com.onersu.onersu`** và khóa ký riêng của OnerSU. Module bootstrap đã được build lại từ nguồn với đường dẫn mới, nên không cần cài DFRoot riêng. Namespace Java/JNI kế thừa được giữ để tương thích nội bộ; chúng không phải package ứng dụng đã cài. OnerSU không thay thế package AgnesSU; việc chuyển sang backend BakaSU cần thực hiện trong một phiên khởi động sạch.

Icon dùng ảnh logo do người dùng cung cấp, đóng gói thành adaptive icon. Launcher quyết định dạng bo góc hoặc tròn theo cài đặt của máy.

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

Workflow GitHub chỉ chạy thủ công và xuất APK **chưa ký**. Ký bản cài đặt bằng khóa riêng giữ ngoài repo; khóa ký và mật khẩu không được commit. Package bootstrap là `com.onersu.onersu`; không đổi applicationId khi sử dụng module hiện tại. Workflow `build-bootstrap.yml` build module `android16-6.12` từ nguồn với DDK image được ghim theo digest. Các module KMI cũ được giữ làm nguồn tham khảo, không nhúng trong APK 0.1.1.

## Nguồn và giấy phép

Manager/backend dựa trên [Baka-SU/BakaSU](https://github.com/Baka-SU/BakaSU), dòng kế thừa ReSukiSU. DFRoot dựa trên [Applying1/DFRootOPLUS](https://github.com/Applying1/DFRootOPLUS) và bản sửa PME110 cục bộ. Các tác giả gốc gồm KernelSU/ReSukiSU/BakaSU, lsposed/lspromise, polygraphene/DFReroot, combeng6th/DirtyInit và các tác giả được ghi trong nguồn.

Giữ nguyên thông báo bản quyền và [LICENSE](LICENSE) của upstream (GPLv3); các tệp kế thừa có thông báo riêng tiếp tục giữ thông báo đó. [dfroot/UPSTREAM.md](dfroot/UPSTREAM.md) là README lịch sử của DFRoot, không phải hướng dẫn sử dụng OnerSU.
