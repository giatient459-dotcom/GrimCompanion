package com.grimcompanion;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.grimcompanion.checks.CheckManager;
import com.grimcompanion.commands.GrimCompanionCommand;
import com.grimcompanion.compat.AntiCheatDetector;
import com.grimcompanion.compat.BypassManager;
import com.grimcompanion.data.DataManager;
import com.grimcompanion.data.SqliteStorage;
import com.grimcompanion.engine.PredictionEngine;
import com.grimcompanion.integration.GrimIntegration;
import com.grimcompanion.listeners.PacketListener;
import com.grimcompanion.utils.ConfigUtil;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Class chinh cua plugin GrimCompanion.
 * Plugin bo sung anti-cheat, hoat dong doc lap hoac song song voi GrimAC.
 */
public final class GrimCompanion extends JavaPlugin {

    private static GrimCompanion instance;

    private CheckManager checkManager;
    private DataManager dataManager;
    private ConfigUtil configUtil;
    private GrimIntegration grimIntegration;
    private PredictionEngine predictionEngine;
    private BypassManager bypassManager;
    private AntiCheatDetector antiCheatDetector;
    private SqliteStorage sqliteStorage;

    @Override
    public void onLoad() {
        instance = this;
        // Khoi tao PacketEvents o giai doan onLoad (bat buoc theo tai lieu PacketEvents)
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // Tao config.yml mac dinh neu chua ton tai
        saveDefaultConfig();

        // Khoi tao PacketEvents
        PacketEvents.getAPI().init();

        // Khoi tao cac manager
        this.configUtil = new ConfigUtil(this);
        this.dataManager = new DataManager(this);
        this.predictionEngine = new PredictionEngine(this);
        this.checkManager = new CheckManager(this);
        this.bypassManager = new BypassManager();

        // Ket noi SQLite storage (luu vi pham qua cac lan restart). Neu that bai, plugin
        // van chay binh thuong o che do chi luu RAM nhu truoc day - xem SqliteStorage.java.
        this.sqliteStorage = new SqliteStorage(this);
        sqliteStorage.connect();

        // Don dep lich su vi pham cu (giu lai 30 ngay gan nhat) - chay 1 lan/ngay (72000 tick),
        // delay lan dau 5 phut sau khi server khoi dong de khong lam nang tai luc startup.
        int keepDays = getConfig().getInt("storage.keep-history-days", 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> sqliteStorage.pruneOldHistory(keepDays), 6000L, 1728000L);

        // Don dep bypass het han moi 20 giay (400 tick), tranh memory leak neu server chay lau
        getServer().getScheduler().runTaskTimer(this, () -> bypassManager.cleanupExpired(), 400L, 400L);

        // Dang ky packet listener (PacketEvents >=2.9: dung asAbstract() de gan priority,
        // vi "PacketListener" gio la interface chu khong con extends duoc PacketListenerAbstract)
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketListener(this).asAbstract(PacketListenerPriority.NORMAL)
        );

        // Dang ky command
        GrimCompanionCommand commandExecutor = new GrimCompanionCommand(this);
        getCommand("grimcompanion").setExecutor(commandExecutor);
        getCommand("grimcompanion").setTabCompleter(commandExecutor);

        // Tich hop GrimAC (soft depend, TUY CHON): GrimCompanion gio da co PredictionEngine
        // rieng nen KHONG con nhuong viec kiem tra di chuyen cho GrimAC nua. Phan tich hop nay
        // chi con dung de relay vi pham cua GrimAC vao chung thong ke (/gc stats) neu ban muon
        // doi chieu 2 nguon du lieu voi nhau.
        this.grimIntegration = new GrimIntegration(this);
        getServer().getScheduler().runTask(this, () -> grimIntegration.tryHook());

        // Tu dong do cac anti-cheat KHAC (khong phai GrimAC) de canh bao/tranh xung dot
        // kiem tra di chuyen trung lap. Xem AntiCheatDetector.java.
        this.antiCheatDetector = new AntiCheatDetector(this);
        getServer().getScheduler().runTask(this, () -> antiCheatDetector.detectAndWarn());

        getLogger().info("GrimCompanion da duoc bat thanh cong! (" + checkManager.getChecks().size() + " check dang hoat dong)");
    }

    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        if (sqliteStorage != null) {
            sqliteStorage.close();
        }
        if (dataManager != null) {
            dataManager.clearAll();
        }
        getLogger().info("GrimCompanion da tat.");
    }

    public static GrimCompanion getInstance() {
        return instance;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public GrimIntegration getGrimIntegration() {
        return grimIntegration;
    }

    public PredictionEngine getPredictionEngine() {
        return predictionEngine;
    }

    public BypassManager getBypassManager() {
        return bypassManager;
    }

    public SqliteStorage getSqliteStorage() {
        return sqliteStorage;
    }
}
