# GrimCompanion

Plugin anti-cheat bổ trợ (companion) cho Paper 1.21, dùng PacketEvents để phát hiện
các dạng gian lận phổ biến mà các anti-cheat khác (như GrimAC) có thể chưa bao phủ
đầy đủ hoặc cần tuỳ chỉnh riêng theo server. Chạy độc lập hoặc song song với GrimAC.

## Yêu cầu

- Paper/Purpur **1.21.11** (hoặc bản 1.21.x server bạn đang chạy - xem lưu ý bên dưới)
- Java **21** (Paper từ 1.20.5 trở đi và PacketEvents 2.10+ đều cần Java 21, KHÔNG còn dùng Java 17)
- [PacketEvents](https://modrinth.com/plugin/packetevents) **2.13.0** (bắt buộc, đặt trước trong `plugins/`)
- GrimAC (tuỳ chọn, soft depend - không bắt buộc)

> **Lưu ý quan trọng về version**: PacketEvents 2.5.0 (bản đầu của project) chỉ hỗ trợ tới
> Minecraft 1.21.1, KHÔNG chạy đúng trên 1.21.11. `build.gradle` và hướng dẫn cài đặt bên dưới
> đã được cập nhật dùng PacketEvents 2.13.0 + Paper API 1.21.11. Nếu server bạn đang/sẽ chạy
> một bản 1.21.x khác, sửa lại dòng `paper-api` trong `build.gradle` cho khớp đúng minor version,
> và kiểm tra trang https://modrinth.com/plugin/packetevents để chọn bản PacketEvents hỗ trợ
> đúng version server của bạn.

## Cài đặt

1. Tải `PacketEvents-Spigot-2.13.0.jar` (hoặc bản mới hơn hỗ trợ đúng version server bạn) và bỏ vào thư mục `plugins/`.
2. Build plugin bằng Gradle: `./gradlew build` (file jar nằm trong `build/libs/`).
3. Bỏ file `GrimCompanion-1.0.0.jar` vào `plugins/`.
4. Khởi động lại server. File `config.yml` sẽ được tạo tự động trong `plugins/GrimCompanion/`.

## Danh sách Check

### Combat
| Check | Mô tả |
|---|---|
| CrystalAura | Đặt/phá End Crystal quá nhanh (< 100ms) |
| AnchorAura | Đặt/phá Respawn Anchor quá nhanh |
| AutoClicker | CPS quá cao hoặc click quá đều (stddev thấp) |
| KillAura | Snap aim (xoay camera đột ngột) khi tấn công |
| Reach | Tấn công từ khoảng cách > 3.2 block |

### Movement
| Check | Mô tả |
|---|---|
| Flight | Bay trên không quá lâu bất thường mà không hợp lệ |
| Speed | Di chuyển nhanh hơn tốc độ cho phép |
| NoSlowdown | Không bị chậm khi block/ăn/giường nổ |

### Exploit
| Check | Mô tả |
|---|---|
| ItemMacro | Macro dùng item (ClickPearl, MiddleClickExtra) |
| AutoFirework | Dùng firework tự động khi bay elytra |
| ElytraTarget | Tự động bám mục tiêu khi bay elytra |
| AutoCart | TNT Cart tự động |
| CrystalOptimizer | Phát hiện Marlow's Crystal Optimizer |
| ClientBrand | Không gửi brand hoặc brand nằm trong danh sách cấm |
| Ping | Ping cao hoặc keep-alive bất thường |

### World
| Check | Mô tả |
|---|---|
| Scaffold | Đặt block quá nhanh (auto-bridge) |

Tất cả ngưỡng (delay, CPS, góc, số VL...) đều chỉnh được trong `config.yml`.

## Lệnh

| Lệnh | Quyền | Mô tả |
|---|---|---|
| `/gc reload` | `grimcompanion.admin` | Reload config |
| `/gc check <player>` | `grimcompanion.admin` | Xem thông tin player (ping, brand...) |
| `/gc alerts` | `grimcompanion.staff` | Bật/tắt nhận cảnh báo |
| `/gc stats <player>` | `grimcompanion.admin` | Xem thống kê VL theo từng check |
| `/gc reset <player>` | `grimcompanion.admin` | Reset toàn bộ VL của player |

## Quyền hạn

- `grimcompanion.*` - tất cả quyền
- `grimcompanion.admin` - quản trị (reload, check, stats, reset)
- `grimcompanion.staff` - nhận cảnh báo vi phạm realtime
- `grimcompanion.bypass` - bỏ qua toàn bộ check (dùng cho test/staff)

## PredictionEngine - Engine di chuyển riêng của GrimCompanion

GrimCompanion có một **engine mô phỏng vật lý di chuyển của riêng mình**
(`com.grimcompanion.engine.PredictionEngine`), không phụ thuộc GrimAC:

- Mỗi tick nhận gói tin vị trí, engine tự tính "vận tốc Y dự kiến" (theo trọng lực/nhảy)
  và "tốc độ ngang tối đa hợp lệ" (theo sprint/sneak/potion), rồi so sánh với dữ liệu
  client báo cáo thực tế.
- Dùng **trust buffer** (bộ đệm tin cậy) riêng cho từng trục: sai lệch 1-2 tick do lag/jitter
  sẽ không bị flag ngay, chỉ khi sai lệch LIÊN TỤC nhiều lần (mặc định 6 lần, chỉnh được qua
  `engine.max-trust-buffer`) mới thực sự bị coi là vi phạm - giảm hẳn false positive.
  Buffer tự hồi phục dần khi người chơi di chuyển hợp lệ trở lại.
- Tự động **bỏ qua (skip)** những trạng thái quá phức tạp để mô phỏng đơn giản (creative,
  elytra, bơi, leo, trong phương tiện, trong nước/nham...) thay vì đoán sai và báo flag oan.
- `FlightCheck` và `SpeedCheck` dùng CHUNG 1 lần mô phỏng/tick (được `CheckManager` tính và
  cache vào `PlayerData`), tránh gọi engine 2 lần làm sai lệch state.
- Các ngưỡng tolerance/buffer đều chỉnh được trong `config.yml` (mục `engine:`).

Engine này hoạt động **hoàn toàn độc lập**, không cần GrimAC để chạy đầy đủ. Nếu server có
GrimAC, GrimCompanion vẫn chạy song song bình thường mà không xung đột/nhường nhịn gì cả.

## Tích hợp GrimAC (tuỳ chọn - chỉ để đối chiếu)

Nếu server có cài GrimAC, GrimCompanion sẽ tự động thử "hook" vào GrimAC khi khởi động
(`GrimIntegration.java`) để **đối chiếu** kết quả giữa 2 hệ thống, KHÔNG còn dùng để nhường
engine như trước nữa:

- **`integration.relay-grimac-violations: true`** (mặc định) - đưa vi phạm từ GrimAC vào
  chung hệ thống cảnh báo/thống kê của GrimCompanion (`/gc stats`, `/gc check`), hiển thị
  với tiền tố `Grim:` để phân biệt với vi phạm từ PredictionEngine riêng của GrimCompanion.
- `/gc check <player>` hiện cả 2 nguồn: trust buffer của engine riêng VÀ (nếu hook được)
  VL của GrimAC cho check Speed, tiện lợi để so sánh 2 hệ thống.

**Quan trọng về tính tương thích API**: GrimAC không cam kết API ổn định 100% giữa các bản.
File `GrimIntegration.java` viết theo cấu trúc phổ biến của module `api` chính thức
(`ac.grim.grimac.api.GrimAPI`), nhưng bạn **cần đối chiếu lại** với đúng version GrimAC
đang chạy trên server (tên class/method có thể khác). Toàn bộ logic tích hợp được bọc trong
`try-catch(Throwable)` nên nếu API không khớp, GrimCompanion sẽ tự động rơi về chế độ độc lập
(không crash, chỉ mất phần đối chiếu - PredictionEngine riêng không bị ảnh hưởng chút nào).

## Lưu ý quan trọng

Đây là một **plugin companion (bổ sung)**, không phải anti-cheat hoàn chỉnh thay thế
GrimAC. Các check di chuyển (Flight, Speed, NoSlowdown) sử dụng heuristic đơn giản
hoá (không có prediction engine đầy đủ như GrimAC), nên có thể cần tinh chỉnh ngưỡng
trong `config.yml` theo đặc thù server của bạn để tránh false positive. Khuyến nghị
test kỹ trên server thử nghiệm trước khi dùng production, đặc biệt với các check
liên quan tới kick/ban tự động.

## Cấu trúc thư mục dữ liệu

```
plugins/GrimCompanion/
├── config.yml
└── logs/
    └── violations.log
```
