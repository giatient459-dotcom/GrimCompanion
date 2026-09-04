# GrimCompanion

Plugin anti-cheat bo sung (companion) cho Paper 1.21, dung PacketEvents de phat hien
cac dang gian lan pho bien ma cac anti-cheat khac (nhu GrimAC) co the chua bao phu
day du hoac can tuy chinh rieng theo server. Chay doc lap hoac song song voi GrimAC.

## Yeu cau

- Paper/Purpur **1.21.11** (hoac ban 1.21.x server ban dang chay - xem luu y ben duoi)
- Java **21** (Paper tu 1.20.5 tro di va PacketEvents 2.10+ deu can Java 21, KHONG con dung Java 17)
- [PacketEvents](https://modrinth.com/plugin/packetevents) **2.13.0** (bat buoc, dat truoc trong `plugins/`)
- GrimAC (tuy chon, soft depend - khong bat buoc)

> **Luu y quan trong ve version**: PacketEvents 2.5.0 (ban dau cua project) chi ho tro toi
> Minecraft 1.21.1, KHONG chay dung tren 1.21.11. `build.gradle` va huong dan cai dat ben duoi
> da duoc cap nhat dung PacketEvents 2.13.0 + Paper API 1.21.11. Neu server ban dang/se chay
> mot ban 1.21.x khac, sua lai dong `paper-api` trong `build.gradle` cho khop dung minor version,
> va kiem tra trang https://modrinth.com/plugin/packetevents de chon ban PacketEvents ho tro
> dung version server cua ban.

## Cai dat

1. Tai `PacketEvents-Spigot-2.13.0.jar` (hoac ban moi hon ho tro dung version server ban) va bo vao thu muc `plugins/`.
2. Build plugin bang Gradle: `./gradlew build` (file jar nam trong `build/libs/`).
3. Bo file `GrimCompanion-1.0.0.jar` vao `plugins/`.
4. Khoi dong lai server. File `config.yml` se duoc tao tu dong trong `plugins/GrimCompanion/`.

## Danh sach Check

### Combat
| Check | Mo ta |
|---|---|
| CrystalAura | Dat/pha End Crystal qua nhanh (< 100ms) |
| AnchorAura | Dat/pha Respawn Anchor qua nhanh |
| AutoClicker | CPS qua cao hoac click qua deu (stddev thap) |
| KillAura | Snap aim (xoay camera dot ngot) khi tan cong |
| Reach | Tan cong tu khoang cach > 3.2 block |

### Movement
| Check | Mo ta |
|---|---|
| Flight | Bay tren khong lau bat thuong ma khong hop le |
| Speed | Di chuyen nhanh hon toc do cho phep |
| NoSlowdown | Khong bi cham khi block/an/giuong no |

### Exploit
| Check | Mo ta |
|---|---|
| ItemMacro | Macro dung item (ClickPearl, MiddleClickExtra) |
| AutoFirework | Dung firework tu dong khi bay elytra |
| ElytraTarget | Tu dong bam muc tieu khi bay elytra |
| AutoCart | TNT Cart tu dong |
| CrystalOptimizer | Phat hien Marlow's Crystal Optimizer |
| ClientBrand | Khong gui brand hoac brand nam trong danh sach cam |
| Ping | Ping cao hoac keep-alive bat thuong |

### World
| Check | Mo ta |
|---|---|
| Scaffold | Dat block qua nhanh (auto-bridge) |

Tat ca nguong (delay, CPS, goc, so VL...) deu chinh duoc trong `config.yml`.

## Lenh

| Lenh | Quyen | Mo ta |
|---|---|---|
| `/gc reload` | `grimcompanion.admin` | Reload config |
| `/gc check <player>` | `grimcompanion.admin` | Xem thong tin player (ping, brand...) |
| `/gc alerts` | `grimcompanion.staff` | Bat/tat nhan canh bao |
| `/gc stats <player>` | `grimcompanion.admin` | Xem thong ke VL theo tung check |
| `/gc reset <player>` | `grimcompanion.admin` | Reset toan bo VL cua player |

## Quyen han

- `grimcompanion.*` - tat ca quyen
- `grimcompanion.admin` - quan tri (reload, check, stats, reset)
- `grimcompanion.staff` - nhan canh bao vi pham realtime
- `grimcompanion.bypass` - bo qua toan bo check (dung cho test/staff)

## PredictionEngine - Engine di chuyen RIENG cua GrimCompanion

GrimCompanion co mot **engine mo phong vat ly di chuyen cua rieng minh**
(`com.grimcompanion.engine.PredictionEngine`), khong phu thuoc GrimAC:

- Moi tick nhan goi tin vi tri, engine tu tinh "van toc Y du kien" (theo trong luc/nhay)
  va "toc do ngang toi da hop le" (theo sprint/sneak/potion), roi so sanh voi du lieu
  client bao cao thuc te.
- Dung **trust buffer** (bo dem tin cay) rieng cho tung truc: sai lech 1-2 tick do lag/jitter
  se khong bi flag ngay, chi khi sai lech LIEN TUC nhieu lan (mac dinh 6 lan, chinh duoc qua
  `engine.max-trust-buffer`) moi thuc su bi coi la vi pham - giam han false positive.
  Buffer tu hoi phuc dan khi nguoi choi di chuyen hop le tro lai.
- Tu dong **bo qua (skip)** nhung trang thai qua phuc tap de mo phong don gian (creative,
  elytra, boi, leo, trong phuong tien, trong nuoc/nham...) thay vi doan sai va bao flag oan.
- `FlightCheck` va `SpeedCheck` dung CHUNG 1 lan mo phong/tick (duoc `CheckManager` tinh va
  cache vao `PlayerData`), tranh goi engine 2 lan lam sai lech state.
- Cac nguong tolerance/buffer deu chinh duoc trong `config.yml` (muc `engine:`).

Engine nay hoat dong **hoan toan doc lap**, khong can GrimAC de chay day du. Neu server co
GrimAC, GrimCompanion van chay song song binh thuong ma khong xung dot/nhuong nhin gi ca.

## Tich hop GrimAC (tuy chon - chi de doi chieu)

Neu server co cai GrimAC, GrimCompanion se tu dong thu "hook" vao GrimAC khi khoi dong
(`GrimIntegration.java`) de **doi chieu** ket qua giua 2 he thong, KHONG con dung de nhuong
engine nhu truoc nua:

- **`integration.relay-grimac-violations: true`** (mac dinh) - dua vi pham tu GrimAC vao
  chung he thong canh bao/thong ke cua GrimCompanion (`/gc stats`, `/gc check`), hien thi
  voi tien to `Grim:` de phan biet voi vi pham tu PredictionEngine rieng cua GrimCompanion.
- `/gc check <player>` hien ca 2 nguon: trust buffer cua engine rieng VA (neu hook duoc)
  VL cua GrimAC cho check Speed, tien loi de so sanh 2 he thong.

**Quan trong ve tinh tuong thich API**: GrimAC khong cam ket API on dinh 100% giua cac ban.
File `GrimIntegration.java` viet theo cau truc pho bien cua module `api` chinh thuc
(`ac.grim.grimac.api.GrimAPI`), nhung ban **can doi chieu lai** voi dung version GrimAC
dang chay tren server (ten class/method co the khac). Toan bo logic tich hop duoc boc trong
`try-catch(Throwable)` nen neu API khong khop, GrimCompanion se tu dong roi ve che do doc lap
(khong crash, chi mat phan doi chieu - PredictionEngine rieng khong bi anh huong chut nao).

## Luu y quan trong

Day la mot **plugin companion (bo sung)**, khong phai anti-cheat hoan chinh thay the
GrimAC. Cac check di chuyen (Flight, Speed, NoSlowdown) su dung heuristic don gian
hoa (khong co prediction engine day du nhu GrimAC), nen co the can tinh chinh nguong
trong `config.yml` theo dac thu server cua ban de tranh false positive. Khuyen nghi
test ky tren server thu nghiem truoc khi dung production, dac biet voi cac check
lien quan toi kick/ban tu dong.

## Cau truc thu muc du lieu

```
plugins/GrimCompanion/
├── config.yml
└── logs/
    └── violations.log
```
