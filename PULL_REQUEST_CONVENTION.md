## Pull Request Convention

### Tên PR
[type] mô tả ngắn gọn

feat: thêm MQTT subscriber
fix: sửa lỗi device online status
refactor: tách DeviceService
chore: update docker-compose

### Mô tả PR (bắt buộc)
## What
- Làm gì trong PR này

## Why
- Tại sao cần thay đổi này

## How to test
- Bước 1: chạy simulator
- Bước 2: gọi API GET /api/devices
- Kết quả mong đợi: ...

### Checklist trước khi tạo PR
[ ] Code chạy được local
[ ] Không có console.log / System.out.println bị quên
[ ] Không hardcode URL, credential
[ ] Đã test tay flow liên quan
[ ] Tên branch đúng convention: feature/xxx, fix/xxx
[ ] Commit message đúng convention

### Quy tắc
- 1 PR chỉ giải quyết 1 vấn đề
- PR không quá 400 lines thay đổi
- Không tạo PR vào cuối ngày khi chưa test
- Assign cho DoThanhHY review
- Không merge khi chưa có approval