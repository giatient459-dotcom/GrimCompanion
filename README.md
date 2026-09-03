# GrimCompaion
Đây là plugin anti-cheat bổ sung dành cho Paper, sử dụng PacketEvents để phát hiện các hình thức gian lận mà plugin anti-cheat (GrimAC) chưa có. Plugin chạy song song với GrimAC, còn độc lập (chưa phát triển đến mức vậy)


## Yêu cầu
· Paper/Purpur **1.21.11** (hoặc phiên bản 1.21.x
mà máy chủ bạn đang chạy)
· Cài **PacketEvents** mới nhất (bắt buộc) và cài thêm GrimAC nữa
· [PacketEvents(https://modrinth.com/plugin/packetevents)
· [GrimAC](https://modrinth.com/plugin/grimac)


## Danh sách Plugin Mà Check

## Combo (Chiến đấu)

Check Và Mô Tả
· CrystalAura: Đặt/phá End Crystal quá nhanh (< 100ms)
· AnchorAura: Đặt/phá Respawn Anchor quá nhanh
· AutoClicker: CPS quá cao hoặc click quá đều (độ lệch chuẩn thấp)
· KillAura: Xoay góc nhìn đột ngột (snap aim) khi tấn công
· Reach: Tấn công từ khoảng cách > 3.2 block


## Movement (Di chuyển)

| Check | Mô tả |
|-------|-------|
| **Flight** | Ở trên không quá lâu bất thường mà không hợp lệ |
| **Speed** | Di chuyển nhanh hơn tốc độ cho phép |
| **NoSlowdown** | Không bị chậm lại khi block/ăn/nổ giường |

## Exploit (Khai thác lỗi)

| Check | Mô tả |
|-------|-------|
| **ItemMacro** | Sử dụng macro item (ClickPearl, MiddleClickExtra) |
| **AutoFirework** | Tự động dùng pháo hoa khi bay elytra |
| **ElytraTarget** | Tự động nhắm mục tiêu khi bay elytra |
| **AutoCart** | Tự động dùng TNT Cart |
| **CrystalOptimizer** | Phát hiện Marlow's Crystal Optimizer |
| **ClientBrand** | Không gửi brand hoặc brand nằm trong danh sách cấm |
| **Ping** | Ping cao hoặc keep-alive bất thường |
