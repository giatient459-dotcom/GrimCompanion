package com.grimcompanion;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.grimcompanion.checks.CheckManager;
import com.grimcompanion.commands.GrimCompanionCommand;
import com.grimcompanion.data.DataManager;
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

        // Dang ky packet listener
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketListener(this), PacketListenerPriority.NORMAL
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

        getLogger().info("GrimCompanion da duoc bat thanh cong! (" + checkManager.getChecks().size() + " check dang hoat dong)");
    }

    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
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
}
